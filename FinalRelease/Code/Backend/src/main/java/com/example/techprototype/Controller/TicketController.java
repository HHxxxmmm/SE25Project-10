package com.example.techprototype.Controller;

import com.example.techprototype.DTO.*;
import com.example.techprototype.Service.TicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/ticket")
@CrossOrigin(origins = "*")
public class TicketController {
    
    @Autowired
    private TicketService ticketService;
    
    /**
     * 购票
     */
    @PostMapping("/book")
    public ResponseEntity<BookingResponse> bookTickets(@Valid @RequestBody BookingRequest request) {
        BookingResponse response = ticketService.bookTickets(request);
        if ("SUCCESS".equals(response.getStatus())) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 退票
     */
    @PostMapping("/refund")
    public ResponseEntity<BookingResponse> refundTickets(@Valid @RequestBody RefundRequest request) {
        BookingResponse response = ticketService.refundTickets(request);
        if ("SUCCESS".equals(response.getStatus())) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 改签
     */
    @PostMapping("/change")
    public ResponseEntity<BookingResponse> changeTickets(@Valid @RequestBody ChangeTicketRequest request) {
        BookingResponse response = ticketService.changeTickets(request);
        if ("SUCCESS".equals(response.getStatus())) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 支付订单
     */
    
    /**
     * 获取本人车票
     */
    @GetMapping("/my-tickets")
    public ResponseEntity<MyTicketResponse> getMyTickets(@RequestParam Long userId) {
        MyTicketResponse response = ticketService.getMyTickets(userId);
        if ("SUCCESS".equals(response.getStatus())) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 根据状态获取本人车票
     */
    @GetMapping("/my-tickets/status")
    public ResponseEntity<MyTicketResponse> getMyTicketsByStatus(
            @RequestParam Long userId, 
            @RequestParam Byte ticketStatus) {
        MyTicketResponse response = ticketService.getMyTicketsByStatus(userId, ticketStatus);
        if ("SUCCESS".equals(response.getStatus())) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 根据出发日期范围获取本人车票
     */
    @GetMapping("/my-tickets/date-range")
    public ResponseEntity<MyTicketResponse> getMyTicketsByDateRange(
            @RequestParam Long userId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        try {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);
            MyTicketResponse response = ticketService.getMyTicketsByDateRange(userId, start, end);
            if ("SUCCESS".equals(response.getStatus())) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(MyTicketResponse.failure("日期格式错误，请使用yyyy-MM-dd格式"));
        }
    }
    
    /**
     * 根据车票状态和出发日期范围获取本人车票
     */
    @GetMapping("/my-tickets/status-date-range")
    public ResponseEntity<MyTicketResponse> getMyTicketsByStatusAndDateRange(
            @RequestParam Long userId,
            @RequestParam Byte ticketStatus,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        try {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);
            MyTicketResponse response = ticketService.getMyTicketsByStatusAndDateRange(userId, ticketStatus, start, end);
            if ("SUCCESS".equals(response.getStatus())) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(MyTicketResponse.failure("日期格式错误，请使用yyyy-MM-dd格式"));
        }
    }
    
    /**
     * 获取车票详情（带权限验证）
     */
    @GetMapping("/detail")
    public ResponseEntity<TicketDetailResponse> getTicketDetail(
            @RequestParam Long ticketId,
            @RequestParam Long userId) {
        TicketDetailResponse response = ticketService.getTicketDetail(ticketId, userId);
        if ("SUCCESS".equals(response.getStatus())) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
} 