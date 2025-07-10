package com.example.techprototype.DAO;

import com.example.techprototype.Entity.User;
import java.util.Optional;

public interface UserDAO {
    
    /**
     * 根据手机号查找用户
     * @param phoneNumber 手机号
     * @return 用户对象，可能为空
     */
    Optional<User> findByPhoneNumber(String phoneNumber);
    
    /**
     * 保存用户信息
     * @param user 用户对象
     * @return 保存后的用户对象
     */
    User save(User user);
    
    /**
     * 根据用户ID查找用户
     * @param userId 用户ID
     * @return 用户对象，可能为空
     */
    Optional<User> findById(Long userId);
    
    /**
     * 根据邮箱查找用户
     * @param email 邮箱地址
     * @return 用户对象，可能为空
     */
    Optional<User> findByEmail(String email);
    
    /**
     * 更新用户信息
     * @param user 用户对象
     */
    void update(User user);
}
