package com.example.tiktok.config;

import com.example.tiktok.entity.user.User;
import com.example.tiktok.holder.UserHolder;
import com.example.tiktok.service.user.UserService;
import com.example.tiktok.utils.JwtUtils;
import com.example.tiktok.utils.R;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

@Component
public class Interceptor implements HandlerInterceptor {

    private ObjectMapper objectMapper = new ObjectMapper();

    private UserService userService;

    public Interceptor (UserService userService) {
        this.userService = userService;
    }

    private boolean response(R r, HttpServletResponse response) throws IOException {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Cache-Control", "no-cache");
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.getWriter().println(objectMapper.writeValueAsString(r));
        // flush()表示强制将缓冲区中的数据发送出去，不必等到缓冲区满
        response.getWriter().flush();
        return false;
    }

    @Override
    public boolean preHandle (HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (request.getMethod().equals("OPTIONS")) {
            return true;
        }

        if(!JwtUtils.checkToken(request)) {
            return response(R.error().message("请登录后再操作"), response);
        }

        final Long userId = JwtUtils.getUserId(request);
        final User user = userService.getById(userId);
        if (ObjectUtils.isEmpty(user)) {
            return response(R.error().message("用户不存在"), response);
        }

        UserHolder.set(userId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 清空ThreadLocal中的数据
        UserHolder.clear();
    }
}
