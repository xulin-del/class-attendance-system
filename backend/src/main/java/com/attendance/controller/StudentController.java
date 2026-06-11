package com.attendance.controller;

import com.attendance.common.Result;
import com.attendance.dto.LoginRequest;
import com.attendance.entity.Student;
import com.attendance.service.StudentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/student")
public class StudentController {
    private static final Logger log = LoggerFactory.getLogger(StudentController.class);

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginRequest request) {
        String studentId = request.getStudentId();
        String password = request.getPassword();
        String studentName = request.getStudentName();

        if (isBlank(studentId)) {
            return Result.error("学号不能为空");
        }
        if (isBlank(password) && isBlank(studentName)) {
            return Result.error("密码不能为空");
        }

        try {
            Student student = !isBlank(password)
                    ? studentService.loginByPassword(studentId, password)
                    : studentService.login(studentId, studentName);
            if (student != null) {
                return Result.success("登录成功", buildStudentData(student));
            }
            return Result.error("登录失败，学号或密码错误");
        } catch (Exception e) {
            log.error("学生登录失败", e);
            return Result.error("服务器错误: " + e.getMessage());
        }
    }

    @PostMapping("/register")
    public Result<Map<String, Object>> register(@RequestBody Map<String, String> request) {
        String studentId = trim(request.get("studentId"));
        String studentName = trim(request.get("studentName"));
        String className = trim(request.get("className"));
        String password = trim(request.get("password"));
        String phone = trim(request.get("phone"));

        if (isBlank(studentId) || isBlank(studentName) || isBlank(className) || isBlank(password) || isBlank(phone)) {
            return Result.error("请完整填写学号、姓名、班级、密码和手机号");
        }

        try {
            Student student = new Student();
            student.setStudentId(studentId);
            student.setStudentName(studentName);
            student.setClassName(className);
            student.setPassword(password);
            student.setPhone(phone);
            if (!studentService.register(student)) {
                return Result.error("注册失败，该学号已存在");
            }
            return Result.success("注册成功", buildStudentData(student));
        } catch (Exception e) {
            log.error("学生注册失败", e);
            return Result.error("服务器错误: " + e.getMessage());
        }
    }

    @PostMapping("/forgotPassword")
    public Result<Map<String, Object>> forgotPassword(@RequestBody Map<String, String> request) {
        String studentId = trim(request.get("studentId"));
        String phone = trim(request.get("phone"));

        if (isBlank(studentId) || isBlank(phone)) {
            return Result.error("请输入学号和注册手机号");
        }

        try {
            Student student = studentService.findByIdAndPhone(studentId, phone);
            if (student == null) {
                return Result.error("账号或手机号不匹配");
            }
            Map<String, Object> data = new HashMap<>();
            data.put("password", student.getPassword() == null ? student.getStudentName() : student.getPassword());
            return Result.success("手机号验证成功", data);
        } catch (Exception e) {
            log.error("学生找回密码失败", e);
            return Result.error("服务器错误: " + e.getMessage());
        }
    }

    @GetMapping("/info")
    public Result<Map<String, Object>> getInfo(@RequestParam String studentId) {
        try {
            Student student = studentService.getStudentById(studentId);
            if (student != null) {
                return Result.success("获取成功", buildStudentData(student));
            }
            return Result.error("获取失败");
        } catch (Exception e) {
            log.error("获取学生信息失败", e);
            return Result.error("服务器错误: " + e.getMessage());
        }
    }

    @GetMapping("/list")
    public Result<List<Student>> getStudentsByClassName(@RequestParam String className) {
        List<Student> students = studentService.getStudentsByClassName(className);
        return Result.success("获取成功", students);
    }

    private Map<String, Object> buildStudentData(Student student) {
        Map<String, Object> data = new HashMap<>();
        data.put("studentId", student.getStudentId());
        data.put("studentName", student.getStudentName());
        data.put("className", student.getClassName());
        data.put("phone", student.getPhone());
        return data;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
