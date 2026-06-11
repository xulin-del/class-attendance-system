package com.attendance.service;

import com.attendance.entity.Teacher;
import com.attendance.mapper.TeacherMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TeacherService {
    @Autowired
    private TeacherMapper teacherMapper;
    
    public Teacher login(String teacherId, String password) {
        Teacher teacher = teacherMapper.selectByTeacherId(teacherId);
        if (teacher != null && teacher.getPassword().equals(password)) {
            return teacher;
        }
        return null;
    }

    public boolean register(Teacher teacher) {
        if (teacherMapper.selectByTeacherId(teacher.getTeacherId()) != null) {
            return false;
        }
        return teacherMapper.insert(teacher) > 0;
    }

    public Teacher findByIdAndPhone(String teacherId, String phone) {
        return teacherMapper.selectByTeacherIdAndPhone(teacherId, phone);
    }
    
    public Teacher getTeacherById(String teacherId) {
        return teacherMapper.selectByTeacherId(teacherId);
    }
}
