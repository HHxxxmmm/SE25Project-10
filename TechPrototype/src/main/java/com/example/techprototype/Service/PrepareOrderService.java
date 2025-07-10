package com.example.techprototype.Service;

import com.example.techprototype.DTO.PrepareOrderRequest;
import com.example.techprototype.DTO.PrepareOrderResponse;

public interface PrepareOrderService {
    
    /**
     * 准备提交订单
     * @param request 请求参数
     * @return 订单准备信息
     */
    PrepareOrderResponse prepareOrder(PrepareOrderRequest request);
} 