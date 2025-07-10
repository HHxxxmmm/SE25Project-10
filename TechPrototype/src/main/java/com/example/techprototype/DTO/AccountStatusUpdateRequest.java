package com.example.techprototype.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountStatusUpdateRequest {
    private Long userId;
    private Byte accountStatus; // 1-正常, 0-冻结
} 