package com.example.techprototype.Service;

import com.example.techprototype.DTO.LoginDTO;
import com.example.techprototype.DTO.RegisterDTO;
import com.example.techprototype.DTO.UserDTO;
import com.example.techprototype.Entity.User;

public interface UserService {
    
    /**
     * 用户登录
     * @param loginDTO 登录信息
     * @return 用户信息
     * @throws Exception 登录失败异常
     */
    UserDTO login(LoginDTO loginDTO) throws Exception;
    
    /**
     * 用户注册
     * @param registerDTO 注册信息
     * @return 注册成功的用户信息
     * @throws Exception 注册失败异常
     */
    UserDTO register(RegisterDTO registerDTO) throws Exception;
    
    /**
     * 通过ID查询用户
     * @param userId 用户ID
     * @return 用户信息
     */
    User getUserById(Long userId);
    
    /**
     * 通过手机号查询用户
     * @param phoneNumber 手机号
     * @return 用户信息
     */
    User getUserByPhoneNumber(String phoneNumber);
    
    /**
     * 校验身份证合法性
     * @param idCardNumber 身份证号
     * @return 是否合法
     */
    boolean validateIdCard(String idCardNumber);
    
    /**
     * 生成MD5密码
     * @param password 原始密码
     * @return MD5加密后的密码
     */
    String generateMD5Password(String password);
}
