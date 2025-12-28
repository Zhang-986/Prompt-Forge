package com.zzk.domain.model.valueobject;

/**
 * 登录结果枚举
 * 
 * @author zzk
 * @since 1.0.0
 */
public enum LoginResult {
    
    /**
     * 登录成功
     */
    SUCCESS,
    
    /**
     * 密码错误
     */
    FAILED_PASSWORD,
    
    /**
     * 账号被封禁
     */
    BANNED,
    
    /**
     * 验证码错误
     */
    CAPTCHA_FAILED,
    
    /**
     * 用户不存在
     */
    USER_NOT_FOUND,
    
    /**
     * 账号已禁用
     */
    ACCOUNT_DISABLED
}
