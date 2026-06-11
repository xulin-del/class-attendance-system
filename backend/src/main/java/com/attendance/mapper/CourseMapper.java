package com.attendance.mapper;

import com.attendance.entity.Course;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface CourseMapper extends BaseMapper<Course> {
    List<Course> selectByTeacherId(@Param("teacherId") String teacherId);
    List<Course> selectByClassName(@Param("className") String className);
}