package com.example.techprototype.DTO;

import lombok.Data;
import java.util.List;

@Data
public class TrainListDTO {
    private String train_id;         // 车次号，如G-123
    private int t_station_number;    // 站点数
    private List<String> t_path;     // 路线（途经站名）
    private String t_from;           // 起点站名
    private String t_to;             // 终点站名
    private String t_start_time;     // 发车时间（yyyy-MM-dd HH:mm:ss）
    private String t_end_time;       // 到达时间（yyyy-MM-dd HH:mm:ss）
    private List<Integer> seat;      // 席别类型（如1头等、2商务、3二等、4无座）
    private List<Integer> seat_number; // 各席别余票
    private List<Integer> seat_price;  // 各席别价格
}