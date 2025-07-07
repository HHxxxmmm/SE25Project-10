package com.example.techprototype.Service.Impl;

import com.example.techprototype.Entity.Seat;
import com.example.techprototype.Entity.Ticket;
import com.example.techprototype.Entity.TrainCarriage;
import com.example.techprototype.Repository.SeatRepository;
import com.example.techprototype.Repository.TrainCarriageRepository;
import com.example.techprototype.Service.SeatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SeatServiceImpl implements SeatService {
    
    @Autowired
    private SeatRepository seatRepository;
    
    @Autowired
    private TrainCarriageRepository trainCarriageRepository;
    
    @Override
    public void releaseSeat(Ticket ticket) {
        try {
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
                    seat.setIsAvailable(true);
                    seatRepository.save(seat);
                    System.out.println("座位释放成功: 车次ID=" + ticket.getTrainId() + 
                                     ", 车厢号=" + ticket.getCarriageNumber() + 
                                     ", 座位号=" + ticket.getSeatNumber());
                }
            }
        } catch (Exception e) {
            System.err.println("释放座位失败: " + e.getMessage());
        }
    }
    
    @Override
    public void assignSeat(Ticket ticket) {
        // 查找可用座位
        List<Seat> availableSeats = seatRepository.findAvailableSeatsByTrainAndType(ticket.getTrainId(), ticket.getCarriageTypeId());
        if (!availableSeats.isEmpty()) {
            Seat seat = availableSeats.get(0);
            // 分配座位时设置为不可用
            seat.setIsAvailable(false);
            seatRepository.save(seat);
            
            // 根据车厢ID查找车厢号
            ticket.setCarriageNumber(getCarriageNumber(seat.getCarriageId()));
            ticket.setSeatNumber(seat.getSeatNumber());
        }
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
} 