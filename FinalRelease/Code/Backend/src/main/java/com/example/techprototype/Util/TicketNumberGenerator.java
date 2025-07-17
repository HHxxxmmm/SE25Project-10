package com.example.techprototype.Util;

import cn.hutool.core.util.IdUtil;

public class TicketNumberGenerator {
    
    private static final String TICKET_PREFIX = "T";
    
    /**
     * 生成车票号
     * 格式：T + 雪花算法ID
     */
    public static String generateTicketNumber() {
        return TICKET_PREFIX + IdUtil.getSnowflake(2, 2).nextId();
    }
    
    /**
     * 生成订单号
     * 格式：雪花算法ID
     */
    public static String generateOrderNumber() {
        return String.valueOf(IdUtil.getSnowflake(1, 1).nextId());
    }
} 