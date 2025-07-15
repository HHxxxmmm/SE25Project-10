package com.example.techprototype.Service.Impl;

import com.example.techprototype.DTO.AddPassengerRequest;
import com.example.techprototype.DTO.AddPassengerResponse;
import com.example.techprototype.DTO.BookingResponse;
import com.example.techprototype.DTO.CheckAddPassengerResponse;
import com.example.techprototype.DTO.DeletePassengerRequest;
import com.example.techprototype.Entity.Passenger;
import com.example.techprototype.Entity.User;
import com.example.techprototype.Entity.UserPassengerRelation;
import com.example.techprototype.Repository.PassengerRepository;
import com.example.techprototype.Repository.UserPassengerRelationRepository;
import com.example.techprototype.Repository.UserRepository;
import com.example.techprototype.Service.PassengerManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class PassengerManagementServiceImpl implements PassengerManagementService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PassengerRepository passengerRepository;
    
    @Autowired
    private UserPassengerRelationRepository userPassengerRelationRepository;
    
    @Override
    public CheckAddPassengerResponse checkCanAddPassenger(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return CheckAddPassengerResponse.notAllowed("用户不存在");
        }
        
        User user = userOpt.get();
        if (user.getRelatedPassenger() >= 3) {
            return CheckAddPassengerResponse.notAllowed("已达到最大乘车人数量限制（3人）");
        }
        
        return CheckAddPassengerResponse.allowed();
    }
    
    @Override
    @Transactional
    public AddPassengerResponse addPassenger(AddPassengerRequest request) {
        // 1. 验证用户是否存在
        Optional<User> userOpt = userRepository.findById(request.getUserId());
        if (userOpt.isEmpty()) {
            return AddPassengerResponse.failure("用户不存在");
        }
        
        User user = userOpt.get();
        
        // 2. 检查是否已达到最大乘车人数量
        if (user.getRelatedPassenger() >= 3) {
            return AddPassengerResponse.failure("已达到最大乘车人数量限制（3人）");
        }
        
        // 3. 根据身份证号查找乘客
        Optional<Passenger> passengerOpt = passengerRepository.findByIdCardNumber(request.getIdCardNumber());
        if (passengerOpt.isEmpty()) {
            return AddPassengerResponse.failure("该乘车人信息无效");
        }
        
        Passenger passenger = passengerOpt.get();
        
        // 4. 验证乘客信息是否匹配
        if (!validatePassengerInfo(passenger, request)) {
            return AddPassengerResponse.failure("该乘车人信息无效");
        }
        
        // 5. 检查是否已被添加
        if (userPassengerRelationRepository.existsByUserIdAndPassengerId(request.getUserId(), passenger.getPassengerId())) {
            return AddPassengerResponse.failure("该乘车人已被添加");
        }
        
        // 6. 创建用户乘客关系
        UserPassengerRelation relation = new UserPassengerRelation();
        relation.setUserId(request.getUserId());
        relation.setPassengerId(passenger.getPassengerId());
        
        // 判断关系类型：如果乘客ID与用户的passenger_id相同，则为本人，否则为其他
        if (passenger.getPassengerId().equals(user.getPassengerId())) {
            relation.setRelationType((byte) 1); // 本人
        } else {
            relation.setRelationType((byte) 3); // 其他
        }
        
        relation.setAddedTime(LocalDateTime.now());
        
        // 7. 保存关系并更新用户关联乘客数量
        UserPassengerRelation savedRelation = userPassengerRelationRepository.save(relation);
        user.setRelatedPassenger(user.getRelatedPassenger() + 1);
        userRepository.save(user);
        
        return AddPassengerResponse.success(savedRelation.getRelationId());
    }
    
    /**
     * 验证乘客信息是否匹配
     */
    private boolean validatePassengerInfo(Passenger passenger, AddPassengerRequest request) {
        return passenger.getRealName().equals(request.getRealName()) &&
               passenger.getIdCardNumber().equals(request.getIdCardNumber()) &&
               (passenger.getPhoneNumber() == null || 
                request.getPhoneNumber() == null || 
                passenger.getPhoneNumber().equals(request.getPhoneNumber()));
    }
    
    @Override
    @Transactional
    public void refreshUserStatus(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // 重新计算关联乘客数量
            long count = userPassengerRelationRepository.countByUserId(userId);
            user.setRelatedPassenger((int) count);
            userRepository.save(user);
        }
    }

    @Override
    @Transactional
    public BookingResponse deletePassenger(DeletePassengerRequest request) {
        try {
            // 1. 验证用户是否存在
            Optional<User> userOpt = userRepository.findById(request.getUserId());
            if (userOpt.isEmpty()) {
                return BookingResponse.failure("用户不存在");
            }
            
            // 2. 验证乘客是否存在
            Optional<Passenger> passengerOpt = passengerRepository.findById(request.getPassengerId());
            if (passengerOpt.isEmpty()) {
                return BookingResponse.failure("乘客不存在");
            }
            
            // 3. 验证用户是否有权限删除该乘客
            UserPassengerRelation relation = userPassengerRelationRepository
                .findByUserIdAndPassengerId(request.getUserId(), request.getPassengerId());
            
            if (relation == null) {
                return BookingResponse.failure("您没有权限删除该乘客");
            }
            
            // 4. 删除用户乘客关系
            userPassengerRelationRepository.delete(relation);
            
            // 5. 更新用户的关联乘客数量
            User user = userOpt.get();
            user.setRelatedPassenger(user.getRelatedPassenger() - 1);
            userRepository.save(user);
            
            return BookingResponse.successWithMessage("乘客删除成功", null, null, null, LocalDateTime.now());
            
        } catch (Exception e) {
            System.err.println("删除乘客失败: " + e.getMessage());
            return BookingResponse.failure("删除乘客失败: " + e.getMessage());
        }
    }
} 