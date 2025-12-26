package com.zzk.infrastructure.config;

import cn.hutool.core.thread.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 线程池配置
 * 
 * <p>为不同业务场景配置独立的线程池：
 * <ul>
 *   <li>arenaExecutor - 竞技场并行调用线程池</li>
 *   <li>asyncExecutor - 异步任务线程池</li>
 * </ul>
 * 
 * @author zzk
 * @since 1.0.0
 */
@Slf4j
@Configuration
@EnableAsync
public class ThreadPoolConfig {

    // ==================== Arena 线程池配置 ====================

    @Value("${thread-pool.arena.core-size:10}")
    private int arenaCoreSize;

    @Value("${thread-pool.arena.max-size:50}")
    private int arenaMaxSize;

    @Value("${thread-pool.arena.queue-capacity:200}")
    private int arenaQueueCapacity;

    @Value("${thread-pool.arena.keep-alive-seconds:60}")
    private int arenaKeepAliveSeconds;

    @Value("${thread-pool.arena.thread-name-prefix:arena-pool-}")
    private String arenaThreadNamePrefix;

    // ==================== Async 线程池配置 ====================

    @Value("${thread-pool.async.core-size:5}")
    private int asyncCoreSize;

    @Value("${thread-pool.async.max-size:20}")
    private int asyncMaxSize;

    @Value("${thread-pool.async.queue-capacity:100}")
    private int asyncQueueCapacity;

    @Value("${thread-pool.async.keep-alive-seconds:60}")
    private int asyncKeepAliveSeconds;

    @Value("${thread-pool.async.thread-name-prefix:async-pool-}")
    private String asyncThreadNamePrefix;

    /**
     * 竞技场线程池
     * 
     * <p>用于并行调用多个 AI 模型
     * 
     * <p>配置说明：
     * <ul>
     *   <li>核心线程数: 10 - 保证基本并发能力</li>
     *   <li>最大线程数: 50 - 应对突发流量</li>
     *   <li>队列容量: 200 - 缓冲突发请求</li>
     *   <li>拒绝策略: CallerRunsPolicy - 由调用者线程执行，实现优雅降级</li>
     * </ul>
     */
    @Bean("arenaExecutor")
    public ThreadPoolExecutor arenaExecutor() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                arenaCoreSize,
                arenaMaxSize,
                arenaKeepAliveSeconds,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(arenaQueueCapacity),
                new ThreadFactoryBuilder()
                        .setNamePrefix(arenaThreadNamePrefix + "%d")
                        .setUncaughtExceptionHandler((t, e) -> 
                                log.error("Arena 线程异常: thread={}, error={}", t.getName(), e.getMessage(), e))
                        .build(),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        log.info("Arena 线程池初始化完成: coreSize={}, maxSize={}, queueCapacity={}", 
                arenaCoreSize, arenaMaxSize, arenaQueueCapacity);

        return executor;
    }

    /**
     * 异步任务线程池
     * 
     * <p>用于执行异步任务，如批量测试、导出报告等
     */
    @Bean("asyncExecutor")
    public ThreadPoolTaskExecutor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(asyncCoreSize);
        executor.setMaxPoolSize(asyncMaxSize);
        executor.setQueueCapacity(asyncQueueCapacity);
        executor.setKeepAliveSeconds(asyncKeepAliveSeconds);
        executor.setThreadNamePrefix(asyncThreadNamePrefix);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();

        log.info("Async 线程池初始化完成: coreSize={}, maxSize={}, queueCapacity={}", 
                asyncCoreSize, asyncMaxSize, asyncQueueCapacity);

        return executor;
    }
}
