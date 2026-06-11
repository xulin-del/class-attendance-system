package com.attendance.mapper;

import com.attendance.entity.AttendanceRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

public interface AttendanceRecordMapper extends BaseMapper<AttendanceRecord> {
    List<AttendanceRecord> selectByCourseId(@Param("courseId") Integer courseId);
    List<AttendanceRecord> selectByStudentId(@Param("studentId") String studentId);
    AttendanceRecord selectByCourseIdAndStudentId(@Param("courseId") Integer courseId, @Param("studentId") String studentId);
    List<Map<String, Object>> selectDailyStatsByTeacherId(@Param("teacherId") String teacherId, @Param("startDate") String startDate, @Param("endDate") String endDate);
    List<Map<String, Object>> selectRecordsByTeacherIdAndDate(@Param("teacherId") String teacherId, @Param("date") String date);
    List<Map<String, Object>> selectStudentRecordsWithCourse(@Param("studentId") String studentId, @Param("offset") Integer offset, @Param("limit") Integer limit);
    Integer countStudentRecords(@Param("studentId") String studentId);
    List<Map<String, Object>> selectRecordsByTeacherId(@Param("teacherId") String teacherId);
    Integer countCoursesByTeacherId(@Param("teacherId") String teacherId);
    Integer countStudentsByTeacherId(@Param("teacherId") String teacherId);
    Integer countTodaySignedByTeacherId(@Param("teacherId") String teacherId, @Param("date") String date);
}