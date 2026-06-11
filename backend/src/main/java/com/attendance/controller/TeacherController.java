package com.attendance.controller;

import com.attendance.common.Result;
import com.attendance.dto.LoginRequest;
import com.attendance.entity.Teacher;
import com.attendance.service.TeacherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/teacher")
public class TeacherController {
    private static final Logger log = LoggerFactory.getLogger(TeacherController.class);

    private final TeacherService teacherService;

    public TeacherController(TeacherService teacherService) {
        this.teacherService = teacherService;
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginRequest request) {
        String teacherId = request.getTeacherId();
        String password = request.getPassword();

        if (isBlank(teacherId)) {
            return Result.error("工号不能为空");
        }
        if (isBlank(password)) {
            return Result.error("密码不能为空");
        }

        try {
            Teacher teacher = teacherService.login(teacherId, password);
            if (teacher != null) {
                return Result.success("登录成功", buildTeacherData(teacher));
            }
            return Result.error("登录失败，工号或密码错误");
        } catch (Exception e) {
            log.error("教师登录失败", e);
            return Result.error("服务器错误: " + e.getMessage());
        }
    }

    @PostMapping("/register")
    public Result<Map<String, Object>> register(@RequestBody Map<String, String> request) {
        String teacherId = trim(request.get("teacherId"));
        String teacherName = trim(request.get("teacherName"));
        String password = trim(request.get("password"));
        String phone = trim(request.get("phone"));

        if (isBlank(teacherId) || isBlank(teacherName) || isBlank(password) || isBlank(phone)) {
            return Result.error("请完整填写工号、姓名、密码和手机号");
        }

        try {
            Teacher teacher = new Teacher();
            teacher.setTeacherId(teacherId);
            teacher.setTeacherName(teacherName);
            teacher.setPassword(password);
            teacher.setPhone(phone);
            if (!teacherService.register(teacher)) {
                return Result.error("注册失败，该工号已存在");
            }
            return Result.success("注册成功", buildTeacherData(teacher));
        } catch (Exception e) {
            log.error("教师注册失败", e);
            return Result.error("服务器错误: " + e.getMessage());
        }
    }

    @PostMapping("/forgotPassword")
    public Result<Map<String, Object>> forgotPassword(@RequestBody Map<String, String> request) {
        String teacherId = trim(request.get("teacherId"));
        String phone = trim(request.get("phone"));

        if (isBlank(teacherId) || isBlank(phone)) {
            return Result.error("请输入工号和注册手机号");
        }

        try {
            Teacher teacher = teacherService.findByIdAndPhone(teacherId, phone);
            if (teacher == null) {
                return Result.error("账号或手机号不匹配");
            }
            Map<String, Object> data = new HashMap<>();
            data.put("password", teacher.getPassword());
            return Result.success("手机号验证成功", data);
        } catch (Exception e) {
            log.error("教师找回密码失败", e);
            return Result.error("服务器错误: " + e.getMessage());
        }
    }

    @GetMapping("/info")
    public Result<Map<String, Object>> getInfo(@RequestParam String teacherId) {
        try {
            Teacher teacher = teacherService.getTeacherById(teacherId);
            if (teacher != null) {
                return Result.success("获取成功", buildTeacherData(teacher));
            }
            return Result.error("获取失败");
        } catch (Exception e) {
            log.error("获取教师信息失败", e);
            return Result.error("服务器错误: " + e.getMessage());
        }
    }

    private Map<String, Object> buildTeacherData(Teacher teacher) {
        Map<String, Object> data = new HashMap<>();
        data.put("teacherId", teacher.getTeacherId());
        data.put("teacherName", teacher.getTeacherName());
        data.put("phone", teacher.getPhone());
        return data;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
