package com.mspathandschedule.repositories;

import com.mspathandschedule.entities.Schedule;
import com.mspathandschedule.entities.ScheduleStatus;
import com.mspathandschedule.entities.ScheduleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    List<Schedule> findByPlanningId(Long planningId);
    List<Schedule> findByStartTimeBetween(LocalDateTime start, LocalDateTime end);
    List<Schedule> findByPlanningIdOrderByStartTimeAsc(Long planningId);

    List<Schedule> findByPlanningIdAndStatus(Long planningId, ScheduleStatus status);

    List<Schedule> findByPlanningIdAndType(Long planningId, ScheduleType type);

    @Query("SELECT s FROM Schedule s WHERE s.planningId = :planningId AND s.startTime >= :start AND s.endTime <= :end")
    List<Schedule> findSchedulesInTimeRange(@Param("planningId") Long planningId,
                                            @Param("start") LocalDateTime start,
                                            @Param("end") LocalDateTime end);

    @Query("SELECT s FROM Schedule s WHERE s.planningId = :planningId AND s.startTime > :now ORDER BY s.startTime ASC")
    List<Schedule> findUpcomingSchedules(@Param("planningId") Long planningId, @Param("now") LocalDateTime now);

    long countByPlanningIdAndStatus(Long planningId, ScheduleStatus status);

    @Query(value = "SELECT AVG(TIMESTAMPDIFF(HOUR, start_time, end_time)) FROM schedule WHERE planning_id = :planningId", nativeQuery = true)
    Double getAverageScheduleDurationNative(@Param("planningId") Long planningId);





    // Find schedules that overlap with a time range
    @Query("SELECT s FROM Schedule s WHERE s.startTime < :end AND s.endTime > :start")
    List<Schedule> findOverlappingSchedules(@Param("start") LocalDateTime start,
                                            @Param("end") LocalDateTime end);

    
}