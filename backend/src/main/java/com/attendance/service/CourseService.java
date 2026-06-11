package com.attendance.service;

import com.attendance.entity.Course;
import com.attendance.mapper.CourseMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CourseService {
    @Autowired
    private CourseMapper courseMapper;
    
    public List<Course> getCoursesByTeacherId(String teacherId) {
        return courseMapper.selectByTeacherId(teacherId);
    }
    
    public List<Course> getCoursesByClassName(String className) {
        return courseMapper.selectByClassName(className);
    }
    
    public Course getCourseById(Integer courseId) {
        return courseMapper.selectById(courseId);
    }
    
    public boolean addCourse(Course course) {
        return courseMapper.insert(course) > 0;
    }
    
    public boolean updateCourse(Course course) {
        return courseMapper.updateById(course) > 0;
    }
    
    public boolean deleteCourse(Integer courseId) {
        return courseMapper.deleteById(courseId) > 0;
    }
}