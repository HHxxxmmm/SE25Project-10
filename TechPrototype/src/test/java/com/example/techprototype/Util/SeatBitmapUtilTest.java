package com.example.techprototype.Util;

import com.example.techprototype.Entity.Seat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class SeatBitmapUtilTest {
    private Seat seat;
    private LocalDate testDate;

    @BeforeEach
    void setUp() {
        seat = new Seat();
        seat.setSeatId(1L);
        seat.setSeatNumber("1A");
        seat.setCarriageId(1L);
        seat.setDate1(0L);
        seat.setDate2(0L);
        seat.setDate3(0L);
        seat.setDate4(0L);
        seat.setDate5(0L);
        seat.setDate6(0L);
        seat.setDate7(0L);
        seat.setDate8(0L);
        seat.setDate9(0L);
        seat.setDate10(0L);
        
        // 使用基准日期作为测试日期
        testDate = SeatBitmapUtil.BASE_DATE;
    }

    @Test
    void testGetDateIndex_ValidDate() {
        LocalDate validDate = SeatBitmapUtil.BASE_DATE;
        assertEquals(1, SeatBitmapUtil.getDateIndex(validDate));
        
        validDate = SeatBitmapUtil.BASE_DATE.plusDays(4);
        assertEquals(5, SeatBitmapUtil.getDateIndex(validDate));
        
        validDate = SeatBitmapUtil.BASE_DATE.plusDays(9);
        assertEquals(10, SeatBitmapUtil.getDateIndex(validDate));
    }

    @Test
    void testGetDateIndex_InvalidDate() {
        LocalDate invalidDate = SeatBitmapUtil.BASE_DATE.minusDays(1);
        assertEquals(-1, SeatBitmapUtil.getDateIndex(invalidDate));
        
        invalidDate = SeatBitmapUtil.BASE_DATE.plusDays(10);
        assertEquals(-1, SeatBitmapUtil.getDateIndex(invalidDate));
    }

    @Test
    void testGenerateIntervalMask_ValidInterval() {
        // 测试区间1:2的掩码
        long mask = SeatBitmapUtil.generateIntervalMask(1, 2);
        assertEquals(6L, mask); // 二进制: 0110 (站点1离开:10, 站点2到达:01)
        
        // 测试区间1:3的掩码
        mask = SeatBitmapUtil.generateIntervalMask(1, 3);
        assertEquals(30L, mask); // 二进制: 11110 (站点1离开:10, 站点2经过:11, 站点3到达:01)
        
        // 测试区间2:4的掩码
        mask = SeatBitmapUtil.generateIntervalMask(2, 4);
        assertEquals(120L, mask); // 二进制: 1111000 (站点2离开:1000, 站点3经过:1100, 站点4到达:0001)
    }

    @Test
    void testGenerateIntervalMask_InvalidInterval() {
        // 出发站序号大于等于到达站序号
        long mask = SeatBitmapUtil.generateIntervalMask(2, 1);
        assertEquals(0L, mask);
        
        mask = SeatBitmapUtil.generateIntervalMask(1, 1);
        assertEquals(0L, mask);
    }

    @Test
    void testIsSeatAvailable_Available() {
        long intervalMask = SeatBitmapUtil.generateIntervalMask(1, 2);
        
        assertTrue(SeatBitmapUtil.isSeatAvailable(seat, testDate, intervalMask));
    }

    @Test
    void testIsSeatAvailable_NotAvailable() {
        long intervalMask = SeatBitmapUtil.generateIntervalMask(1, 2);
        
        // 先锁定座位
        SeatBitmapUtil.lockSeat(seat, testDate, intervalMask);
        
        // 检查是否不可用
        assertFalse(SeatBitmapUtil.isSeatAvailable(seat, testDate, intervalMask));
    }

    @Test
    void testIsSeatAvailable_InvalidDate() {
        LocalDate invalidDate = SeatBitmapUtil.BASE_DATE.minusDays(1);
        long intervalMask = SeatBitmapUtil.generateIntervalMask(1, 2);
        
        assertFalse(SeatBitmapUtil.isSeatAvailable(seat, invalidDate, intervalMask));
    }

    @Test
    void testLockSeat() {
        long intervalMask = SeatBitmapUtil.generateIntervalMask(1, 2);
        
        SeatBitmapUtil.lockSeat(seat, testDate, intervalMask);
        
        // 验证座位已被锁定
        assertFalse(SeatBitmapUtil.isSeatAvailable(seat, testDate, intervalMask));
        assertEquals(6L, seat.getDate1()); // 二进制: 0110
    }

    @Test
    void testReleaseSeat() {
        long intervalMask = SeatBitmapUtil.generateIntervalMask(1, 2);
        
        // 先锁定座位
        SeatBitmapUtil.lockSeat(seat, testDate, intervalMask);
        assertFalse(SeatBitmapUtil.isSeatAvailable(seat, testDate, intervalMask));
        
        // 释放座位
        SeatBitmapUtil.releaseSeat(seat, testDate, intervalMask);
        
        // 验证座位已被释放
        assertTrue(SeatBitmapUtil.isSeatAvailable(seat, testDate, intervalMask));
        assertEquals(0L, seat.getDate1());
    }

    @Test
    void testBitmapToString() {
        // 测试空位图
        assertEquals("无占用", SeatBitmapUtil.bitmapToString(0L));
        
        // 测试区间1:2的位图
        long bitmap = SeatBitmapUtil.generateIntervalMask(1, 2);
        String result = SeatBitmapUtil.bitmapToString(bitmap);
        assertTrue(result.contains("站点1") || result.contains("站点2"));
        
        // 测试复杂位图
        long complexBitmap = SeatBitmapUtil.generateIntervalMask(1, 3);
        result = SeatBitmapUtil.bitmapToString(complexBitmap);
        assertTrue(result.contains("站点"));
    }

    @Test
    void testLockAndReleaseMultipleIntervals() {
        // 锁定区间1:2
        long mask1 = SeatBitmapUtil.generateIntervalMask(1, 2);
        SeatBitmapUtil.lockSeat(seat, testDate, mask1);
        
        // 锁定区间3:4
        long mask2 = SeatBitmapUtil.generateIntervalMask(3, 4);
        SeatBitmapUtil.lockSeat(seat, testDate, mask2);
        
        // 验证两个区间都被锁定
        assertFalse(SeatBitmapUtil.isSeatAvailable(seat, testDate, mask1));
        assertFalse(SeatBitmapUtil.isSeatAvailable(seat, testDate, mask2));
        
        // 释放区间1:2
        SeatBitmapUtil.releaseSeat(seat, testDate, mask1);
        
        // 验证区间1:2已释放，区间3:4仍被锁定
        assertTrue(SeatBitmapUtil.isSeatAvailable(seat, testDate, mask1));
        assertFalse(SeatBitmapUtil.isSeatAvailable(seat, testDate, mask2));
    }
} 