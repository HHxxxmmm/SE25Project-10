package com.example.techprototype.DAO;

import com.example.techprototype.Entity.Order;
import com.example.techprototype.Entity.Ticket;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.time.LocalDate;
import java.util.Map;

@Mapper
public interface OrderDAO {
    // 按用户和状态等筛选订单
    @Select("<script>" +
            "SELECT * FROM orders WHERE user_id = #{userId} " +
            "<if test='status != null'>AND order_status = #{status}</if> " +
            "<if test='orderNumber != null'>AND order_number LIKE CONCAT('%',#{orderNumber},'%')</if> " +
            "<if test='startDate != null'>AND order_time &gt;= #{startDate}</if> " +
            "<if test='endDate != null'>AND order_time &lt;= #{endDate}</if> " +
            "</script>")
    List<Order> findOrdersByConditions(@Param("userId") Long userId,
                                       @Param("status") Byte status,
                                       @Param("orderNumber") String orderNumber,
                                       @Param("startDate") LocalDate startDate,
                                       @Param("endDate") LocalDate endDate);

    // 查询订单下所有车票详情（联表）
    @Select("SELECT t.* FROM tickets t WHERE t.order_id = #{orderId}")
    List<Ticket> findTicketsByOrderId(@Param("orderId") Long orderId);

    // 订单+车票详情联表（示例：返回订单和车票所有字段）
    @Select("SELECT o.*, t.* FROM orders o JOIN tickets t ON o.order_id = t.order_id WHERE o.order_id = #{orderId}")
    List<Map<String, Object>> findOrderWithTickets(@Param("orderId") Long orderId);
}

