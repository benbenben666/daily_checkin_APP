package com.ben.daily_check_in.security;

import com.ben.daily_check_in.entity.User;
import com.ben.daily_check_in.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录拦截器：解析 Authorization 头中的 JWT，
 * 并从数据库现查用户状态与角色（禁用、改角色立即生效）。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;

    public AuthInterceptor(JwtUtil jwtUtil, UserMapper userMapper) {
        this.jwtUtil = jwtUtil;
        this.userMapper = userMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // CORS 预检请求（OPTIONS）由浏览器自动发出，**不携带 Authorization 头**。
        // 必须在这里直接放行，否则预检失败，浏览器会拦掉后面真正的请求
        //（前端所有请求都带 Content-Type: application/json 或 Authorization，必然触发预检）。
        if (CorsUtils.isPreFlightRequest(request)) {
            return true;
        }
        String token = extractToken(request);
        if (token == null) {
            throw com.ben.daily_check_in.common.BizException.unauthorized("未登录");
        }
        Long userId = jwtUtil.parseUserId(token);
        if (userId == null) {
            throw com.ben.daily_check_in.common.BizException.unauthorized("登录已过期，请重新登录");
        }
        User user = userMapper.selectById(userId);
        if (user == null || user.getStatus() == 0) {
            throw com.ben.daily_check_in.common.BizException.unauthorized("账号已被禁用");
        }
        UserContext.set(new LoginUser(user.getId(), user.getPhone(), user.getNickname(), user.getSystemRole()));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear();
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
