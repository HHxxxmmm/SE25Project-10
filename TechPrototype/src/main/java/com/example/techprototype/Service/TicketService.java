package com.example.techprototype.Service;

import com.example.techprototype.DTO.BookingRequest;
import com.example.techprototype.DTO.BookingResponse;
import com.example.techprototype.DTO.RefundRequest;
import com.example.techprototype.DTO.ChangeTicketRequest;

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
    

} 