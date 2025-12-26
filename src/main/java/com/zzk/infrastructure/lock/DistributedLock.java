package com.zzk.infrastructure.lock;

import java.lang.annotation.*;

/**
 * 分布式锁注解
 * 
 * <p>使用示例：
 * <pre>
 * &#64;DistributedLock(key = "'prompt:commit:' + #promptId", waitTime = 5, leaseTime = 30)
 * public void commit(Long promptId) {
 *     // 业务逻辑
 * }
 * </pre>
 * 
 * @author zzk
 * @since 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DistributedLock {

    /**
     * 锁的 Key，支持 SpEL 表达式
     * 
     * <p>示例：
     * <ul>
     *   <li>'prompt:commit:' + #promptId - 拼接参数</li>
     *   <li>#user.id - 访问对象属性</li>
     * </ul>
     */
    String key();

    /**
     * 等待获取锁的最大时间（秒）
     * 
     * <p>如果在此时间内未获取到锁，将抛出异常
     */
    long waitTime() default 3;

    /**
     * 锁的租约时间（秒）
     * 
     * <p>获取锁后，锁的最大持有时间。
     * 超过此时间锁将自动释放，防止死锁。
     */
    long leaseTime() default 30;
}
