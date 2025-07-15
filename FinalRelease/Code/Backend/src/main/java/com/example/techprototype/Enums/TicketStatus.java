package com.example.techprototype.Enums;

public enum TicketStatus {
    PENDING(0, "待支付"),
    UNUSED(1, "未使用"),
    USED(2, "已使用"),
    REFUNDED(3, "已退票"),
    CHANGED(4, "已改签");
    
    private final int code;
    private final String description;
    
    TicketStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public int getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    public static TicketStatus fromCode(int code) {
        for (TicketStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid ticket status code: " + code);
    }
} 