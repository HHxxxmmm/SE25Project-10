package com.example.techprototype.Controller;

import com.example.techprototype.DTO.PrepareOrderRequest;
import com.example.techprototype.DTO.PrepareOrderResponse;
import com.example.techprototype.Service.PrepareOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/prepare-order")
@CrossOrigin(origins = "*")
public class PrepareOrderController {
    
    @Autowired
    private PrepareOrderService prepareOrderService;
    
    /**
     * 准备提交订单
     * @param request 请求参数
     * @return 订单准备信息
     */
    @PostMapping("/prepare")
    public ResponseEntity<PrepareOrderResponse> prepareOrder(@RequestBody PrepareOrderRequest request) {
        try {
            PrepareOrderResponse response = prepareOrderService.prepareOrder(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
} 