package com.zzk.infrastructure.annotation;

import java.lang.annotation.*;

/**
 * 敏感词检测注解
 * 
 * <p>标记在方法上，AOP会自动检测指定参数的字段是否包含敏感词
 * 
 * @author zzk
 * @since 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SensitiveCheck {
    
    /**
     * 需要检测的字段名（支持嵌套，如 "request.content"）
     * 如果为空，则检测方法的所有 String 类型参数
     */
    String[] fields() default {};
    
    /**
     * 检测模式
     */
    Mode mode() default Mode.BLOCK;
    
    /**
     * 检测模式枚举
     */
    enum Mode {
        /**
         * 阻止模式：发现敏感词直接抛异常阻止执行
         */
        BLOCK,
        
        /**
         * 替换模式：将敏感词替换为 *** 后继续执行
         */
        REPLACE,
        
        /**
         * 警告模式：记录日志但不阻止执行
         */
        WARN
    }
}
