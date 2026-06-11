package com.attendance.mapper;

import com.attendance.entity.Schedule;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface ScheduleMapper extends BaseMapper<Schedule> {
    List<Schedule> selectByTeacherId(@Param("teacherId") String teacherId);
    List<Integer> selectDistinctDaysByTeacherId(@Param("teacherId") String teacherId);
    List<Schedule> selectByDayOfWeek(@Param("dayOfWeek") int dayOfWeek);
}
