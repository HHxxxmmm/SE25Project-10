package com.example.techprototype.Controller;

import com.example.techprototype.DTO.*;
import com.example.techprototype.Service.TicketService;
import com.example.techprototype.Service.DigitalTicketService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ticket")
@CrossOrigin(origins = "*")
public class TicketController {
    
    @Autowired
    private TicketService ticketService;
    
    @Autowired
    private DigitalTicketService digitalTicketService;
    
    // 数字票证服务
    
    /**
     * 购票
     */
    @PostMapping("/book")
    public ResponseEntity<BookingResponse> bookTickets(@Valid @RequestBody BookingRequest request) {
        BookingResponse response = ticketService.bookTickets(request);
        if ("SUCCESS".equals(response.getStatus())) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 退票
     */
    @PostMapping("/refund")
    public ResponseEntity<BookingResponse> refundTickets(@Valid @RequestBody RefundRequest request) {
        BookingResponse response = ticketService.refundTickets(request);
        if ("SUCCESS".equals(response.getStatus())) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 改签
     */
    @PostMapping("/change")
    public ResponseEntity<BookingResponse> changeTickets(@Valid @RequestBody ChangeTicketRequest request) {
        BookingResponse response = ticketService.changeTickets(request);
        if ("SUCCESS".equals(response.getStatus())) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 支付订单
     */
    
    /**
     * 获取本人车票
     */
    @GetMapping("/my-tickets")
    public ResponseEntity<MyTicketResponse> getMyTickets(@RequestParam Long userId) {
        MyTicketResponse response = ticketService.getMyTickets(userId);
        if ("SUCCESS".equals(response.getStatus())) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 根据状态获取本人车票
     */
    @GetMapping("/my-tickets/status")
    public ResponseEntity<MyTicketResponse> getMyTicketsByStatus(
            @RequestParam Long userId, 
            @RequestParam Byte ticketStatus) {
        MyTicketResponse response = ticketService.getMyTicketsByStatus(userId, ticketStatus);
        if ("SUCCESS".equals(response.getStatus())) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 根据出发日期范围获取本人车票
     */
    @GetMapping("/my-tickets/date-range")
    public ResponseEntity<MyTicketResponse> getMyTicketsByDateRange(
            @RequestParam Long userId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        try {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);
            MyTicketResponse response = ticketService.getMyTicketsByDateRange(userId, start, end);
            if ("SUCCESS".equals(response.getStatus())) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(MyTicketResponse.failure("日期格式错误，请使用yyyy-MM-dd格式"));
        }
    }
    
    /**
     * 根据车票状态和出发日期范围获取本人车票
     */
    @GetMapping("/my-tickets/status-date-range")
    public ResponseEntity<MyTicketResponse> getMyTicketsByStatusAndDateRange(
            @RequestParam Long userId,
            @RequestParam Byte ticketStatus,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        try {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);
            MyTicketResponse response = ticketService.getMyTicketsByStatusAndDateRange(userId, ticketStatus, start, end);
            if ("SUCCESS".equals(response.getStatus())) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(MyTicketResponse.failure("日期格式错误，请使用yyyy-MM-dd格式"));
        }
    }
    
    /**
     * 获取车票详情（带权限验证）
     */
    @GetMapping("/detail")
    public ResponseEntity<TicketDetailResponse> getTicketDetail(
            @RequestParam Long ticketId,
            @RequestParam Long userId) {
        TicketDetailResponse response = ticketService.getTicketDetail(ticketId, userId);
        if ("SUCCESS".equals(response.getStatus())) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取电子票证数据（二维码数据和验证公钥）
     */
    @GetMapping("/digital")
    public ResponseEntity<DigitalTicketResponse> getDigitalTicket(
            @RequestParam Long ticketId,
            @RequestParam Long userId) {
        
        System.out.println("接收到获取数字票证请求 - 票证ID: " + ticketId + ", 用户ID: " + userId);
        
        // 获取车票二维码数据
        System.out.println("开始获取车票二维码数据...");
        String qrCodeData = ticketService.getTicketQRCodeData(ticketId, userId);
        
        if (qrCodeData == null) {
            System.err.println("获取车票二维码数据失败 - 票证ID: " + ticketId + ", 用户ID: " + userId);
            return ResponseEntity.badRequest()
                .body(DigitalTicketResponse.failure("获取数字票证失败，请确认权限或车票状态"));
        }
        System.out.println("获取车票二维码数据成功 - 数据长度: " + qrCodeData.length());
        
        // 获取验证用的公钥
        System.out.println("获取验证公钥...");
        String publicKey = digitalTicketService.getPublicKeyBase64();
        System.out.println("获取公钥成功 - 公钥长度: " + publicKey.length());
        
        try {
            // 解析二维码数据，获取票证基本信息
            System.out.println("解析二维码数据...");
            DigitalTicketData ticketData = DigitalTicketData.fromQRString(qrCodeData);
            System.out.println("二维码数据解析成功 - 票证ID: " + ticketData.getTicketId() + 
                             ", 票证号: " + ticketData.getTicketNumber() + 
                             ", 乘客: " + ticketData.getPassengerName());
            
            // 构建响应
            System.out.println("构建响应数据...");
            DigitalTicketResponse.DigitalTicketInfo ticketInfo = new DigitalTicketResponse.DigitalTicketInfo(
                qrCodeData,
                publicKey,
                ticketData.getTicketId(),
                ticketData.getTicketNumber(),
                ticketData.getPassengerName(),
                ticketData.getTrainNumber(),
                "", // 这里可以补充车站名，暂时留空
                "",
                ticketData.getTravelDate()
            );
            
            System.out.println("数字票证数据处理完成，准备返回 - 票证ID: " + ticketId);
            return ResponseEntity.ok(DigitalTicketResponse.success(ticketInfo));
        } catch (Exception e) {
            System.err.println("处理数字票证数据时出错: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest()
                .body(DigitalTicketResponse.failure("处理数字票证数据失败：" + e.getMessage()));
        }
    }
    
    /**
     * 验证数字票证签名
     */
    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyTicket(
            @RequestBody Map<String, String> request) {
        
        System.out.println("接收到票证验证请求");
        
        String qrData = request.get("qrData");
        if (qrData == null || qrData.isEmpty()) {
            System.err.println("验证失败：请求中缺少qrData参数");
            Map<String, Object> response = new HashMap<>();
            response.put("status", "FAILURE");
            response.put("message", "缺少二维码数据");
            response.put("valid", false);
            response.put("timestamp", LocalDateTime.now());
            return ResponseEntity.badRequest().body(response);
        }
        System.out.println("收到的二维码数据长度: " + qrData.length());
        
        System.out.println("开始验证票证签名...");
        boolean isValid = digitalTicketService.verifyTicketSignature(qrData);
        System.out.println("验证结果: " + (isValid ? "有效" : "无效"));
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("valid", isValid);
        response.put("message", isValid ? "验证成功" : "验证失败，可能是伪造票证");
        response.put("timestamp", LocalDateTime.now());
        
        if (isValid) {
            try {
                // 获取并返回票证基本信息用于显示
                System.out.println("解析票证基本信息...");
                DigitalTicketData ticketData = DigitalTicketData.fromQRString(qrData);
                System.out.println("票证信息解析成功 - 票证ID: " + ticketData.getTicketId() + 
                                 ", 票证号: " + ticketData.getTicketNumber());
                
                response.put("ticketId", ticketData.getTicketId());
                response.put("ticketNumber", ticketData.getTicketNumber());
                response.put("passengerName", ticketData.getPassengerName());
                response.put("trainNumber", ticketData.getTrainNumber());
                response.put("travelDate", ticketData.getTravelDate());
            } catch (Exception e) {
                // 解析失败则不返回详细信息
                System.err.println("解析票证信息失败: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        System.out.println("验证请求处理完成，准备返回响应");
        return ResponseEntity.ok(response);
    }
} 