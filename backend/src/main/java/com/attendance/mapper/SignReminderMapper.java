package com.attendance.mapper;

import com.attendance.entity.SignReminder;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface SignReminderMapper extends BaseMapper<SignReminder> {
    List<SignReminder> selectPendingByClassName(@Param("className") String className);
    List<SignReminder> selectPendingByStudentId(@Param("studentId") String studentId);
    int countClassReminder(@Param("scheduleId") Integer scheduleId, @Param("reminderDate") String reminderDate);
    void updateStatusById(@Param("id") Integer id, @Param("status") String status);
}
