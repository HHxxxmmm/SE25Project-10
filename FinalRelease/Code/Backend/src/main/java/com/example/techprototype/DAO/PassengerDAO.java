package com.example.techprototype.DAO;

import com.example.techprototype.Entity.Passenger;
import java.util.Optional;

public interface PassengerDAO {
    
    /**
     * 根据身份证号查找乘客
     * @param idCardNumber 身份证号
     * @return 乘客对象，可能为空
     */
    Optional<Passenger> findByIdCardNumber(String idCardNumber);
    
    /**
     * 保存乘客信息
     * @param passenger 乘客对象
     * @return 保存后的乘客对象
     */
    Passenger save(Passenger passenger);
    
    /**
     * 根据乘客ID查找乘客
     * @param passengerId 乘客ID
     * @return 乘客对象，可能为空
     */
    Optional<Passenger> findById(Long passengerId);
}
