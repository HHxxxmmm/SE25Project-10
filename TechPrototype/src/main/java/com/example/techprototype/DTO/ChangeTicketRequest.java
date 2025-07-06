package com.example.techprototype.DTO;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangeTicketRequest {
    
    @NotNull(message = "用户ID不能为空")
    private Long userId;
    
    @NotNull(message = "原订单号不能为空")
    private String originalOrderNumber;
    
    @NotNull(message = "改签车票ID列表不能为空")
    private List<Long> ticketIds;
    
    @NotNull(message = "新车次ID不能为空")
    private Integer newTrainId;
    
    @NotNull(message = "新出发站ID不能为空")
    private Long newDepartureStopId;
    
    @NotNull(message = "新到达站ID不能为空")
    private Long newArrivalStopId;
    
    @NotNull(message = "新出行日期不能为空")
    private LocalDate newTravelDate;
    
    @NotNull(message = "新车厢类型ID不能为空")
    private Integer newCarriageTypeId;
} 