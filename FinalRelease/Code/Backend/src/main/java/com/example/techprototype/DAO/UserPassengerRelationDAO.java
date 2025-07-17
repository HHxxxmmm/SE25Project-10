package com.example.techprototype.DAO;

import com.example.techprototype.Entity.UserPassengerRelation;
import java.util.List;
import java.util.Optional;

public interface UserPassengerRelationDAO {
    
    /**
     * 保存用户与乘客的关系
     * @param relation 关系对象
     * @return 保存后的关系对象
     */
    UserPassengerRelation save(UserPassengerRelation relation);
    
    /**
     * 根据用户ID查询所有关联的乘客关系
     * @param userId 用户ID
     * @return 关系列表
     */
    List<UserPassengerRelation> findAllByUserId(Long userId);
    
    /**
     * 根据用户ID和乘客ID查询关系
     * @param userId 用户ID
     * @param passengerId 乘客ID
     * @return 关系对象，可能为空
     */
    Optional<UserPassengerRelation> findByUserIdAndPassengerId(Long userId, Long passengerId);
}
