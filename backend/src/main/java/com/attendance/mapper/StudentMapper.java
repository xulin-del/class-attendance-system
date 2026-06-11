package com.attendance.mapper;

import com.attendance.entity.Student;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface StudentMapper extends BaseMapper<Student> {
    Student selectByStudentId(@Param("studentId") String studentId);
    Student selectByStudentIdAndName(@Param("studentId") String studentId, @Param("studentName") String studentName);
    Student selectByStudentIdAndPhone(@Param("studentId") String studentId, @Param("phone") String phone);
    List<Student> selectByClassName(@Param("className") String className);
}
