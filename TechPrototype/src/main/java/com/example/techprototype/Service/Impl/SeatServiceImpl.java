package com.example.techprototype.Service.Impl;

import com.example.techprototype.Entity.Seat;
import com.example.techprototype.Entity.Ticket;
import com.example.techprototype.Entity.TrainCarriage;
import com.example.techprototype.Entity.TrainStop;
import com.example.techprototype.Repository.SeatRepository;
import com.example.techprototype.Repository.TrainCarriageRepository;
import com.example.techprototype.Repository.TrainStopRepository;
import com.example.techprototype.Service.SeatService;
import com.example.techprototype.Util.SeatBitmapUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class SeatServiceImpl implements SeatService {
    
    @Autowired
    private SeatRepository seatRepository;
    
    @Autowired
    private TrainCarriageRepository trainCarriageRepository;
    
    @Autowired
    private TrainStopRepository trainStopRepository;
    
    @Override
    @Transactional
    public void releaseSeat(Ticket ticket) {
        try {
            // 获取出发站和到达站的sequence_number
            Optional<TrainStop> departureStopOpt = trainStopRepository.findByTrainIdAndStopId(ticket.getTrainId(), ticket.getDepartureStopId());
            Optional<TrainStop> arrivalStopOpt = trainStopRepository.findByTrainIdAndStopId(ticket.getTrainId(), ticket.getArrivalStopId());
            
            if (!departureStopOpt.isPresent() || !arrivalStopOpt.isPresent()) {
                System.err.println("未找到站点信息: 车次" + ticket.getTrainId() + ", 出发站" + ticket.getDepartureStopId() + ", 到达站" + ticket.getArrivalStopId());
                return;
            }
            
            int departureSequence = departureStopOpt.get().getSequenceNumber();
            int arrivalSequence = arrivalStopOpt.get().getSequenceNumber();
            
            // 查找车厢
            Optional<TrainCarriage> carriageOpt = trainCarriageRepository.findByTrainIdAndCarriageNumber(
                    ticket.getTrainId(), ticket.getCarriageNumber());
            
            if (carriageOpt.isPresent()) {
                TrainCarriage carriage = carriageOpt.get();
                
                // 查找座位
                List<Seat> seats = seatRepository.findByCarriageIdAndSeatNumber(
                        carriage.getCarriageId(), ticket.getSeatNumber());
                
                if (!seats.isEmpty()) {
                    Seat seat = seats.get(0);
                    // 使用位图释放座位
                    releaseSeatBySequence(seat, ticket.getTravelDate(), departureSequence, arrivalSequence);
                    seatRepository.save(seat);
                    System.out.println("座位释放成功: 车次ID=" + ticket.getTrainId() + 
                                     ", 车厢号=" + ticket.getCarriageNumber() + 
                                     ", 座位号=" + ticket.getSeatNumber() +
                                     ", 日期=" + ticket.getTravelDate() +
                                     ", 出发序号=" + departureSequence + 
                                     ", 到达序号=" + arrivalSequence);
                }
            }
        } catch (Exception e) {
            System.err.println("释放座位失败: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional
    public void assignSeat(Ticket ticket) {
        // 获取出发站和到达站的sequence_number
        Optional<TrainStop> departureStopOpt = trainStopRepository.findByTrainIdAndStopId(ticket.getTrainId(), ticket.getDepartureStopId());
        Optional<TrainStop> arrivalStopOpt = trainStopRepository.findByTrainIdAndStopId(ticket.getTrainId(), ticket.getArrivalStopId());
        
        if (!departureStopOpt.isPresent() || !arrivalStopOpt.isPresent()) {
            System.err.println("未找到站点信息: 车次" + ticket.getTrainId() + ", 出发站" + ticket.getDepartureStopId() + ", 到达站" + ticket.getArrivalStopId());
            return;
        }
        
        int departureSequence = departureStopOpt.get().getSequenceNumber();
        int arrivalSequence = arrivalStopOpt.get().getSequenceNumber();
        
        // 查找可用座位
        Optional<Seat> availableSeat = findAvailableSeatBySequence(
                ticket.getTrainId(), 
                ticket.getCarriageTypeId(), 
                ticket.getTravelDate(),
                departureSequence, 
                arrivalSequence
        );
        
        if (availableSeat.isPresent()) {
            Seat seat = availableSeat.get();
            // 锁定座位
            lockSeatBySequence(seat, ticket.getTravelDate(), departureSequence, arrivalSequence);
            seatRepository.save(seat);
            
            // 根据车厢ID查找车厢号
            ticket.setCarriageNumber(getCarriageNumber(seat.getCarriageId()));
            ticket.setSeatNumber(seat.getSeatNumber());
            
            System.out.println("座位分配成功: 车次ID=" + ticket.getTrainId() + 
                             ", 车厢号=" + ticket.getCarriageNumber() + 
                             ", 座位号=" + ticket.getSeatNumber() +
                             ", 日期=" + ticket.getTravelDate() +
                             ", 出发序号=" + departureSequence + 
                             ", 到达序号=" + arrivalSequence);
        }
    }
    
    @Override
    @Transactional
    public Optional<Seat> findAvailableSeat(Integer trainId, Integer typeId, LocalDate travelDate, 
                                          Long departureStopId, Long arrivalStopId) {
        // 获取出发站和到达站的sequence_number
        Optional<TrainStop> departureStopOpt = trainStopRepository.findByTrainIdAndStopId(trainId, departureStopId);
        Optional<TrainStop> arrivalStopOpt = trainStopRepository.findByTrainIdAndStopId(trainId, arrivalStopId);
        
        if (!departureStopOpt.isPresent() || !arrivalStopOpt.isPresent()) {
            System.err.println("未找到站点信息: 车次" + trainId + ", 出发站" + departureStopId + ", 到达站" + arrivalStopId);
            return Optional.empty();
        }
        
        int departureSequence = departureStopOpt.get().getSequenceNumber();
        int arrivalSequence = arrivalStopOpt.get().getSequenceNumber();
        
        return findAvailableSeatBySequence(trainId, typeId, travelDate, departureSequence, arrivalSequence);
    }
    
    @Override
    public void lockSeat(Seat seat, LocalDate travelDate, Long departureStopId, Long arrivalStopId) {
        // 这里需要根据trainId获取sequence_number，暂时简化处理
        // 实际应该查询TrainStop表获取sequence_number
        System.err.println("lockSeat方法需要trainId参数，请使用lockSeatBySequence方法");
    }
    
    @Override
    public void releaseSeat(Seat seat, LocalDate travelDate, Long departureStopId, Long arrivalStopId) {
        // 这里需要根据trainId获取sequence_number，暂时简化处理
        // 实际应该查询TrainStop表获取sequence_number
        System.err.println("releaseSeat方法需要trainId参数，请使用releaseSeatBySequence方法");
    }
    
    @Override
    public boolean isSeatAvailable(Seat seat, LocalDate travelDate, Long departureStopId, Long arrivalStopId) {
        // 这里需要根据trainId获取sequence_number，暂时简化处理
        // 实际应该查询TrainStop表获取sequence_number
        System.err.println("isSeatAvailable方法需要trainId参数，请使用isSeatAvailableBySequence方法");
        return false;
    }
    
    @Override
    @Transactional
    public Optional<Seat> findAvailableSeatBySequence(Integer trainId, Integer typeId, LocalDate travelDate, 
                                                     int departureSequence, int arrivalSequence) {
        // 生成区间掩码
        long intervalMask = SeatBitmapUtil.generateIntervalMask(departureSequence, arrivalSequence);
        
        System.out.println("查找可用座位 - 车次:" + trainId + ", 车厢类型:" + typeId + 
                          ", 日期:" + travelDate + ", 出发序号:" + departureSequence + 
                          ", 到达序号:" + arrivalSequence + ", 区间掩码:" + intervalMask);
        
        // 查找指定车次和车厢类型的所有座位
        List<Seat> seats = seatRepository.findByTrainAndType(trainId, typeId);
        
        // 遍历座位，找到第一个可用的
        for (Seat seat : seats) {
            if (isSeatAvailableBySequence(seat, travelDate, departureSequence, arrivalSequence)) {
                return Optional.of(seat);
            }
        }
        
        return Optional.empty();
    }
    
    @Override
    public void lockSeatBySequence(Seat seat, LocalDate travelDate, int departureSequence, int arrivalSequence) {
        long intervalMask = SeatBitmapUtil.generateIntervalMask(departureSequence, arrivalSequence);
        SeatBitmapUtil.lockSeat(seat, travelDate, intervalMask);
        
        System.out.println("座位锁定: 座位ID=" + seat.getSeatId() + 
                         ", 日期=" + travelDate + 
                         ", 出发序号=" + departureSequence + 
                         ", 到达序号=" + arrivalSequence +
                         ", 区间掩码=" + intervalMask + 
                         ", 位图状态=" + SeatBitmapUtil.bitmapToString(getDateBitmap(seat, SeatBitmapUtil.getDateIndex(travelDate))));
    }
    
    @Override
    public void releaseSeatBySequence(Seat seat, LocalDate travelDate, int departureSequence, int arrivalSequence) {
        long intervalMask = SeatBitmapUtil.generateIntervalMask(departureSequence, arrivalSequence);
        SeatBitmapUtil.releaseSeat(seat, travelDate, intervalMask);
        
        System.out.println("座位释放: 座位ID=" + seat.getSeatId() + 
                         ", 日期=" + travelDate + 
                         ", 出发序号=" + departureSequence + 
                         ", 到达序号=" + arrivalSequence +
                         ", 区间掩码=" + intervalMask + 
                         ", 位图状态=" + SeatBitmapUtil.bitmapToString(getDateBitmap(seat, SeatBitmapUtil.getDateIndex(travelDate))));
    }
    
    @Override
    public boolean isSeatAvailableBySequence(Seat seat, LocalDate travelDate, int departureSequence, int arrivalSequence) {
        long intervalMask = SeatBitmapUtil.generateIntervalMask(departureSequence, arrivalSequence);
        return SeatBitmapUtil.isSeatAvailable(seat, travelDate, intervalMask);
    }
    
    /**
     * 根据车厢ID获取车厢号
     */
    private String getCarriageNumber(Long carriageId) {
        Optional<TrainCarriage> carriageOpt = trainCarriageRepository.findByCarriageId(carriageId);
        if (carriageOpt.isPresent()) {
            return carriageOpt.get().getCarriageNumber();
        }
        return "1"; // 默认值
    }
    
    /**
     * 获取指定日期的位图值
     */
    private long getDateBitmap(Seat seat, int dateIndex) {
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
} 