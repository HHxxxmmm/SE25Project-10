package com.example.techprototype.Enums;

public enum OrderStatus {
    PENDING_PAYMENT(0, "待支付"),
    PAID(1, "已支付"),
    COMPLETED(2, "已完成"),
    CANCELLED(3, "已取消");
    
    private final int code;
    private final String description;
    
    OrderStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public int getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    public static OrderStatus fromCode(int code) {
        for (OrderStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid order status code: " + code);
    }
} 