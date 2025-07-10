package com.example.techprototype.Service.Impl;

import com.example.techprototype.DAO.PassengerDAO;
import com.example.techprototype.DAO.UserDAO;
import com.example.techprototype.DAO.UserPassengerRelationDAO;
import com.example.techprototype.DTO.LoginDTO;
import com.example.techprototype.DTO.RegisterDTO;
import com.example.techprototype.DTO.UserDTO;
import com.example.techprototype.Entity.Passenger;
import com.example.techprototype.Entity.User;
import com.example.techprototype.Entity.UserPassengerRelation;
import com.example.techprototype.Service.UserService;
import com.example.techprototype.Util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);
    
    @Autowired
    private UserDAO userDAO;
    
    @Autowired
    private PassengerDAO passengerDAO;
    
    @Autowired
    private UserPassengerRelationDAO userPassengerRelationDAO;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Override
    public UserDTO login(LoginDTO loginDTO) throws Exception {
        logger.info("用户登录: {}", loginDTO.getPhoneNumber());
        
        // 验证手机号
        Optional<User> userOptional = userDAO.findByPhoneNumber(loginDTO.getPhoneNumber());
        if (!userOptional.isPresent()) {
            logger.warn("登录失败: 手机号 {} 未注册", loginDTO.getPhoneNumber());
            throw new Exception("用户不存在");
        }
        
        User user = userOptional.get();
        
        // 验证密码
        String md5Password = generateMD5Password(loginDTO.getPassword());
        if (!md5Password.equals(user.getPasswordHash())) {
            logger.warn("登录失败: 密码错误, 用户手机号: {}", loginDTO.getPhoneNumber());
            throw new Exception("密码错误");
        }
        
        // 更新最后登录时间
        user.setLastLoginTime(LocalDateTime.now());
        userDAO.update(user);
        
        // 生成JWT令牌
        String token = jwtUtil.generateToken(user.getUserId());
        
        // 构建返回对象
        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(user.getUserId());
        userDTO.setRealName(user.getRealName());
        userDTO.setPhoneNumber(user.getPhoneNumber());
        userDTO.setEmail(user.getEmail());
        userDTO.setToken(token);
        userDTO.setPassengerId(user.getPassengerId());
        
        logger.info("用户 {} 登录成功", user.getRealName());
        return userDTO;
    }
    
    @Override
    @Transactional
    public UserDTO register(RegisterDTO registerDTO) throws Exception {
        logger.info("用户注册开始: {}", registerDTO.getPhoneNumber());
        
        // 验证手机号是否已注册
        if (userDAO.findByPhoneNumber(registerDTO.getPhoneNumber()).isPresent()) {
            logger.warn("注册失败: 手机号 {} 已被注册", registerDTO.getPhoneNumber());
            throw new Exception("手机号已被注册");
        }
        
        // 验证邮箱是否已注册(如果提供了邮箱)
        if (registerDTO.getEmail() != null && !registerDTO.getEmail().trim().isEmpty() && 
                userDAO.findByEmail(registerDTO.getEmail()).isPresent()) {
            logger.warn("注册失败: 邮箱 {} 已被注册", registerDTO.getEmail());
            throw new Exception("邮箱已被注册");
        }
        
        // 验证身份证号码合法性
        if (!validateIdCard(registerDTO.getIdCardNumber())) {
            logger.warn("注册失败: 身份证号 {} 不合法", registerDTO.getIdCardNumber());
            throw new Exception("身份证号码不合法");
        }
        
        // 创建或查找乘客
        Passenger passenger;
        Optional<Passenger> existingPassenger = passengerDAO.findByIdCardNumber(registerDTO.getIdCardNumber());
        
        if (existingPassenger.isPresent()) {
            passenger = existingPassenger.get();
            logger.info("找到已存在的乘客信息，ID: {}", passenger.getPassengerId());
        } else {
            passenger = new Passenger();
            passenger.setRealName(registerDTO.getRealName());
            passenger.setIdCardNumber(registerDTO.getIdCardNumber());
            passenger.setPassengerType((byte) 1); // 默认成人
            passenger.setPhoneNumber(registerDTO.getPhoneNumber());
            passenger.setStudentTypeLeft(0); // 默认非学生
            
            passenger = passengerDAO.save(passenger);
            logger.info("创建新乘客信息，ID: {}", passenger.getPassengerId());
        }
        
        // 创建用户
        User user = new User();
        user.setRealName(registerDTO.getRealName());
        user.setPasswordHash(generateMD5Password(registerDTO.getPassword()));
        user.setPhoneNumber(registerDTO.getPhoneNumber());
        user.setEmail(registerDTO.getEmail());
        user.setRegistrationTime(LocalDateTime.now());
        user.setAccountStatus((byte) 1); // 账号状态正常
        user.setRelatedPassenger(1); // 至少关联自己作为乘客
        user.setPassengerId(passenger.getPassengerId()); // 关联到自己的乘客信息
        
        user = userDAO.save(user);
        logger.info("创建用户成功, ID: {}", user.getUserId());
        
        // 创建用户与乘客的关联关系
        UserPassengerRelation relation = new UserPassengerRelation();
        relation.setUserId(user.getUserId());
        relation.setPassengerId(passenger.getPassengerId());
        relation.setRelationType((byte) 1); // 1-本人
        relation.setAddedTime(LocalDateTime.now());
        
        userPassengerRelationDAO.save(relation);
        logger.info("创建用户和乘客关系成功");
        
        // 生成JWT令牌
        String token = jwtUtil.generateToken(user.getUserId());
        
        // 构建返回对象
        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(user.getUserId());
        userDTO.setRealName(user.getRealName());
        userDTO.setPhoneNumber(user.getPhoneNumber());
        userDTO.setEmail(user.getEmail());
        userDTO.setToken(token);
        userDTO.setPassengerId(passenger.getPassengerId());
        
        logger.info("用户注册完成: {}", user.getRealName());
        return userDTO;
    }
    
    @Override
    public User getUserById(Long userId) {
        logger.info("根据ID查询用户: {}", userId);
        Optional<User> userOptional = userDAO.findById(userId);
        return userOptional.orElse(null);
    }
    
    @Override
    public User getUserByPhoneNumber(String phoneNumber) {
        logger.info("根据手机号查询用户: {}", phoneNumber);
        Optional<User> userOptional = userDAO.findByPhoneNumber(phoneNumber);
        return userOptional.orElse(null);
    }
    
    @Override
    public boolean validateIdCard(String idCardNumber) {
        logger.info("验证身份证号: {}", idCardNumber);
        
        // 身份证号码为18位，15位也是合法的
        if (idCardNumber == null || (idCardNumber.length() != 18 && idCardNumber.length() != 15)) {
            return false;
        }
        
        // 简单的正则表达式验证
        String regex = "^[1-9]\\d{5}(19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[0-9Xx]$";
        if (idCardNumber.length() == 18) {
            return Pattern.matches(regex, idCardNumber);
        } else {
            // 15位身份证转18位验证
            regex = "^[1-9]\\d{5}\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}$";
            return Pattern.matches(regex, idCardNumber);
        }
    }
    
    @Override
    public String generateMD5Password(String password) {
        logger.debug("生成MD5密码");
        
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(password.getBytes());
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : messageDigest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            logger.error("MD5加密失败: {}", e.getMessage());
            throw new RuntimeException("MD5加密失败", e);
        }
    }
}
