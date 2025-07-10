package com.example.techprototype.DTO;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrepareOrderRequest {
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 库存ID列表（最多3个，对应不同席别）
     */
    private List<Long> inventoryIds;
} 