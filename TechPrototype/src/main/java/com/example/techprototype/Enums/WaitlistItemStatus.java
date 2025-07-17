package com.example.techprototype.Enums;

public enum WaitlistItemStatus {
    PENDING_PAYMENT(0, "待支付"),
    PENDING_FULFILLMENT(1, "待兑现"),
    FULFILLED(2, "已兑现"),
    CANCELLED(3, "已取消");
    
    private final int code;
    private final String description;
    
    WaitlistItemStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public int getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    public static WaitlistItemStatus fromCode(int code) {
        for (WaitlistItemStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid waitlist item status code: " + code);
    }
} 
 