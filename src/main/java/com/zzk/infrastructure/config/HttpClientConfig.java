package com.zzk.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * HTTP 客户端配置
 * 
 * @author zzk
 * @since 1.0.0
 */
@Configuration
public class HttpClientConfig {

    /**
     * RestTemplate Bean - 用于 HTTP 请求
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
