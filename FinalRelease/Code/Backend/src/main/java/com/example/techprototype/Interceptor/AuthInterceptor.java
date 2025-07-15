package com.example.techprototype.Interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(AuthInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestURI = request.getRequestURI();
        
        // 对于不需要验证的路径，直接放行
        if (isPublicPath(requestURI)) {
            return true;
        }
        
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("userId") != null) {
            logger.debug("用户已认证，允许访问：{}", requestURI);
            return true;
        }
        
        logger.warn("拦截未认证访问：{}", requestURI);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"success\":false,\"message\":\"未登录或登录已过期\"}");
        return false;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        // 请求处理后调用
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 请求完成后调用
    }
    
    /**
     * 判断路径是否为公开访问
     * 
     * @param path 请求路径
     * @return 是否公开访问
     */
    private boolean isPublicPath(String path) {
        // 允许公开访问的路径
        String[] publicPaths = {
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/logout",
            "/api/auth/currentUser",
            "/api/trains/list",
            "/api/trains/direct",
            "/api/trains/byTime",
            "/api/trains/transfer",
            "/api/trains/search",
            "/error"
        };
        
        for (String publicPath : publicPaths) {
            if (path.startsWith(publicPath)) {
                return true;
            }
        }
        
        return false;
    }
}
