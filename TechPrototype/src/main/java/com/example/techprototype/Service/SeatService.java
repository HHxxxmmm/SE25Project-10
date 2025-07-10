package com.example.techprototype.Service;

import com.example.techprototype.Entity.Seat;
import com.example.techprototype.Entity.Ticket;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SeatService {
    
    /**
     * 释放座位
     */
    void releaseSeat(Ticket ticket);
    
    /**
     * 分配座位
     */
    void assignSeat(Ticket ticket);
    
    /**
     * 查找可用座位（基于位图）
     * @param trainId 车次ID
     * @param typeId 车厢类型ID
     * @param travelDate 出发日期
     * @param departureStopId 出发站ID
     * @param arrivalStopId 到达站ID
     * @return 可用的座位
     */
    Optional<Seat> findAvailableSeat(Integer trainId, Integer typeId, LocalDate travelDate, 
                                   Long departureStopId, Long arrivalStopId);
    
    /**
     * 锁定座位（设置区间占用）
     * @param seat 座位对象
     * @param travelDate 出发日期
     * @param departureStopId 出发站ID
     * @param arrivalStopId 到达站ID
     */
    void lockSeat(Seat seat, LocalDate travelDate, Long departureStopId, Long arrivalStopId);
    
    /**
     * 释放座位（清除区间占用）
     * @param seat 座位对象
     * @param travelDate 出发日期
     * @param departureStopId 出发站ID
     * @param arrivalStopId 到达站ID
     */
    void releaseSeat(Seat seat, LocalDate travelDate, Long departureStopId, Long arrivalStopId);
    
    /**
     * 检查座位是否可用
     * @param seat 座位对象
     * @param travelDate 出发日期
     * @param departureStopId 出发站ID
     * @param arrivalStopId 到达站ID
     * @return true表示可用
     */
    boolean isSeatAvailable(Seat seat, LocalDate travelDate, Long departureStopId, Long arrivalStopId);
    
    /**
     * 根据sequence_number查找可用座位（基于位图）
     * @param trainId 车次ID
     * @param typeId 车厢类型ID
     * @param travelDate 出发日期
     * @param departureSequence 出发站序号
     * @param arrivalSequence 到达站序号
     * @return 可用的座位
     */
    Optional<Seat> findAvailableSeatBySequence(Integer trainId, Integer typeId, LocalDate travelDate, 
                                             int departureSequence, int arrivalSequence);
    
    /**
     * 根据sequence_number锁定座位（设置区间占用）
     * @param seat 座位对象
     * @param travelDate 出发日期
     * @param departureSequence 出发站序号
     * @param arrivalSequence 到达站序号
     */
    void lockSeatBySequence(Seat seat, LocalDate travelDate, int departureSequence, int arrivalSequence);
    
    /**
     * 根据sequence_number释放座位（清除区间占用）
     * @param seat 座位对象
     * @param travelDate 出发日期
     * @param departureSequence 出发站序号
     * @param arrivalSequence 到达站序号
     */
    void releaseSeatBySequence(Seat seat, LocalDate travelDate, int departureSequence, int arrivalSequence);
    
    /**
     * 根据sequence_number检查座位是否可用
     * @param seat 座位对象
     * @param travelDate 出发日期
     * @param departureSequence 出发站序号
     * @param arrivalSequence 到达站序号
     * @return true表示可用
     */
    boolean isSeatAvailableBySequence(Seat seat, LocalDate travelDate, int departureSequence, int arrivalSequence);
} 