package com.example.techprototype.DTO;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingRequest {
    
    @NotNull(message = "用户ID不能为空")
    private Long userId;
    
    @NotNull(message = "车次ID不能为空")
    private Integer trainId;
    
    @NotNull(message = "出发站ID不能为空")
    private Long departureStopId;
    
    @NotNull(message = "到达站ID不能为空")
    private Long arrivalStopId;
    
    @NotNull(message = "出行日期不能为空")
    private LocalDate travelDate;
    
    @NotNull(message = "车厢类型ID不能为空")
    private Integer carriageTypeId;
    
    @NotNull(message = "乘客信息不能为空")
    private List<PassengerInfo> passengers;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PassengerInfo {
        @NotNull(message = "乘客ID不能为空")
        private Long passengerId;
        
        @NotNull(message = "票种不能为空")
        @Min(value = 1, message = "票种值无效")
        private Byte ticketType; // 1-成人, 2-儿童, 3-学生, 4-残疾, 5-军人
    }
} 