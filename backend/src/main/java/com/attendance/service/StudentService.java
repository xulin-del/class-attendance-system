package com.attendance.service;

import com.attendance.entity.Student;
import com.attendance.mapper.StudentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class StudentService {
    @Autowired
    private StudentMapper studentMapper;
    
    public Student login(String studentId, String studentName) {
        return studentMapper.selectByStudentIdAndName(studentId, studentName);
    }

    public Student loginByPassword(String studentId, String password) {
        Student student = studentMapper.selectByStudentId(studentId);
        if (student == null) {
            return null;
        }
        if (student.getPassword() != null && student.getPassword().equals(password)) {
            return student;
        }
        if (student.getPassword() == null && student.getStudentName().equals(password)) {
            return student;
        }
        return null;
    }

    public boolean register(Student student) {
        if (studentMapper.selectByStudentId(student.getStudentId()) != null) {
            return false;
        }
        return studentMapper.insert(student) > 0;
    }

    public Student findByIdAndPhone(String studentId, String phone) {
        return studentMapper.selectByStudentIdAndPhone(studentId, phone);
    }
    
    public Student getStudentById(String studentId) {
        return studentMapper.selectByStudentId(studentId);
    }
    
    public List<Student> getStudentsByClassName(String className) {
        return studentMapper.selectByClassName(className);
    }

    public boolean updateStudent(Student student) {
        return studentMapper.updateById(student) > 0;
    }
}
