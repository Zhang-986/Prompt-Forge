package com.zzk.infrastructure.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 缓存配置
 * 
 * <p>实现多级缓存架构：
 * <ul>
 *   <li>L1: Caffeine 本地缓存 - 高性能，低延迟</li>
 *   <li>L2: Redis 分布式缓存 - 跨实例共享</li>
 * </ul>
 * 
 * @author zzk
 * @since 1.0.0
 */
@Slf4j
@Configuration
@EnableCaching
public class CacheConfig {

    // ==================== Caffeine 配置 ====================

    @Value("${cache.caffeine.prompt.initial-capacity:100}")
    private int promptInitialCapacity;

    @Value("${cache.caffeine.prompt.maximum-size:1000}")
    private int promptMaximumSize;

    @Value("${cache.caffeine.prompt.expire-after-write-minutes:5}")
    private int promptExpireMinutes;

    // ==================== Redis 配置 ====================

    @Value("${cache.redis.prompt.expire-minutes:30}")
    private int redisPromptExpireMinutes;

    @Value("${cache.redis.user.expire-minutes:60}")
    private int redisUserExpireMinutes;

    /**
     * Caffeine 本地缓存管理器 (L1)
     * 
     * <p>特点：
     * <ul>
     *   <li>内存级别访问速度</li>
     *   <li>自动过期淘汰</li>
     *   <li>不支持分布式</li>
     * </ul>
     */
    @Bean("caffeineCacheManager")
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .initialCapacity(promptInitialCapacity)
                .maximumSize(promptMaximumSize)
                .expireAfterWrite(promptExpireMinutes, TimeUnit.MINUTES)
                .recordStats() // 开启统计，便于监控
        );
        
        log.info("Caffeine 缓存管理器初始化完成: maxSize={}, expireMinutes={}", 
                promptMaximumSize, promptExpireMinutes);
        
        return cacheManager;
    }

    /**
     * Redis 分布式缓存管理器 (L2)
     * 
     * <p>特点：
     * <ul>
     *   <li>支持分布式部署</li>
     *   <li>数据持久化</li>
     *   <li>支持集群</li>
     * </ul>
     */
    @Bean("redisCacheManager")
    @Primary
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        // 创建支持 Java 8 时间类型的 ObjectMapper
        com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        objectMapper.setVisibility(com.fasterxml.jackson.annotation.PropertyAccessor.ALL, 
                com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY);
        objectMapper.activateDefaultTyping(
                com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator.instance,
                com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping.NON_FINAL
        );
        // 忽略未知属性（兼容旧缓存数据）
        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);
        
        // 默认配置
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(redisPromptExpireMinutes))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(jsonSerializer))
                .disableCachingNullValues(); // 不缓存空值，防止缓存穿透

        // 不同缓存的个性化配置
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // Prompt 缓存：30分钟过期
        cacheConfigurations.put("prompt", defaultConfig
                .entryTtl(Duration.ofMinutes(redisPromptExpireMinutes)));
        
        // 用户缓存：60分钟过期
        cacheConfigurations.put("user", defaultConfig
                .entryTtl(Duration.ofMinutes(redisUserExpireMinutes)));
        
        // 版本缓存：1小时过期
        cacheConfigurations.put("promptVersion", defaultConfig
                .entryTtl(Duration.ofHours(1)));

        RedisCacheManager cacheManager = RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();

        log.info("Redis 缓存管理器初始化完成: promptExpire={}min, userExpire={}min", 
                redisPromptExpireMinutes, redisUserExpireMinutes);

        return cacheManager;
    }
}
