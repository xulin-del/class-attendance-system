package com.attendance.service;

import com.attendance.entity.Schedule;
import com.attendance.mapper.ScheduleMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ScheduleService {
    @Autowired
    private ScheduleMapper scheduleMapper;

    public List<Schedule> getSchedulesByTeacherId(String teacherId) {
        return scheduleMapper.selectByTeacherId(teacherId);
    }

    public boolean addSchedule(Schedule schedule) {
        return scheduleMapper.insert(schedule) > 0;
    }

    public boolean updateSchedule(Schedule schedule) {
        return scheduleMapper.updateById(schedule) > 0;
    }

    public boolean deleteSchedule(Integer id) {
        return scheduleMapper.deleteById(id) > 0;
    }
}
