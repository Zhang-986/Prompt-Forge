package com.zzk.domain.model.valueobject;

/**
 * 验证码结果值对象
 * 
 * @author zzk
 * @since 1.0.0
 */
public record CaptchaResult(
    /**
     * 验证码唯一标识Key
     */
    String captchaKey,
    
    /**
     * Base64编码的验证码图片
     */
    String captchaImage
) {
}
