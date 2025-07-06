package com.example.techprototype.Entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;
    
    @Column(name = "real_name", nullable = false, length = 50)
    private String realName;
    
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;
    
    @Column(name = "email", length = 100, unique = true)
    private String email;
    
    @Column(name = "phone_number", nullable = false, length = 20, unique = true)
    private String phoneNumber;
    
    @Column(name = "id_card_number", nullable = false, length = 18, unique = true)
    private String idCardNumber;
    
    @Column(name = "registration_time", nullable = false)
    private LocalDateTime registrationTime;
    
    @Column(name = "last_login_time")
    private LocalDateTime lastLoginTime;
    
    @Column(name = "account_status", nullable = false)
    private Byte accountStatus = 1; // 1-正常, 0-冻结
} 