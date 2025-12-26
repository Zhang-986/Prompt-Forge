package com.zzk.infrastructure.interceptor;

import com.alibaba.fastjson2.JSON;
import com.zzk.interfaces.dto.response.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录拦截器
 * 
 * @author zzk
 * @since 1.0.0
 */
@Slf4j
@Component
public class LoginInterceptor implements HandlerInterceptor {

    // Token 存储（与 UserController 共享，生产环境应使用 Redis）
    public static final Map<String, Long> TOKEN_STORE = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取 Authorization Header
        String authorization = request.getHeader("Authorization");
        
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);
            Long userId = TOKEN_STORE.get(token);
            
            if (userId != null) {
                // Token 有效，将用户 ID 放入 request
                request.setAttribute("userId", userId);
                return true;
            }
        }
        
        // Token 无效，返回 401
        sendUnauthorized(response);
        return false;
    }
    
    private void sendUnauthorized(HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        Result<?> result = Result.error(401, "请先登录");
        response.getWriter().write(JSON.toJSONString(result));
    }
}
