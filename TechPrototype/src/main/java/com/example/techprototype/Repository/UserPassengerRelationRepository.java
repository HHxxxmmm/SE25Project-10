package com.example.techprototype.Repository;

import com.example.techprototype.Entity.UserPassengerRelation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface UserPassengerRelationRepository extends JpaRepository<UserPassengerRelation, Long> {
    
    /**
     * 根据用户ID查找所有乘客关系
     */
    List<UserPassengerRelation> findByUserId(Long userId);
    
    /**
     * 根据用户ID和乘客ID查找关系
     */
    UserPassengerRelation findByUserIdAndPassengerId(Long userId, Long passengerId);
    
    /**
     * 检查用户是否有指定的乘客关系
     */
    boolean existsByUserIdAndPassengerId(Long userId, Long passengerId);
} 