package com.example.techprototype.Service;

import com.example.techprototype.Entity.Ticket;
import com.example.techprototype.Entity.TrainStop;
import com.example.techprototype.Repository.TicketRepository;
import com.example.techprototype.Repository.TrainStopRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;

@Service
public class TimeConflictService {
    
    @Autowired
    private TicketRepository ticketRepository;
    
    @Autowired
    private TrainStopRepository trainStopRepository;
    
    /**
     * 检查乘客在指定时间段内是否有时间冲突的车票
     * 
     * @param passengerId 乘客ID
     * @param travelDate 出行日期
     * @param trainId 车次ID
     * @param departureStopId 出发站ID
     * @param arrivalStopId 到达站ID
     * @param excludeTicketId 排除的车票ID（用于改签时排除原票）
     * @return 冲突的车票列表，如果没有冲突返回空列表
     */
    public List<Ticket> checkTimeConflict(Long passengerId, LocalDate travelDate, 
                                        Integer trainId, Long departureStopId, Long arrivalStopId,
                                        Long excludeTicketId) {
        
        System.out.println("开始检查时间冲突 - 乘客ID: " + passengerId + ", 车次: " + trainId + 
                          ", 出发站: " + departureStopId + ", 到达站: " + arrivalStopId + 
                          ", 日期: " + travelDate);
        
        // 获取新票的时间段
        LocalTime newDepartureTime = getDepartureTime(trainId, departureStopId);
        LocalTime newArrivalTime = getArrivalTime(trainId, arrivalStopId);
        
        System.out.println("新票时间 - 出发: " + newDepartureTime + ", 到达: " + newArrivalTime);
        
        // 对于区间票，使用出发站的出发时间和到达站的到达时间
        if (newDepartureTime == null) {
            System.out.println("无法获取出发时间，跳过时间冲突检测");
            return List.of();
        }
        
        if (newArrivalTime == null) {
            System.out.println("无法获取到达时间，跳过时间冲突检测");
            return List.of();
        }
        
        // 计算新票的实际出发和到达日期时间
        LocalDateTime newDepartureDateTime = LocalDateTime.of(travelDate, newDepartureTime);
        LocalDateTime newArrivalDateTime = LocalDateTime.of(travelDate, newArrivalTime);
        
        // 如果到达时间早于出发时间，说明跨天了
        if (newArrivalTime.isBefore(newDepartureTime)) {
            newArrivalDateTime = newArrivalDateTime.plusDays(1);
            System.out.println("检测到跨天车票，到达时间调整为: " + newArrivalDateTime);
        }
        
        // 获取需要检查的日期范围（新票出发日期前后各1天，以覆盖跨天情况）
        LocalDate checkStartDate = travelDate.minusDays(1);
        LocalDate checkEndDate = newArrivalDateTime.toLocalDate();
        
        System.out.println("检查日期范围: " + checkStartDate + " 到 " + checkEndDate);
        
        // 查询乘客在日期范围内的所有有效车票
        List<Ticket> allExistingTickets = new ArrayList<>();
        for (LocalDate date = checkStartDate; !date.isAfter(checkEndDate); date = date.plusDays(1)) {
            List<Ticket> ticketsForDate;
            if (excludeTicketId != null) {
                ticketsForDate = ticketRepository.findValidTicketsByPassengerAndDate(
                        passengerId, date, excludeTicketId);
            } else {
                ticketsForDate = ticketRepository.findValidTicketsByPassengerAndDate(
                        passengerId, date);
            }
            System.out.println("日期 " + date + " 找到 " + ticketsForDate.size() + " 张车票");
            allExistingTickets.addAll(ticketsForDate);
        }
        
        System.out.println("总共找到 " + allExistingTickets.size() + " 张现有车票");
        
        // 创建final变量用于lambda表达式
        final LocalDateTime finalNewDepartureDateTime = newDepartureDateTime;
        final LocalDateTime finalNewArrivalDateTime = newArrivalDateTime;
        
        // 检查时间冲突
        List<Ticket> conflictTickets = allExistingTickets.stream()
                .filter(ticket -> hasTimeConflict(ticket, finalNewDepartureDateTime, finalNewArrivalDateTime))
                .toList();
        
        System.out.println("检测到 " + conflictTickets.size() + " 张冲突车票");
        
        return conflictTickets;
    }
    
    /**
     * 检查乘客在指定时间段内是否有时间冲突的车票（不排除任何车票）
     */
    public List<Ticket> checkTimeConflict(Long passengerId, LocalDate travelDate, 
                                        Integer trainId, Long departureStopId, Long arrivalStopId) {
        return checkTimeConflict(passengerId, travelDate, trainId, departureStopId, arrivalStopId, null);
    }
    
    /**
     * 检查两个时间段是否有冲突
     * 冲突条件：两个时间段有重叠
     */
    private boolean hasTimeConflict(Ticket existingTicket, LocalDateTime newDepartureDateTime, LocalDateTime newArrivalDateTime) {
        // 获取现有车票的时间段
        LocalTime existingDepartureTime = getDepartureTime(existingTicket.getTrainId(), existingTicket.getDepartureStopId());
        LocalTime existingArrivalTime = getArrivalTime(existingTicket.getTrainId(), existingTicket.getArrivalStopId());
        
        if (existingDepartureTime == null || existingArrivalTime == null) {
            return false; // 无法获取时间信息，不阻止
        }
        
        // 计算现有车票的实际出发和到达日期时间
        LocalDateTime existingDepartureDateTime = LocalDateTime.of(existingTicket.getTravelDate(), existingDepartureTime);
        LocalDateTime existingArrivalDateTime = LocalDateTime.of(existingTicket.getTravelDate(), existingArrivalTime);
        
        // 如果到达时间早于出发时间，说明跨天了
        if (existingArrivalTime.isBefore(existingDepartureTime)) {
            existingArrivalDateTime = existingArrivalDateTime.plusDays(1);
        }
        
        // 检查时间重叠
        // 情况1：新票的出发时间在现有票的时间段内
        if (newDepartureDateTime.isAfter(existingDepartureDateTime) && newDepartureDateTime.isBefore(existingArrivalDateTime)) {
            return true;
        }
        
        // 情况2：新票的到达时间在现有票的时间段内
        if (newArrivalDateTime.isAfter(existingDepartureDateTime) && newArrivalDateTime.isBefore(existingArrivalDateTime)) {
            return true;
        }
        
        // 情况3：新票的时间段完全包含现有票的时间段
        if (newDepartureDateTime.isBefore(existingDepartureDateTime) && newArrivalDateTime.isAfter(existingArrivalDateTime)) {
            return true;
        }
        
        // 情况4：新票的时间段与现有票的时间段完全重叠
        if (newDepartureDateTime.equals(existingDepartureDateTime) && newArrivalDateTime.equals(existingArrivalDateTime)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 获取车次在指定站的出发时间
     */
    private LocalTime getDepartureTime(Integer trainId, Long stopId) {
        Optional<TrainStop> trainStopOpt = trainStopRepository.findByStopId(stopId);
        LocalTime departureTime = trainStopOpt.map(TrainStop::getDepartureTime).orElse(null);
        System.out.println("查询出发时间 - stopId: " + stopId + ", 结果: " + departureTime);
        return departureTime;
    }
    
    /**
     * 获取车次在指定站的到达时间
     */
    private LocalTime getArrivalTime(Integer trainId, Long stopId) {
        Optional<TrainStop> trainStopOpt = trainStopRepository.findByStopId(stopId);
        LocalTime arrivalTime = trainStopOpt.map(TrainStop::getArrivalTime).orElse(null);
        System.out.println("查询到达时间 - stopId: " + stopId + ", 结果: " + arrivalTime);
        return arrivalTime;
    }
    
    /**
     * 生成时间冲突的错误信息
     */
    public String generateConflictMessage(List<Ticket> conflictTickets) {
        if (conflictTickets.isEmpty()) {
            return "";
        }
        
        StringBuilder message = new StringBuilder("乘客在同一时间段内已有其他车票，存在时间冲突：\n");
        
        for (Ticket ticket : conflictTickets) {
            LocalTime departureTime = getDepartureTime(ticket.getTrainId(), ticket.getDepartureStopId());
            LocalTime arrivalTime = getArrivalTime(ticket.getTrainId(), ticket.getArrivalStopId());
            
            // 计算实际到达日期
            LocalDate actualArrivalDate = ticket.getTravelDate();
            if (arrivalTime != null && departureTime != null && arrivalTime.isBefore(departureTime)) {
                actualArrivalDate = actualArrivalDate.plusDays(1);
            }
            
            message.append(String.format("- 车票号: %s, 车次: %d, 日期: %s, 时间: %s-%s\n", 
                    ticket.getTicketNumber(), 
                    ticket.getTrainId(),
                    ticket.getTravelDate() + (actualArrivalDate.equals(ticket.getTravelDate()) ? "" : "~" + actualArrivalDate),
                    departureTime != null ? departureTime.toString() : "未知",
                    arrivalTime != null ? arrivalTime.toString() : "未知"));
        }
        
        return message.toString();
    }
} 