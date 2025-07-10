package com.example.techprototype.DTO;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefundPreparationRequest {
    private Long userId;
    private Long orderId;
    private List<Long> ticketIds;
} 