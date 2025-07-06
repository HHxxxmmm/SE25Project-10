package com.example.techprototype.Service;

import com.example.techprototype.Entity.Ticket;

public interface SeatService {
    
    /**
     * 释放座位
     */
    void releaseSeat(Ticket ticket);
    
    /**
     * 分配座位
     */
    void assignSeat(Ticket ticket);
} 