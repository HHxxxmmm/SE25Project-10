package com.example.techprototype.DTO;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefundRequest {
    
    @NotNull(message = "用户ID不能为空")
    private Long userId;
    
    @NotNull(message = "订单号不能为空")
    private String orderNumber;
    
    @NotNull(message = "退票车票ID列表不能为空")
    private List<Long> ticketIds;
    
    private String refundReason;
} 