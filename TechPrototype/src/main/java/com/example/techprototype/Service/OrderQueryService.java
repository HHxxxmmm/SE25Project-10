package com.example.techprototype.Service;

import com.example.techprototype.Entity.Order;
import com.example.techprototype.Entity.Ticket;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface OrderQueryService {
    List<Order> getOrdersByUserAndStatus(Long userId, Byte status);
    List<Ticket> getTicketsByOrderId(Long orderId);
    List<Map<String, Object>> getOrderWithTickets(Long orderId);  // 修改返回类型

    // 新增多条件查询方法
    List<Order> findOrdersByConditions(Long userId, Byte status, String orderNumber,
                                       LocalDate startDate, LocalDate endDate);
}
