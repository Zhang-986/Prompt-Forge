package com.zzk.domain.service.auth;

import com.zzk.domain.model.valueobject.CaptchaResult;

/**
 * 验证码服务接口
 * 
 * <p>定义验证码的生成与校验抽象，由基础设施层使用 Hutool Captcha 实现。
 * 
 * @author zzk
 * @since 1.0.0
 */
public interface CaptchaService {

    /**
     * 生成验证码
     * 
     * @return 验证码结果，包含 key 和 Base64 图片
     */
    CaptchaResult generateCaptcha();

    /**
     * 校验验证码
     * 
     * @param captchaKey  验证码Key
     * @param captchaCode 用户输入的验证码
     * @return true 表示校验通过
     */
    boolean verifyCaptcha(String captchaKey, String captchaCode);
}
