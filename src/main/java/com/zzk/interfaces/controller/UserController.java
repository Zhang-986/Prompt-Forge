package com.zzk.interfaces.controller;

import com.zzk.domain.model.aggregate.User;
import com.zzk.domain.model.entity.LoginAuditLog;
import com.zzk.domain.model.valueobject.CaptchaResult;
import com.zzk.domain.model.valueobject.LoginAttemptInfo;
import com.zzk.domain.model.valueobject.LoginResult;
import com.zzk.domain.repository.UserRepository;
import com.zzk.domain.service.auth.LoginGuardService;
import com.zzk.infrastructure.exception.BusinessException;
import com.zzk.infrastructure.util.JwtUtil;
import com.zzk.interfaces.dto.response.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.zzk.application.service.EmailService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户控制器
 * 
 * @author zzk
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "用户管理", description = "用户注册、登录、信息管理")
public class UserController {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final LoginGuardService loginGuardService;
    private final EmailService emailService;
    private final com.zzk.domain.service.StorageService storageService;

    /**
     * 上传头像
     */
    @PostMapping(value = "/avatar", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传头像")
    public Result<String> uploadAvatar(
            @RequestAttribute("userId") Long userId,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {

        // 校验文件
        if (file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new BusinessException("文件大小不能超过 5MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException("仅支持图片文件");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        // 生成文件名: avatars/userId/timestamp_filename
        String filename = "avatars/" + userId + "/" + System.currentTimeMillis() + "_" + file.getOriginalFilename();

        // 上传
        String avatarUrl = storageService.upload(filename, file);

        // 更新用户头像
        user.setAvatar(avatarUrl);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user); // Repository implementation usually handles Entity -> PO conversion and
                                   // verifies ID

        return Result.success("上传成功", avatarUrl);
    }

    /**
     * 更新个人资料 (昵称)
     */
    @PutMapping("/profile")
    @Operation(summary = "更新个人资料")
    public Result<Void> updateProfile(
            @RequestAttribute("userId") Long userId,
            @RequestBody Map<String, String> request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        if (request.containsKey("nickname")) {
            String nickname = request.get("nickname");
            if (nickname != null && nickname.length() > 50) {
                throw new BusinessException("昵称长度不能超过 50 个字符");
            }
            user.setNickname(nickname);
        }

        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        return Result.success("更新成功", null);
    }

    /**
     * 发送邮箱验证码
     */
    @PostMapping("/send-email-code")
    @Operation(summary = "发送邮箱验证码", description = "发送验证码到指定邮箱，用于注册验证")
    public Result<Map<String, Object>> sendEmailCode(@RequestBody Map<String, String> request) {
        String email = request.get("email");

        if (email == null || email.isBlank()) {
            throw new BusinessException("邮箱不能为空");
        }

        // 简单的邮箱格式验证
        if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            throw new BusinessException("邮箱格式不正确");
        }

        // 检查邮箱是否已注册
        if (userRepository.findByEmail(email).isPresent()) {
            throw new BusinessException("该邮箱已被注册");
        }

        // 发送验证码
        int cooldown = emailService.sendVerificationCode(email);

        Map<String, Object> data = new HashMap<>();
        if (cooldown > 0) {
            data.put("cooldown", cooldown);
            return Result.success("请稍后再试", data);
        }

        data.put("cooldown", 60); // 返回默认冷却时间
        return Result.success("验证码已发送", data);
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    @Operation(summary = "用户注册")
    public Result<Map<String, Object>> register(
            HttpServletRequest httpRequest,
            @Valid @RequestBody RegisterRequest request) {
        log.info("用户注册: username={}", request.getUsername());

        // 验证邮箱验证码
        if (!emailService.verifyCode(request.getEmail(), request.getEmailCode())) {
            throw new BusinessException("验证码错误或已过期");
        }

        // 检查用户名是否存在
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new BusinessException("用户名已存在");
        }

        // 检查邮箱是否存在
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new BusinessException("邮箱已被注册");
        }

        // 创建用户
        User user = User.builder()
                .username(request.getUsername())
                .password(hashPassword(request.getPassword()))
                .email(request.getEmail())
                .role("MEMBER")
                .status(1)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        userRepository.save(user);
        log.info("用户注册成功: id={}", user.getId());

        // 生成 JWT Token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());

        // 记录登录日志（注册后自动登录）
        String ip = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        loginGuardService.logAudit(LoginAuditLog.registerSuccess(
                user.getUsername(), ip, userAgent));
        log.info("注册后自动登录日志已记录: username={}, ip={}", user.getUsername(), ip);

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("user", buildUserInfo(user));

        return Result.success("注册成功", data);
    }

    /**
     * 用户登录（集成登录防护）
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public Result<Map<String, Object>> login(
            HttpServletRequest httpRequest,
            @Valid @RequestBody LoginRequest request) {

        String ip = getClientIp(httpRequest);
        String username = request.getUsername();
        String userAgent = httpRequest.getHeader("User-Agent");

        log.info("用户登录: username={}, ip={}", username, ip);

        // 1. 登录前检查（封禁/验证码）
        LoginAttemptInfo attemptInfo = loginGuardService.preLoginCheck(ip, username);

        if (attemptInfo.banned()) {
            // 记录审计日志
            loginGuardService.logAudit(LoginAuditLog.failure(
                    username, ip, userAgent, LoginResult.BANNED, "账号被封禁"));

            String bannedUntilStr = attemptInfo.bannedUntil()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            throw new BusinessException("账号已被临时封禁，解封时间：" + bannedUntilStr);
        }

        // 2. 验证码校验（需要验证码时必须校验）
        if (attemptInfo.captchaRequired()) {
            if (request.getCaptchaKey() == null || request.getCaptchaCode() == null) {
                throw new BusinessException(428, "需要输入验证码");
            }
            if (!loginGuardService.verifyCaptcha(request.getCaptchaKey(), request.getCaptchaCode())) {
                // 记录审计日志
                loginGuardService.logAudit(LoginAuditLog.failure(
                        username, ip, userAgent, LoginResult.CAPTCHA_FAILED, "验证码错误"));
                throw new BusinessException(428, "验证码错误或已过期");
            }
        }

        // 3. 查找用户
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            loginGuardService.recordFailure(ip, username, "用户不存在");
            loginGuardService.logAudit(LoginAuditLog.failure(
                    username, ip, userAgent, LoginResult.USER_NOT_FOUND, "用户不存在"));
            throw new BusinessException("用户名或密码错误");
        }

        // 4. 验证密码
        if (!verifyPassword(request.getPassword(), user.getPassword())) {
            LoginAttemptInfo newAttempt = loginGuardService.recordFailure(ip, username, "密码错误");
            loginGuardService.logAudit(LoginAuditLog.failure(
                    username, ip, userAgent, LoginResult.FAILED_PASSWORD, "密码错误"));

            // 返回友好提示
            if (newAttempt.banned()) {
                String bannedUntilStr = newAttempt.bannedUntil()
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                throw new BusinessException("登录失败次数过多，账号已被临时封禁至 " + bannedUntilStr);
            } else if (newAttempt.captchaRequired()) {
                throw new BusinessException(428, "密码错误，请输入验证码后重试");
            }
            throw new BusinessException("用户名或密码错误");
        }

        // 5. 检查账号状态
        if (user.getStatus() != 1) {
            loginGuardService.logAudit(LoginAuditLog.failure(
                    username, ip, userAgent, LoginResult.ACCOUNT_DISABLED, "账号已禁用"));
            throw new BusinessException("账号已被禁用");
        }

        // 6. 登录成功
        loginGuardService.recordSuccess(ip, username);
        loginGuardService.logAudit(LoginAuditLog.success(username, ip, userAgent));

        // 生成 JWT Token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("user", buildUserInfo(user));

        log.info("用户登录成功: id={}", user.getId());
        return Result.success("登录成功", data);
    }

    /**
     * 获取验证码
     */
    @GetMapping("/captcha")
    @Operation(summary = "获取验证码", description = "用于登录失败次数过多时需要输入验证码")
    public Result<CaptchaResult> getCaptcha() {
        CaptchaResult captcha = loginGuardService.generateCaptcha();
        return Result.success(captcha);
    }

    /**
     * 检查登录状态（是否需要验证码）
     */
    @GetMapping("/login-check")
    @Operation(summary = "检查登录状态", description = "检查指定用户名是否需要验证码")
    public Result<Map<String, Object>> checkLoginStatus(
            HttpServletRequest httpRequest,
            @RequestParam String username) {

        String ip = getClientIp(httpRequest);
        LoginAttemptInfo attemptInfo = loginGuardService.preLoginCheck(ip, username);

        Map<String, Object> data = new HashMap<>();
        data.put("captchaRequired", attemptInfo.captchaRequired());
        data.put("banned", attemptInfo.banned());
        if (attemptInfo.banned() && attemptInfo.bannedUntil() != null) {
            data.put("bannedUntil", attemptInfo.bannedUntil()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }

        return Result.success(data);
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    @Operation(summary = "获取当前用户信息")
    public Result<Map<String, Object>> getCurrentUser(@RequestAttribute("userId") Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        return Result.success(buildUserInfo(user));
    }

    /**
     * 退出登录
     */
    @PostMapping("/logout")
    @Operation(summary = "退出登录")
    public Result<Void> logout() {
        // JWT 无状态，客户端删除 Token 即可
        return Result.success("退出成功", null);
    }

    /**
     * 修改密码
     */
    @PostMapping("/change-password")
    @Operation(summary = "修改密码")
    public Result<Void> changePassword(
            @RequestAttribute("userId") Long userId,
            @Valid @RequestBody ChangePasswordRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        // 验证旧密码
        if (!verifyPassword(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException("原密码错误");
        }

        // 更新密码
        user.setPassword(hashPassword(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        return Result.success("密码修改成功", null);
    }

    /**
     * 搜索用户（用于邀请成员）
     */
    @GetMapping("/search")
    @Operation(summary = "搜索用户", description = "通过用户名搜索用户")
    public Result<Map<String, Object>> searchUser(@RequestParam String username) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return Result.success(buildUserInfo(user));
    }

    // ==================== 辅助方法 ====================

    /**
     * 获取客户端真实 IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多个代理时，取第一个 IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private String hashPassword(String password) {
        org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        return encoder.encode(password);
    }

    private boolean verifyPassword(String rawPassword, String hashedPassword) {
        if (hashedPassword != null && hashedPassword.startsWith("$2a$")) {
            org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
            return encoder.matches(rawPassword, hashedPassword);
        }
        return String.valueOf(rawPassword.hashCode()).equals(hashedPassword);
    }

    private Map<String, Object> buildUserInfo(User user) {
        Map<String, Object> info = new HashMap<>();
        info.put("id", user.getId());
        info.put("username", user.getUsername());
        info.put("nickname", user.getNickname());
        info.put("email", user.getEmail());
        info.put("avatar", user.getAvatar());
        info.put("role", user.getRole());
        return info;
    }

    // ==================== Request DTOs ====================

    @Data
    public static class RegisterRequest {
        @NotBlank(message = "用户名不能为空")
        private String username;

        @NotBlank(message = "密码不能为空")
        private String password;

        @NotBlank(message = "邮箱不能为空")
        private String email;

        @NotBlank(message = "验证码不能为空")
        private String emailCode;
    }

    @Data
    public static class LoginRequest {
        @NotBlank(message = "用户名不能为空")
        private String username;

        @NotBlank(message = "密码不能为空")
        private String password;

        /**
         * 验证码Key（需要验证码时必填）
         */
        private String captchaKey;

        /**
         * 验证码值（需要验证码时必填）
         */
        private String captchaCode;
    }

    @Data
    public static class ChangePasswordRequest {
        @NotBlank(message = "原密码不能为空")
        private String oldPassword;

        @NotBlank(message = "新密码不能为空")
        private String newPassword;
    }
}
