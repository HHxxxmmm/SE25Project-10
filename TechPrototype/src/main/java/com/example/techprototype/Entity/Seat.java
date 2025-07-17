package com.example.techprototype.Entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "seats")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Seat {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "seat_id")
    private Long seatId;
    
    @Column(name = "carriage_id", nullable = false)
    private Long carriageId;
    
    @Column(name = "seat_number", nullable = false, length = 10)
    private String seatNumber;
    
    @Column(name = "seat_type", length = 10)
    private String seatType; // 靠窗, 靠过道, 中间等
    
    @Column(name = "date_1", nullable = false)
    private Long date1 = 0L; // 动态日期位图，最后10位表示区间占用
    
    @Column(name = "date_2", nullable = false)
    private Long date2 = 0L; // 动态日期位图，最后10位表示区间占用
    
    @Column(name = "date_3", nullable = false)
    private Long date3 = 0L; // 动态日期位图，最后10位表示区间占用
    
    @Column(name = "date_4", nullable = false)
    private Long date4 = 0L; // 动态日期位图，最后10位表示区间占用
    
    @Column(name = "date_5", nullable = false)
    private Long date5 = 0L; // 动态日期位图，最后10位表示区间占用
    
    @Column(name = "date_6", nullable = false)
    private Long date6 = 0L; // 动态日期位图，最后10位表示区间占用
    
    @Column(name = "date_7", nullable = false)
    private Long date7 = 0L; // 动态日期位图，最后10位表示区间占用
    
    @Column(name = "date_8", nullable = false)
    private Long date8 = 0L; // 动态日期位图，最后10位表示区间占用
    
    @Column(name = "date_9", nullable = false)
    private Long date9 = 0L; // 动态日期位图，最后10位表示区间占用
    
    @Column(name = "date_10", nullable = false)
    private Long date10 = 0L; // 动态日期位图，最后10位表示区间占用
} 