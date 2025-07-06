package com.example.techprototype.Controller;

import com.example.techprototype.DTO.*;
import com.example.techprototype.Service.TicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

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

} 