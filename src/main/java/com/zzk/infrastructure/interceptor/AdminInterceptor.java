package com.zzk.infrastructure.interceptor;

import com.alibaba.fastjson2.JSON;
import com.zzk.domain.model.aggregate.User;
import com.zzk.domain.repository.UserRepository;
import com.zzk.infrastructure.util.JwtUtil;
import com.zzk.interfaces.dto.response.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * 管理员权限拦截器
 * 
 * <p>校验用户是否具有管理员权限，用于保护管理员专属接口
 * 
 * @author zzk
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 放行 OPTIONS 请求 (CORS 预检)
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 获取已认证的用户ID（由 LoginInterceptor 设置）
        Long userId = (Long) request.getAttribute("userId");
        
        if (userId == null) {
            sendForbidden(response, "未登录");
            return false;
        }

        // 查询用户信息，验证是否为管理员
        User user = userRepository.findById(userId).orElse(null);
        
        if (user == null) {
            sendForbidden(response, "用户不存在");
            return false;
        }

        if (!user.isAdmin()) {
            log.warn("非管理员用户尝试访问管理接口: userId={}, role={}", userId, user.getRole());
            sendForbidden(response, "需要管理员权限");
            return false;
        }

        log.debug("管理员权限验证通过: userId={}", userId);
        return true;
    }

    private void sendForbidden(HttpServletResponse response, String message) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        Result<?> result = Result.error(403, message);
        response.getWriter().write(JSON.toJSONString(result));
    }
}
