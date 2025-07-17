package com.example.techprototype.DTO;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

/**
 * 数字票证数据对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DigitalTicketData {
    
    /**
     * 票证ID
     */
    private Long ticketId;
    
    /**
     * 票证编号
     */
    private String ticketNumber;
    
    /**
     * 乘客ID
     */
    private Long passengerId;
    
    /**
     * 乘客姓名
     */
    private String passengerName;
    
    /**
     * 乘车日期
     */
    private String travelDate;
    
    /**
     * 列车号
     */
    private String trainNumber;
    
    /**
     * 时间戳（用于防止重放攻击）
     */
    private String timestamp;
    
    /**
     * 签名数据
     */
    private String signature;
    
    /**
     * 转换为二维码数据字符串
     * 格式: ticketId|ticketNumber|passengerId|passengerName|travelDate|trainNumber|timestamp|signature
     */
    public String toQRString() {
        System.out.println("正在构建票证二维码数据字符串 - 票证ID: " + ticketId + ", 票号: " + ticketNumber);
        
        String qrString = String.join("|", 
            String.valueOf(ticketId),
            ticketNumber,
            String.valueOf(passengerId),
            passengerName,
            travelDate,
            trainNumber,
            timestamp,
            signature
        );
        
        System.out.println("二维码字符串构建完成 - 长度: " + qrString.length() + 
                         ", 签名长度: " + (signature != null ? signature.length() : 0));
        return qrString;
    }
    
    /**
     * 从二维码数据字符串解析
     */
    public static DigitalTicketData fromQRString(String qrData) {
        System.out.println("正在解析票证二维码数据 - 输入长度: " + qrData.length());
        
        String[] parts = qrData.split("\\|");
        System.out.println("分解得到的部分数量: " + parts.length);
        
        if (parts.length != 8) {
            System.err.println("无效的二维码数据格式: 预期8个部分，但得到 " + parts.length);
            throw new IllegalArgumentException("无效的二维码数据格式");
        }
        
        try {
            DigitalTicketData data = DigitalTicketData.builder()
                .ticketId(Long.parseLong(parts[0]))
                .ticketNumber(parts[1])
                .passengerId(Long.parseLong(parts[2]))
                .passengerName(parts[3])
                .travelDate(parts[4])
                .trainNumber(parts[5])
                .timestamp(parts[6])
                .signature(parts[7])
                .build();
                
            System.out.println("二维码数据解析成功 - 票证ID: " + data.getTicketId() + 
                             ", 票号: " + data.getTicketNumber() + 
                             ", 签名长度: " + data.getSignature().length());
            return data;
        } catch (NumberFormatException e) {
            System.err.println("解析票证数据时出错 - 无法将ID转换为数字: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.err.println("解析票证数据时出错: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * 获取待签名的数据字符串
     * 注意：不包含签名字段本身
     */
    public String getDataToSign() {
        System.out.println("构建待签名数据 - 票证ID: " + ticketId + ", 票号: " + ticketNumber);
        
        String dataToSign = String.join("|", 
            String.valueOf(ticketId),
            ticketNumber,
            String.valueOf(passengerId),
            passengerName,
            travelDate,
            trainNumber,
            timestamp
        );
        
        System.out.println("待签名数据构建完成 - 长度: " + dataToSign.length());
        return dataToSign;
    }
}
