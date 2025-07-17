package com.example.techprototype.Service;

import com.example.techprototype.DTO.BookingRequest;
import com.example.techprototype.DTO.BookingResponse;
import com.example.techprototype.DTO.RefundRequest;
import com.example.techprototype.DTO.ChangeTicketRequest;
import com.example.techprototype.DTO.MyTicketResponse;
import com.example.techprototype.DTO.TicketDetailResponse;
import java.time.LocalDate;

public interface TicketService {
    
    /**
     * 购票
     */
    BookingResponse bookTickets(BookingRequest request);
    
    /**
     * 退票
     */
    BookingResponse refundTickets(RefundRequest request);
    
    /**
     * 改签
     */
    BookingResponse changeTickets(ChangeTicketRequest request);
    
    /**
     * 获取本人车票
     */
    MyTicketResponse getMyTickets(Long userId);
    
    /**
     * 根据车票状态获取本人车票
     */
    MyTicketResponse getMyTicketsByStatus(Long userId, Byte ticketStatus);
    
    /**
     * 根据出发日期范围获取本人车票
     */
    MyTicketResponse getMyTicketsByDateRange(Long userId, LocalDate startDate, LocalDate endDate);
    
    /**
     * 根据车票状态和出发日期范围获取本人车票
     */
    MyTicketResponse getMyTicketsByStatusAndDateRange(Long userId, Byte ticketStatus, LocalDate startDate, LocalDate endDate);
    
    /**
     * 获取车票详情（带权限验证）
     */
    TicketDetailResponse getTicketDetail(Long ticketId, Long userId);

} 