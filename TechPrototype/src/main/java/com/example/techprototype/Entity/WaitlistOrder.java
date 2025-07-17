package com.example.techprototype.Entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "waitlist_orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WaitlistOrder {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "waitlist_id")
    private Long waitlistId;
    
    @Column(name = "order_number", nullable = false, length = 32, unique = true)
    private String orderNumber;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "order_time", nullable = false)
    private LocalDateTime orderTime;
    
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;
    
    @Column(name = "expire_time", nullable = false)
    private LocalDateTime expireTime;
    
    @Column(name = "order_status", nullable = false)
    private Byte orderStatus = 0; // 0-待支付, 1-待兑现, 2-已兑现, 3-已取消
    
    @Column(name = "item_count")
    private Integer itemCount;
} 
 

