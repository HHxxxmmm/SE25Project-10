package com.example.techprototype.Repository;

import com.example.techprototype.Entity.CarriageType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
 
@Repository
public interface CarriageTypeRepository extends JpaRepository<CarriageType, Integer> {
} 