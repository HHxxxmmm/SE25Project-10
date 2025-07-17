package com.example.techprototype.DAO;

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
class OrderDAOTest {
    @Autowired
    private OrderDAO orderDAO;

    @Test
    void findOrdersByConditions() {
        // 假设数据库有userId=1的订单
        List<Order> orders = orderDAO.findOrdersByConditions(1L, null, null, null, null);
        assertNotNull(orders);
        // 可根据实际数据进一步断言
    }

    @Test
    void findTicketsByOrderId() {
        // 假设数据库有orderId=1的车票
        List<Ticket> tickets = orderDAO.findTicketsByOrderId(1L);
        assertNotNull(tickets);
    }

    @Test
    void findOrderWithTickets() {
        // 假设数据库有orderId=1的订单及车票
        List<Map<String, Object>> result = orderDAO.findOrderWithTickets(1L);
        assertNotNull(result);
    }
}