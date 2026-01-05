package com.zzk.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

/**
 * 邮箱验证码服务
 * 
 * @author zzk
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final StringRedisTemplate redisTemplate;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${email.verification.code-length:6}")
    private int codeLength;

    @Value("${email.verification.expiration-minutes:5}")
    private int expirationMinutes;

    @Value("${email.verification.resend-interval-seconds:60}")
    private int resendIntervalSeconds;

    private static final String CODE_KEY_PREFIX = "email:verification:code:";
    private static final String COOLDOWN_KEY_PREFIX = "email:verification:cooldown:";
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * 发送验证码到指定邮箱
     * 
     * @param email 目标邮箱
     * @return 剩余冷却时间（秒），0表示发送成功
     */
    public int sendVerificationCode(String email) {
        // 检查冷却时间
        Long cooldown = getResendCooldown(email);
        if (cooldown > 0) {
            log.info("邮箱 {} 还在冷却期，剩余 {} 秒", email, cooldown);
            return cooldown.intValue();
        }

        // 生成验证码
        String code = generateCode();

        // 存储到 Redis
        String codeKey = CODE_KEY_PREFIX + email;
        String cooldownKey = COOLDOWN_KEY_PREFIX + email;
        
        redisTemplate.opsForValue().set(codeKey, code, expirationMinutes, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(cooldownKey, "1", resendIntervalSeconds, TimeUnit.SECONDS);

        // 发送邮件
        try {
            sendEmail(email, code);
            log.info("验证码已发送到邮箱: {}", email);
            return 0;
        } catch (Exception e) {
            log.error("发送验证码失败: {}", e.getMessage(), e);
            // 删除已存储的验证码和冷却计时
            redisTemplate.delete(codeKey);
            redisTemplate.delete(cooldownKey);
            throw new RuntimeException("发送验证码失败，请稍后重试");
        }
    }

    /**
     * 验证邮箱验证码
     * 
     * @param email 邮箱
     * @param code 验证码
     * @return 是否验证成功
     */
    public boolean verifyCode(String email, String code) {
        if (email == null || code == null) {
            return false;
        }
        
        String codeKey = CODE_KEY_PREFIX + email;
        String storedCode = redisTemplate.opsForValue().get(codeKey);
        
        if (storedCode != null && storedCode.equals(code)) {
            // 验证成功后删除验证码，防止重复使用
            redisTemplate.delete(codeKey);
            return true;
        }
        
        return false;
    }

    /**
     * 获取重发冷却剩余时间
     * 
     * @param email 邮箱
     * @return 剩余秒数，0表示可以重发
     */
    public Long getResendCooldown(String email) {
        String cooldownKey = COOLDOWN_KEY_PREFIX + email;
        Long ttl = redisTemplate.getExpire(cooldownKey, TimeUnit.SECONDS);
        return ttl != null && ttl > 0 ? ttl : 0L;
    }

    /**
     * 生成随机验证码
     */
    private String generateCode() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < codeLength; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }

    /**
     * 发送验证码邮件
     */
    private void sendEmail(String toEmail, String code) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(toEmail);
        helper.setSubject("【Prompt-Forge】邮箱验证码");

        String htmlContent = buildEmailContent(code);
        helper.setText(htmlContent, true);

        mailSender.send(message);
    }

    /**
     * 构建邮件正文 HTML
     */
    private String buildEmailContent(String code) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background-color: #f5f5f5; margin: 0; padding: 20px; }
                    .container { max-width: 500px; margin: 0 auto; background: #ffffff; border-radius: 8px; border: 1px solid #e0e0e0; }
                    .header { padding: 24px; border-bottom: 1px solid #eee; }
                    .header h1 { color: #333; margin: 0; font-size: 18px; font-weight: 600; }
                    .content { padding: 32px 24px; }
                    .message { color: #555; font-size: 14px; line-height: 1.6; margin: 0 0 24px 0; }
                    .code-box { background: #f8f8f8; padding: 16px 24px; border-radius: 6px; text-align: center; border: 1px solid #eee; }
                    .code { font-size: 28px; font-weight: 600; color: #333; letter-spacing: 6px; font-family: 'Courier New', monospace; }
                    .hint { color: #888; font-size: 13px; margin-top: 24px; }
                    .footer { padding: 16px 24px; border-top: 1px solid #eee; text-align: center; color: #999; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Prompt-Forge</h1>
                    </div>
                    <div class="content">
                        <p class="message">您好，您正在注册账号，验证码如下：</p>
                        <div class="code-box">
                            <span class="code">%s</span>
                        </div>
                        <p class="hint">验证码 %d 分钟内有效，请勿泄露给他人。</p>
                    </div>
                    <div class="footer">
                        如非本人操作，请忽略此邮件
                    </div>
                </div>
            </body>
            </html>
            """.formatted(code, expirationMinutes);
    }
}
