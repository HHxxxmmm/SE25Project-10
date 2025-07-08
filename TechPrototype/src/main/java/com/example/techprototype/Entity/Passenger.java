package com.example.techprototype.Entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "passengers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Passenger {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "passenger_id")
    private Long passengerId;
    
    @Column(name = "id_card_number", nullable = false, length = 20, unique = true)
    private String idCardNumber;
    
    @Column(name = "real_name", nullable = false, length = 50)
    private String realName;
    
    @Column(name = "passenger_type", nullable = false)
    private Byte passengerType; // 1-成人, 2-儿童, 3-学生, 4-残疾军人
    
    @Column(name = "phone_number", length = 20)
    private String phoneNumber;
    
    @Column(name = "student_type_left")
    private Integer studentTypeLeft = 0; // 学生剩余次数，仅当passenger_type=3(学生)时设为4，其他设为0
} 