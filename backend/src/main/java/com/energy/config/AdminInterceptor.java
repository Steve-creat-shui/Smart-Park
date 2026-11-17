package com.energy.config;

import com.energy.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AdminInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            sendForbidden(response, "未登录");
            return false;
        }
        String token = header.substring(7);
        try {
            Claims claims = jwtUtil.parseToken(token);
            String role = claims.get("role", String.class);
            if (!"admin".equals(role)) {
                sendForbidden(response, "权限不足，需要管理员权限");
                return false;
            }
            return true;
        } catch (Exception e) {
            sendForbidden(response, "Token 无效");
            return false;
        }
    }

    private void sendForbidden(HttpServletResponse response, String message) throws Exception {
        response.setStatus(403);
        response.setContentType("application/json;charset=UTF-8");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", 403);
        body.put("message", message);
        body.put("data", null);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
