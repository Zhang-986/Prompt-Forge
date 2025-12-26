package com.zzk.infrastructure.lock;

import com.zzk.infrastructure.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * 分布式锁切面
 * 
 * <p>使用 Redisson 实现分布式锁，支持：
 * <ul>
 *   <li>可重入锁</li>
 *   <li>锁等待超时</li>
 *   <li>锁自动续期</li>
 *   <li>SpEL 表达式解析锁 Key</li>
 * </ul>
 * 
 * @author zzk
 * @since 1.0.0
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class DistributedLockAspect {

    private final RedissonClient redissonClient;

    private static final SpelExpressionParser PARSER = new SpelExpressionParser();
    private static final DefaultParameterNameDiscoverer NAME_DISCOVERER = new DefaultParameterNameDiscoverer();

    private static final String LOCK_PREFIX = "lock:";

    @Around("@annotation(distributedLock)")
    public Object around(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) throws Throwable {
        // 1. 解析锁 Key
        String lockKey = LOCK_PREFIX + parseLockKey(joinPoint, distributedLock.key());
        long waitTime = distributedLock.waitTime();
        long leaseTime = distributedLock.leaseTime();

        log.debug("尝试获取分布式锁: key={}, waitTime={}s, leaseTime={}s", lockKey, waitTime, leaseTime);

        // 2. 获取锁
        RLock lock = redissonClient.getLock(lockKey);
        boolean acquired = false;

        try {
            // 尝试获取锁
            acquired = lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);
            
            if (!acquired) {
                log.warn("获取分布式锁失败: key={}", lockKey);
                throw new BusinessException("操作过于频繁，请稍后重试");
            }

            log.debug("获取分布式锁成功: key={}", lockKey);
            
            // 3. 执行业务逻辑
            return joinPoint.proceed();
            
        } finally {
            // 4. 释放锁
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("释放分布式锁: key={}", lockKey);
            }
        }
    }

    /**
     * 解析锁 Key（支持 SpEL 表达式）
     */
    private String parseLockKey(ProceedingJoinPoint joinPoint, String keyExpression) {
        // 获取方法签名
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // 获取参数名
        String[] parameterNames = NAME_DISCOVERER.getParameterNames(method);
        if (parameterNames == null) {
            return keyExpression;
        }

        // 构建 SpEL 上下文
        EvaluationContext context = new StandardEvaluationContext();
        Object[] args = joinPoint.getArgs();
        for (int i = 0; i < parameterNames.length; i++) {
            context.setVariable(parameterNames[i], args[i]);
        }

        // 解析表达式
        try {
            Expression expression = PARSER.parseExpression(keyExpression);
            Object value = expression.getValue(context);
            return value != null ? value.toString() : keyExpression;
        } catch (Exception e) {
            log.warn("SpEL 表达式解析失败: expression={}, error={}", keyExpression, e.getMessage());
            return keyExpression;
        }
    }
}
