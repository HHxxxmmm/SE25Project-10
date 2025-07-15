package com.example.techprototype.Enums;

public enum PassengerType {
    ADULT(1, "成人"),
    CHILD(2, "儿童"),
    STUDENT(3, "学生"),
    DISABLED(4, "残疾"),
    SOLDIER(5, "军人");
    
    private final int code;
    private final String description;
    
    PassengerType(int code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public int getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    public static PassengerType fromCode(int code) {
        for (PassengerType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid passenger type code: " + code);
    }
} 