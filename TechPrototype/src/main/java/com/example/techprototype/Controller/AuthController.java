package com.example.techprototype.Controller;

import com.example.techprototype.DTO.LoginDTO;
import com.example.techprototype.DTO.RegisterDTO;
import com.example.techprototype.DTO.UserDTO;
import com.example.techprototype.Service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    
    @Autowired
    private UserService userService;
    
    /**
     * 用户登录接口
     *
     * @param loginDTO 登录信息
     * @param response HTTP响应对象
     * @param session  HTTP会话对象
     * @return 登录结果
     */
    @PostMapping("/login")
    public ResponseEntity<Object> login(@RequestBody LoginDTO loginDTO, 
                                      HttpServletResponse response,
                                      HttpSession session) {
        logger.info("接收到登录请求: {}", loginDTO.getPhoneNumber());
        Map<String, Object> responseData = new HashMap<>();
        
        try {
            // 登录认证
            UserDTO userDTO = userService.login(loginDTO);
            
            // 存入session
            session.setAttribute("userId", userDTO.getUserId());
            session.setAttribute("realName", userDTO.getRealName());
            session.setAttribute("phoneNumber", userDTO.getPhoneNumber());
            session.setAttribute("token", userDTO.getToken());
            
            // 添加cookie
            Cookie userCookie = new Cookie("mini12306_auth", "true");
            userCookie.setPath("/");
            userCookie.setMaxAge(24 * 60 * 60); // 24小时
            userCookie.setHttpOnly(true);
            response.addCookie(userCookie);
            
            // 构建响应
            responseData.put("success", true);
            responseData.put("message", "登录成功");
            responseData.put("user", userDTO);
            
            logger.info("用户 {} 登录成功", userDTO.getRealName());
            return ResponseEntity.ok(responseData);
        } catch (Exception e) {
            logger.error("登录失败: {}", e.getMessage());
            responseData.put("success", false);
            responseData.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(responseData);
        }
    }
    
    /**
     * 用户注册接口
     *
     * @param registerDTO 注册信息
     * @param response    HTTP响应对象
     * @param session     HTTP会话对象
     * @return 注册结果
     */
    @PostMapping("/register")
    public ResponseEntity<Object> register(@RequestBody RegisterDTO registerDTO,
                                         HttpServletResponse response,
                                         HttpSession session) {
        logger.info("接收到注册请求: {}", registerDTO.getPhoneNumber());
        Map<String, Object> responseData = new HashMap<>();
        
        try {
            // 注册用户
            UserDTO userDTO = userService.register(registerDTO);
            
            // 存入session
            session.setAttribute("userId", userDTO.getUserId());
            session.setAttribute("realName", userDTO.getRealName());
            session.setAttribute("phoneNumber", userDTO.getPhoneNumber());
            session.setAttribute("token", userDTO.getToken());
            
            // 添加cookie
            Cookie userCookie = new Cookie("mini12306_auth", "true");
            userCookie.setPath("/");
            userCookie.setMaxAge(24 * 60 * 60); // 24小时
            userCookie.setHttpOnly(true);
            response.addCookie(userCookie);
            
            // 构建响应
            responseData.put("success", true);
            responseData.put("message", "注册成功");
            responseData.put("user", userDTO);
            
            logger.info("用户 {} 注册成功", userDTO.getRealName());
            return ResponseEntity.status(HttpStatus.CREATED).body(responseData);
        } catch (Exception e) {
            logger.error("注册失败: {}", e.getMessage());
            responseData.put("success", false);
            responseData.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseData);
        }
    }
    
    /**
     * 用户登出接口
     *
     * @param request  HTTP请求对象
     * @param response HTTP响应对象
     * @param session  HTTP会话对象
     * @return 登出结果
     */
    @PostMapping("/logout")
    public ResponseEntity<Object> logout(HttpServletRequest request, 
                                       HttpServletResponse response, 
                                       HttpSession session) {
        logger.info("接收到登出请求");
        Map<String, Object> responseData = new HashMap<>();
        
        // 清除session
        session.invalidate();
        
        // 清除cookie
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("mini12306_auth".equals(cookie.getName())) {
                    cookie.setValue("");
                    cookie.setPath("/");
                    cookie.setMaxAge(0);
                    response.addCookie(cookie);
                    break;
                }
            }
        }
        
        // 构建响应
        responseData.put("success", true);
        responseData.put("message", "登出成功");
        
        logger.info("用户登出成功");
        return ResponseEntity.ok(responseData);
    }
    
    /**
     * 获取当前登录用户信息
     *
     * @param session HTTP会话对象
     * @return 用户信息
     */
    @GetMapping("/currentUser")
    public ResponseEntity<Object> getCurrentUser(HttpSession session) {
        logger.info("接收到获取当前用户信息请求");
        Map<String, Object> responseData = new HashMap<>();
        
        Long userId = (Long) session.getAttribute("userId");
        if (userId != null) {
            UserDTO userDTO = new UserDTO();
            userDTO.setUserId(userId);
            userDTO.setRealName((String) session.getAttribute("realName"));
            userDTO.setPhoneNumber((String) session.getAttribute("phoneNumber"));
            userDTO.setToken((String) session.getAttribute("token"));
            
            responseData.put("success", true);
            responseData.put("user", userDTO);
            
            logger.info("返回当前用户信息: {}", userDTO.getRealName());
            return ResponseEntity.ok(responseData);
        } else {
            responseData.put("success", false);
            responseData.put("message", "未登录");
            
            logger.info("当前用户未登录");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(responseData);
        }
    }
}
