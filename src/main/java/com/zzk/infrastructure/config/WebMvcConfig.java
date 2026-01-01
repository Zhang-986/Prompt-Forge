package com.zzk.infrastructure.config;

import com.zzk.infrastructure.interceptor.AdminInterceptor;
import com.zzk.infrastructure.interceptor.LoginInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 * 
 * @author zzk
 * @since 1.0.0
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final LoginInterceptor loginInterceptor;
    private final AdminInterceptor adminInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 登录拦截器 - 拦截所有 API
        registry.addInterceptor(loginInterceptor)
                // 需要拦截的路径
                .addPathPatterns("/api/**")
                // 排除的路径（不需要登录）
                .excludePathPatterns(
                    // 用户登录注册
                    "/api/users/login",
                    "/api/users/register",
                    // 验证码相关（登录防护）
                    "/api/users/captcha",
                    "/api/users/login-check",
                    // Swagger 文档
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    // 静态资源
                    "/",
                    "/index.html",
                    "/login.html",
                    "/*.html",
                    "/css/**",
                    "/js/**",
                    "/favicon.ico",
                    "/error"
                );

        // 管理员拦截器 - 拦截管理员专属接口
        registry.addInterceptor(adminInterceptor)
                .addPathPatterns("/api/admin/**");
    }

    @Override
    public void addCorsMappings(org.springframework.web.servlet.config.annotation.CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
