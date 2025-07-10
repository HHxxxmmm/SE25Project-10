package com.example.techprototype.DTO;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrepareOrderResponse {
    
    /**
     * 车次信息
     */
    private TrainInfo trainInfo;
    
    /**
     * 席别信息列表
     */
    private List<CarriageInfo> carriages;
    
    /**
     * 用户关联的乘客列表
     */
    private List<PassengerInfo> passengers;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrainInfo {
        private String trainNumber;        // 车次号
        private String departureStation;   // 出发站
        private String arrivalStation;     // 到达站
        private LocalTime departureTime;   // 出发时间
        private LocalTime arrivalTime;     // 到达时间
        private LocalDate travelDate;      // 出发日期
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CarriageInfo {
        private Long inventoryId;          // 库存ID
        private String carriageTypeName;   // 席别名称
        private BigDecimal price;          // 价格
        private Boolean hasStock;          // 是否有余票
        private Integer availableStock;    // 可用库存数量
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PassengerInfo {
        private Long passengerId;          // 乘客ID
        private String realName;           // 姓名
        private String idCardNumber;       // 身份证号
        private String phoneNumber;        // 手机号
        private Byte passengerType;        // 乘客类型
        private String passengerTypeName;  // 乘客类型名称
        private Byte relationType;         // 关系类型
        private String relationTypeName;   // 关系类型名称
    }
} 