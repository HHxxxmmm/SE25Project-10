package com.example.techprototype.DTO;

/**
 * 登录请求DTO
 */
public class LoginDTO {
    
    private String phoneNumber;
    private String password;
    
    // Getters and setters
    public String getPhoneNumber() {
        return phoneNumber;
    }
    
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
}
