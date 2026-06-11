package com.attendance.mapper;

import com.attendance.entity.AttendanceApplication;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface AttendanceApplicationMapper extends BaseMapper<AttendanceApplication> {
    AttendanceApplication selectByRecordId(@Param("recordId") Integer recordId);
    List<AttendanceApplication> selectPendingByTeacherId(@Param("teacherId") String teacherId);
    int countPendingByTeacherId(@Param("teacherId") String teacherId);
    List<AttendanceApplication> selectByStudentId(@Param("studentId") String studentId);
}
