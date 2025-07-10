package com.example.techprototype.Entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long orderId;
    
    @Column(name = "order_number", nullable = false, length = 32, unique = true)
    private String orderNumber;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "order_time", nullable = false)
    private LocalDateTime orderTime;
    
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;
    
    @Column(name = "payment_time")
    private LocalDateTime paymentTime;
    
    @Column(name = "payment_method", length = 20)
    private String paymentMethod;
    
    @Column(name = "order_status", nullable = false)
    private Byte orderStatus = 0; // 0-待支付, 1-已支付, 2-已完成, 3-已取消
    
    @Column(name = "ticket_count", nullable = false)
    private Integer ticketCount = 0; // 订单包含的票数
} 