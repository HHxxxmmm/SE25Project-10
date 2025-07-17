package com.example.techprototype.Service.Impl;

import com.example.techprototype.Entity.Order;
import com.example.techprototype.Entity.Ticket;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class OrderQueryServiceImplTest {

    @Autowired
    private OrderQueryServiceImpl orderQueryService;

    @Test
    void getOrdersByUserAndStatus() {
        // 假设数据库有userId=1, status=1的订单
        List<Order> orders = orderQueryService.getOrdersByUserAndStatus(1L, (byte) 1);
        assertNotNull(orders);
    }

    @Test
    void getTicketsByOrderId() {
        // 假设数据库有orderId=1的车票
        List<Ticket> tickets = orderQueryService.getTicketsByOrderId(1L);
        assertNotNull(tickets);
    }

    @Test
    void getOrderWithTickets() {
        // 假设数据库有orderId=1的订单及车票
        List<Map<String, Object>> result = orderQueryService.getOrderWithTickets(1L);
        assertNotNull(result);
    }

    @Test
    void findOrdersByConditions() {
        // 假设数据库有userId=1, status=1, orderNumber=null, 日期范围为null的订单
        List<Order> orders = orderQueryService.findOrdersByConditions(1L, (byte) 1, null, null, null);
        assertNotNull(orders);
    }
}