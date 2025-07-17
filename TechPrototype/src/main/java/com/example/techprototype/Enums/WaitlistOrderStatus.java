package com.example.techprototype.Enums;

public enum WaitlistOrderStatus {
    PENDING_PAYMENT(0, "待支付"),
    PENDING_FULFILLMENT(1, "待兑现"),
    FULFILLED(2, "已兑现"),
    CANCELLED(3, "已取消");
    
    private final int code;
    private final String description;
    
    WaitlistOrderStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public int getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    public static WaitlistOrderStatus fromCode(int code) {
        for (WaitlistOrderStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid waitlist order status code: " + code);
    }
} 
 