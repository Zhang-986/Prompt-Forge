package com.zzk.infrastructure.auth;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.core.lang.UUID;
import com.zzk.domain.model.valueobject.CaptchaResult;
import com.zzk.domain.service.auth.CaptchaService;
import com.zzk.domain.service.auth.LoginGuardConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Hutool 验证码服务实现
 * 
 * <p>使用 Hutool CaptchaUtil 生成验证码图片，使用 Redisson 存储验证码。
 * 
 * @author zzk
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HutoolCaptchaService implements CaptchaService {

    private final RedissonClient redissonClient;
    private final LoginGuardConfig config;

    /**
     * 验证码 Key 前缀
     */
    private static final String CAPTCHA_PREFIX = "captcha:";

    /**
     * 验证码图片宽度
     */
    private static final int CAPTCHA_WIDTH = 200;

    /**
     * 验证码图片高度
     */
    private static final int CAPTCHA_HEIGHT = 80;

    /**
     * 验证码字符数
     */
    private static final int CAPTCHA_CODE_COUNT = 4;

    /**
     * 干扰线数量
     */
    private static final int CAPTCHA_LINE_COUNT = 50;

    @Override
    public CaptchaResult generateCaptcha() {
        // 生成验证码
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(
                CAPTCHA_WIDTH, 
                CAPTCHA_HEIGHT, 
                CAPTCHA_CODE_COUNT, 
                CAPTCHA_LINE_COUNT
        );
        
        // 生成唯一标识
        String captchaKey = CAPTCHA_PREFIX + UUID.fastUUID().toString(true);
        String code = captcha.getCode();
        
        // 存储到 Redis
        RBucket<String> bucket = redissonClient.getBucket(captchaKey);
        bucket.set(code, config.getCaptchaExpiration().toMillis(), TimeUnit.MILLISECONDS);
        
        // 获取 Base64 编码的图片
        String imageBase64 = captcha.getImageBase64Data();
        
        log.debug("生成验证码: key={}, code={}", captchaKey, code);
        
        return new CaptchaResult(captchaKey, imageBase64);
    }

    @Override
    public boolean verifyCaptcha(String captchaKey, String captchaCode) {
        if (captchaKey == null || captchaCode == null) {
            log.debug("验证码参数为空");
            return false;
        }
        
        RBucket<String> bucket = redissonClient.getBucket(captchaKey);
        String storedCode = bucket.get();
        
        if (storedCode == null) {
            log.debug("验证码不存在或已过期: key={}", captchaKey);
            return false;
        }
        
        // 不区分大小写比较
        boolean matched = storedCode.equalsIgnoreCase(captchaCode);
        
        // 无论成功失败都删除验证码，防止重放攻击
        bucket.delete();
        
        log.debug("验证码校验: key={}, matched={}", captchaKey, matched);
        
        return matched;
    }
}
