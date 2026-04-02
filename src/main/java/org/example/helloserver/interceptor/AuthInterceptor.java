package org.example.helloserver.interceptor;

import org.example.helloserver.common.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

public class AuthInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader("Authorization");
        if (token == null || token.isEmpty()) {
            response.setContentType("application/json; charset=UTF-8");
            String errorJson = String.format(
                    "{\"code\": %d, \"msg\":\"%s\"}",
                    ResultCode.TOKEN_INVALID.getCode(),
                    ResultCode.TOKEN_INVALID.getMsg()
            );
            response.getWriter().write(errorJson);
            return false;
        }
        return true;
    }
}
