package com.example.techprototype.Util;

import java.time.LocalDate;

/**
 * 座位位图管理工具类
 * 用于处理日期和区间的位图操作
 * 全局基准日期：2025-07-01
 * 日期范围：2025-07-01 到 2025-07-10 (date_1 到 date_10)
 * 区间位图：使用最后10位表示区间占用，每个站点用2个bit表示
 * 从最低位开始，每2个bit表示一个站点：
 * - 低位：1表示到达该站点
 * - 高位：1表示离开该站点
 * 例如：区间1:2的bit码是0110，表示离开1号站点(10)和到达2号站点(01)
 */
public class SeatBitmapUtil {
    
    // 全局基准日期
    public static final LocalDate BASE_DATE = LocalDate.of(2025, 7, 1);
    
    /**
     * 将日期转换为对应的日期索引 (1-10)
     * @param date 目标日期
     * @return 日期索引，如果超出范围返回-1
     */
    public static int getDateIndex(LocalDate date) {
        if (date.isBefore(BASE_DATE) || date.isAfter(BASE_DATE.plusDays(9))) {
            return -1; // 超出范围
        }
        return (int) (date.toEpochDay() - BASE_DATE.toEpochDay() + 1);
    }
    
    /**
     * 根据出发站和到达站的sequence_number生成区间掩码
     * @param departureSequence 出发站序号
     * @param arrivalSequence 到达站序号
     * @return 区间掩码
     */
    public static long generateIntervalMask(int departureSequence, int arrivalSequence) {
        if (departureSequence >= arrivalSequence) {
            return 0L; // 无效区间
        }
        
        long mask = 0L;
        
        // 设置出发站：高位为1（离开）
        // 出发站的bit位置：2*(departureSequence-1) 和 2*(departureSequence-1)+1
        int departureLowBit = 2 * (departureSequence - 1);
        int departureHighBit = 2 * (departureSequence - 1) + 1;
        mask |= (1L << departureHighBit); // 高位为1（离开）
        
        // 设置到达站：低位为1（到达）
        // 到达站的bit位置：2*(arrivalSequence-1) 和 2*(arrivalSequence-1)+1
        int arrivalLowBit = 2 * (arrivalSequence - 1);
        int arrivalHighBit = 2 * (arrivalSequence - 1) + 1;
        mask |= (1L << arrivalLowBit); // 低位为1（到达）
        
        // 设置中间站点：两个bit都为1（经过）
        for (int i = departureSequence + 1; i < arrivalSequence; i++) {
            int lowBit = 2 * (i - 1);
            int highBit = 2 * (i - 1) + 1;
            mask |= (1L << lowBit);     // 低位（到达）
            mask |= (1L << highBit);    // 高位（离开）
        }
        
        System.out.println("生成区间掩码 - 出发序号:" + departureSequence + 
                         ", 到达序号:" + arrivalSequence + 
                         ", 掩码:" + mask + 
                         ", 二进制:" + Long.toBinaryString(mask));
        
        return mask;
    }
    
    /**
     * 检查座位在指定日期和区间是否可用
     * @param seat 座位对象
     * @param date 目标日期
     * @param intervalMask 区间掩码
     * @return true表示可用，false表示不可用
     */
    public static boolean isSeatAvailable(com.example.techprototype.Entity.Seat seat, LocalDate date, long intervalMask) {
        int dateIndex = getDateIndex(date);
        if (dateIndex == -1) {
            return false; // 日期超出范围
        }
        
        long currentBitmap = getDateBitmap(seat, dateIndex);
        return (currentBitmap & intervalMask) == 0; // 没有冲突
    }
    
    /**
     * 锁定座位（设置区间占用）
     * @param seat 座位对象
     * @param date 目标日期
     * @param intervalMask 区间掩码
     */
    public static void lockSeat(com.example.techprototype.Entity.Seat seat, LocalDate date, long intervalMask) {
        int dateIndex = getDateIndex(date);
        if (dateIndex == -1) {
            return; // 日期超出范围
        }
        
        long currentBitmap = getDateBitmap(seat, dateIndex);
        long newBitmap = currentBitmap | intervalMask; // OR操作设置占用
        setDateBitmap(seat, dateIndex, newBitmap);
    }
    
    /**
     * 释放座位（清除区间占用）
     * @param seat 座位对象
     * @param date 目标日期
     * @param intervalMask 区间掩码
     */
    public static void releaseSeat(com.example.techprototype.Entity.Seat seat, LocalDate date, long intervalMask) {
        int dateIndex = getDateIndex(date);
        if (dateIndex == -1) {
            return; // 日期超出范围
        }
        
        long currentBitmap = getDateBitmap(seat, dateIndex);
        long newBitmap = currentBitmap & (~intervalMask); // AND操作清除占用
        setDateBitmap(seat, dateIndex, newBitmap);
    }
    
    /**
     * 获取指定日期的位图值
     * @param seat 座位对象
     * @param dateIndex 日期索引 (1-10)
     * @return 位图值
     */
    private static long getDateBitmap(com.example.techprototype.Entity.Seat seat, int dateIndex) {
        switch (dateIndex) {
            case 1: return seat.getDate1();
            case 2: return seat.getDate2();
            case 3: return seat.getDate3();
            case 4: return seat.getDate4();
            case 5: return seat.getDate5();
            case 6: return seat.getDate6();
            case 7: return seat.getDate7();
            case 8: return seat.getDate8();
            case 9: return seat.getDate9();
            case 10: return seat.getDate10();
            default: return 0L;
        }
    }
    
    /**
     * 设置指定日期的位图值
     * @param seat 座位对象
     * @param dateIndex 日期索引 (1-10)
     * @param bitmap 位图值
     */
    private static void setDateBitmap(com.example.techprototype.Entity.Seat seat, int dateIndex, long bitmap) {
        switch (dateIndex) {
            case 1: seat.setDate1(bitmap); break;
            case 2: seat.setDate2(bitmap); break;
            case 3: seat.setDate3(bitmap); break;
            case 4: seat.setDate4(bitmap); break;
            case 5: seat.setDate5(bitmap); break;
            case 6: seat.setDate6(bitmap); break;
            case 7: seat.setDate7(bitmap); break;
            case 8: seat.setDate8(bitmap); break;
            case 9: seat.setDate9(bitmap); break;
            case 10: seat.setDate10(bitmap); break;
        }
    }
    
    /**
     * 将位图转换为可读的字符串
     * @param bitmap 位图值
     * @return 可读的字符串，如 "站点1离开,站点2到达"
     */
    public static String bitmapToString(long bitmap) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        
        // 检查每个站点的2个bit
        for (int i = 1; i <= 5; i++) { // 假设最多5个站点
            int lowBit = 2 * i - 1;
            int highBit = 2 * i;
            
            boolean hasArrival = (bitmap & (1L << lowBit)) != 0;
            boolean hasDeparture = (bitmap & (1L << highBit)) != 0;
            
            if (hasArrival || hasDeparture) {
                if (!first) sb.append(",");
                if (hasArrival && hasDeparture) {
                    sb.append("站点").append(i).append("经过");
                } else if (hasArrival) {
                    sb.append("站点").append(i).append("到达");
                } else if (hasDeparture) {
                    sb.append("站点").append(i).append("离开");
                }
                first = false;
            }
        }
        
        return sb.length() == 0 ? "无占用" : sb.toString();
    }
} 