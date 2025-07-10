package com.example.techprototype.Service.Impl;

import com.example.techprototype.DAO.OrderDAO;
import com.example.techprototype.Entity.Order;
import com.example.techprototype.Entity.Ticket;
import com.example.techprototype.Service.OrderQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class OrderQueryServiceImpl implements OrderQueryService {
    @Autowired
    private OrderDAO orderDAO;

    @Override
    public List<Order> getOrdersByUserAndStatus(Long userId, Byte status) {
        return orderDAO.findOrdersByConditions(userId, status, null, null, null);
    }

    @Override
    public List<Ticket> getTicketsByOrderId(Long orderId) {
        return orderDAO.findTicketsByOrderId(orderId);
    }

    @Override
    public List<Map<String, Object>> getOrderWithTickets(Long orderId) {
        return orderDAO.findOrderWithTickets(orderId);
    }

    // 实现多条件查询方法
    @Override
    public List<Order> findOrdersByConditions(Long userId, Byte status, 
                                             String orderNumber, 
                                             LocalDate startDate, 
                                             LocalDate endDate) {
        return orderDAO.findOrdersByConditions(userId, status, orderNumber, startDate, endDate);
    }
}