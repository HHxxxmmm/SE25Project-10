package com.example.techprototype.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LastLoginUpdateRequest {
    private Long userId;
    private LocalDateTime loginTime;
}