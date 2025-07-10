package com.example.techprototype.Entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tickets")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Ticket {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ticket_id")
    private Long ticketId;
    
    @Column(name = "ticket_number", nullable = false, length = 32, unique = true)
    private String ticketNumber;
    
    @Column(name = "order_id", nullable = false)
    private Long orderId;
    
    @Column(name = "passenger_id", nullable = false)
    private Long passengerId;
    
    @Column(name = "train_id", nullable = false)
    private Integer trainId;
    
    @Column(name = "departure_stop_id", nullable = false)
    private Long departureStopId;
    
    @Column(name = "arrival_stop_id", nullable = false)
    private Long arrivalStopId;
    
    @Column(name = "travel_date", nullable = false)
    private LocalDate travelDate;
    
    @Column(name = "running_days", nullable = false)
    private Integer runningDays = 1;
    
    @Column(name = "carriage_type_id", nullable = false)
    private Integer carriageTypeId;
    
    @Column(name = "carriage_number", length = 10)
    private String carriageNumber;
    
    @Column(name = "seat_number", length = 10)
    private String seatNumber;
    
    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
    
    @Column(name = "ticket_status", nullable = false)
    private Byte ticketStatus = 0; // 0-待支付, 1-未使用, 2-已使用, 3-已退票, 4-已改签
    
    @Column(name = "digital_signature", length = 255)
    private String digitalSignature;
    
    @Column(name = "created_time", nullable = false)
    private LocalDateTime createdTime;
    
    @Column(name = "ticket_type", nullable = false)
    private Byte ticketType = 1; // 1-成人, 2-儿童, 3-学生, 4-残疾, 5-军人
} 