package com.attendance.mapper;

import com.attendance.entity.Teacher;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

public interface TeacherMapper extends BaseMapper<Teacher> {
    Teacher selectByTeacherId(@Param("teacherId") String teacherId);
    Teacher selectByTeacherIdAndPhone(@Param("teacherId") String teacherId, @Param("phone") String phone);
}
