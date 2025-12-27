package com.zzk.infrastructure.interceptor;

import com.alibaba.fastjson2.JSON;
import com.zzk.infrastructure.util.JwtUtil;
import com.zzk.interfaces.dto.response.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * 登录拦截器 - 使用 JWT 验证
 * 
 * @author zzk
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 放行 OPTIONS 请求 (CORS 预检)
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 获取 Authorization Header
        String authorization = request.getHeader("Authorization");
        
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);
            
            // 使用 JWT 验证 Token
            Long userId = jwtUtil.getUserIdFromToken(token);
            
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
