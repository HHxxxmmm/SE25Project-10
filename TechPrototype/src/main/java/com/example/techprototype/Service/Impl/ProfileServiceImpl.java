package com.example.techprototype.Service.Impl;

import com.example.techprototype.DTO.*;
import com.example.techprototype.Entity.Passenger;
import com.example.techprototype.Entity.User;
import com.example.techprototype.Entity.UserPassengerRelation;
import com.example.techprototype.Enums.PassengerType;
import com.example.techprototype.Repository.PassengerRepository;
import com.example.techprototype.Repository.UserRepository;
import com.example.techprototype.Repository.UserPassengerRelationRepository;
import com.example.techprototype.Service.ProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ProfileServiceImpl implements ProfileService {

    private final UserRepository userRepository;
    private final PassengerRepository passengerRepository;
    private final UserPassengerRelationRepository userPassengerRelationRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Autowired
    public ProfileServiceImpl(UserRepository userRepository, PassengerRepository passengerRepository, UserPassengerRelationRepository userPassengerRelationRepository) {
        this.userRepository = userRepository;
        this.passengerRepository = passengerRepository;
        this.userPassengerRelationRepository = userPassengerRelationRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @Override
    public ProfileResponse getUserProfile(Long userId) {
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return ProfileResponse.failure("用户不存在");
            }

            User user = userOpt.get();
            ProfileResponse.UserProfileData profileData = new ProfileResponse.UserProfileData();
            profileData.setUserId(user.getUserId());
            profileData.setRealName(user.getRealName());
            profileData.setEmail(user.getEmail());
            profileData.setPhoneNumber(user.getPhoneNumber());
            profileData.setRegistrationTime(user.getRegistrationTime());
            profileData.setLastLoginTime(user.getLastLoginTime());
            profileData.setAccountStatus(user.getAccountStatus());
            profileData.setRelatedPassenger(user.getRelatedPassenger());

            // 查找用户的所有关联乘客
            List<UserPassengerRelation> relations = userPassengerRelationRepository.findByUserId(userId);
            List<ProfileResponse.PassengerData> passengerDataList = new ArrayList<>();
            
            for (UserPassengerRelation relation : relations) {
                Optional<Passenger> passengerOpt = passengerRepository.findById(relation.getPassengerId());
                
                if (passengerOpt.isPresent()) {
                    Passenger passenger = passengerOpt.get();
                    ProfileResponse.PassengerData passengerData = new ProfileResponse.PassengerData();
                    passengerData.setPassengerId(passenger.getPassengerId());
                    passengerData.setIdCardNumber(passenger.getIdCardNumber());
                    passengerData.setRealName(passenger.getRealName());
                    passengerData.setPassengerType(passenger.getPassengerType());

                    // 获取乘客类型文本
                    try {
                        PassengerType type = PassengerType.fromCode(passenger.getPassengerType());
                        passengerData.setPassengerTypeText(type.getDescription());
                    } catch (IllegalArgumentException e) {
                        passengerData.setPassengerTypeText("未知");
                    }

                    passengerData.setPhoneNumber(passenger.getPhoneNumber());
                    
                    // 只有学生类型才显示学生票剩余次数
                    if (passenger.getPassengerType() == 3) { // 学生类型
                        passengerData.setStudentTypeLeft(passenger.getStudentTypeLeft());
                    } else {
                        passengerData.setStudentTypeLeft(null);
                    }

                    passengerDataList.add(passengerData);
                }
            }
            
            profileData.setLinkedPassengers(passengerDataList);

            return ProfileResponse.success(profileData);

        } catch (Exception e) {
            System.err.println("获取用户个人资料失败: " + e.getMessage());
            return ProfileResponse.failure("获取用户个人资料失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public ProfileResponse updateUserProfile(Long userId, ProfileRequest request) {
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return ProfileResponse.failure("用户不存在");
            }

            User user = userOpt.get();

            // 如果要更新的邮箱与当前邮箱不同，检查新邮箱是否已被使用
            if (request.getEmail() != null && !request.getEmail().isEmpty() &&
                    !request.getEmail().equals(user.getEmail())) {
                if (userRepository.existsByEmail(request.getEmail())) {
                    return ProfileResponse.failure("邮箱已被其他用户使用");
                }
                user.setEmail(request.getEmail());
            }

            // 如果要更新的手机号与当前手机号不同，检查新手机号是否已被使用
            if (request.getPhoneNumber() != null && !request.getPhoneNumber().isEmpty() &&
                    !request.getPhoneNumber().equals(user.getPhoneNumber())) {
                if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
                    return ProfileResponse.failure("手机号已被其他用户使用");
                }
                user.setPhoneNumber(request.getPhoneNumber());
            }

            // 更新用户真实姓名
            if (request.getRealName() != null && !request.getRealName().isEmpty()) {
                user.setRealName(request.getRealName());
            }

            // 保存更新后的用户信息
            userRepository.save(user);

            // 返回更新后的用户信息
            return getUserProfile(userId);

        } catch (Exception e) {
            System.err.println("更新用户个人资料失败: " + e.getMessage());
            return ProfileResponse.failure("更新用户个人资料失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public ChangePasswordResponse changePassword(ChangePasswordRequest request) {
        try {
            Optional<User> userOpt = userRepository.findById(request.getUserId());
            if (userOpt.isEmpty()) {
                return ChangePasswordResponse.failure("用户不存在");
            }

            User user = userOpt.get();

            // 验证当前密码
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
                return ChangePasswordResponse.failure("当前密码不正确");
            }

            // 验证新密码合法性
            if (request.getNewPassword() == null || request.getNewPassword().length() < 6) {
                return ChangePasswordResponse.failure("新密码不能为空且长度不能少于6位");
            }

            // 更新密码
            user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
            userRepository.save(user);

            return ChangePasswordResponse.success();

        } catch (Exception e) {
            System.err.println("修改密码失败: " + e.getMessage());
            return ChangePasswordResponse.failure("修改密码失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public BasicResponse updateLastLoginTime(LastLoginUpdateRequest request) {
        try {
            Optional<User> userOpt = userRepository.findById(request.getUserId());
            if (userOpt.isEmpty()) {
                return BasicResponse.failure("用户不存在");
            }

            User user = userOpt.get();

            // 更新最后登录时间
            LocalDateTime loginTime = request.getLoginTime();
            if (loginTime == null) {
                loginTime = LocalDateTime.now();
            }

            user.setLastLoginTime(loginTime);
            userRepository.save(user);

            return BasicResponse.success("最后登录时间更新成功");

        } catch (Exception e) {
            System.err.println("更新最后登录时间失败: " + e.getMessage());
            return BasicResponse.failure("更新最后登录时间失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public BasicResponse updateAccountStatus(AccountStatusUpdateRequest request) {
        try {
            Optional<User> userOpt = userRepository.findById(request.getUserId());
            if (userOpt.isEmpty()) {
                return BasicResponse.failure("用户不存在");
            }

            User user = userOpt.get();

            // 验证账户状态值
            byte status = request.getAccountStatus();
            if (status != 0 && status != 1) {
                return BasicResponse.failure("账户状态值无效，必须是0(冻结)或1(正常)");
            }

            // 更新账户状态
            user.setAccountStatus(status);
            userRepository.save(user);

            String statusText = status == 1 ? "正常" : "冻结";
            return BasicResponse.success("账户状态已更新为：" + statusText);

        } catch (Exception e) {
            System.err.println("更新账户状态失败: " + e.getMessage());
            return BasicResponse.failure("更新账户状态失败: " + e.getMessage());
        }
    }
}