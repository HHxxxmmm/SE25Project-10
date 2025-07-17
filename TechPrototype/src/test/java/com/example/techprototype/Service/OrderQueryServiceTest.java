package com.example.techprototype.Service;

import com.example.techprototype.Entity.Order;
import com.example.techprototype.Entity.Ticket;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class OrderQueryServiceTest {

    @Autowired
    private OrderQueryService orderQueryService;

    @Test
    void getOrdersByUserAndStatus() {
        List<Order> orders = orderQueryService.getOrdersByUserAndStatus(1L, (byte) 1);
        assertNotNull(orders);
    }

    @Test
    void getTicketsByOrderId() {
        List<Ticket> tickets = orderQueryService.getTicketsByOrderId(1L);
        assertNotNull(tickets);
    }

    @Test
    void getOrderWithTickets() {
        List<Map<String, Object>> result = orderQueryService.getOrderWithTickets(1L);
        assertNotNull(result);
    }

    @Test
    void findOrdersByConditions() {
        List<Order> orders = orderQueryService.findOrdersByConditions(1L, (byte) 1, null, null, null);
        assertNotNull(orders);
    }
}