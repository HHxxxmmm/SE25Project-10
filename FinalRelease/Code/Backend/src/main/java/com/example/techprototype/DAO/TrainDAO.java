package com.example.techprototype.DAO;

import com.example.techprototype.Entity.Train;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface TrainDAO {
    // 直达
    @Select("SELECT t.* FROM trains t " +
            "JOIN train_stops s1 ON t.train_id = s1.train_id " +
            "JOIN train_stops s2 ON t.train_id = s2.train_id " +
            "WHERE s1.station_id = #{startStationId} AND s2.station_id = #{endStationId} AND s1.sequence_number < s2.sequence_number")
    List<Train> findDirectTrains(@Param("startStationId") Integer startStationId, @Param("endStationId") Integer endStationId);

    // 按时间区间
    @Select("SELECT t.* FROM trains t WHERE t.departure_time BETWEEN #{start} AND #{end}")
    List<Train> findByDepartureTimeBetween(@Param("start") LocalTime start, @Param("end") LocalTime end);

    // 中转查询优化方案
    @Select("SELECT " +
            "t1.train_id AS t1_train_id, t1.train_number AS t1_train_number, " +
            "t1.train_type AS t1_train_type, t1.departure_time AS t1_departure_time, " +
            "t1.arrival_time AS t1_arrival_time, t1.duration_minutes AS t1_duration_minutes, " +
            "s1a.station_id AS start_station_id, s1b.station_id AS transfer_station_id, " +
            "t2.train_id AS t2_train_id, t2.train_number AS t2_train_number, " +
            "t2.train_type AS t2_train_type, t2.departure_time AS t2_departure_time, " +
            "t2.arrival_time AS t2_arrival_time, t2.duration_minutes AS t2_duration_minutes, " +
            "s2b.station_id AS end_station_id " +
            "FROM trains t1 " +
            "JOIN train_stops s1a ON t1.train_id = s1a.train_id " +
            "JOIN train_stops s1b ON t1.train_id = s1b.train_id " +
            "JOIN trains t2 ON t2.train_id IN ( " +
            "   SELECT DISTINCT train_id FROM train_stops " +
            "   WHERE station_id = #{transferStationId} OR station_id = #{endStationId}) " +
            "JOIN train_stops s2a ON t2.train_id = s2a.train_id " +
            "JOIN train_stops s2b ON t2.train_id = s2b.train_id " +
            "WHERE s1a.station_id = #{startStationId} " +
            "AND s1b.station_id = #{transferStationId} " +
            "AND s2a.station_id = #{transferStationId} " +
            "AND s2b.station_id = #{endStationId} " +
            "AND s1a.sequence_number < s1b.sequence_number " +
            "AND s2a.sequence_number < s2b.sequence_number " +
            "AND t1.train_id != t2.train_id")
    List<Map<String, Object>> findTransferTrains(
            @Param("startStationId") Integer startStationId,
            @Param("transferStationId") Integer transferStationId,
            @Param("endStationId") Integer endStationId
    );

    // 添加中转站查询方法
    @Select("SELECT DISTINCT ts.station_id " +
            "FROM train_stops ts " +
            "WHERE ts.station_id NOT IN (#{startStationId}, #{endStationId}) " +
            "  AND EXISTS (SELECT 1 FROM train_stops s1 " +
            "              WHERE s1.station_id = #{startStationId} " +
            "                AND s1.train_id = ts.train_id) " +
            "  AND EXISTS (SELECT 1 FROM train_stops s2 " +
            "              WHERE s2.station_id = #{endStationId} " +
            "                AND s2.train_id = ts.train_id)")
    List<Integer> findAllTransferStations(
            @Param("startStationId") Integer startStationId,
            @Param("endStationId") Integer endStationId);
}
