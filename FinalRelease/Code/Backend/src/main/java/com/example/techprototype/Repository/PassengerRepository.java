package com.example.techprototype.Repository;

import com.example.techprototype.Entity.Passenger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
 
@Repository
public interface PassengerRepository extends JpaRepository<Passenger, Long> {
    
    /**
     * 根据身份证号查找乘客
     */
    Optional<Passenger> findByIdCardNumber(String idCardNumber);
} 