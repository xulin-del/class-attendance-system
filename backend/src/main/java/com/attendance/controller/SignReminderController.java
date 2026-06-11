package com.attendance.controller;

import com.attendance.common.RequestParams;
import com.attendance.common.Result;
import com.attendance.entity.SignReminder;
import com.attendance.entity.Course;
import com.attendance.mapper.SignReminderMapper;
import com.attendance.mapper.CourseMapper;
import com.attendance.service.AttendanceService;
import com.attendance.service.ScheduleReminderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/signReminder")
public class SignReminderController {
    @Autowired
    private SignReminderMapper signReminderMapper;

    @Autowired
    private CourseMapper courseMapper;

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private ScheduleReminderService scheduleReminderService;

    @PostMapping("/create")
    public Result<Map<String, Object>> createReminder(@RequestBody Map<String, Object> params) {
        Integer courseId = RequestParams.getInteger(params, "courseId");
        String signCode = RequestParams.getString(params, "signCode");
        String teacherId = RequestParams.getString(params, "teacherId");

        if (courseId == null || signCode == null) {
            return Result.error("参数不完整");
        }

        Course course = courseMapper.selectById(courseId);
        String courseName = course != null ? course.getCourseName() : "";
        String className = course != null ? course.getClassName() : "";

        SignReminder reminder = new SignReminder();
        reminder.setCourseId(courseId);
        reminder.setCourseName(courseName);
        reminder.setClassName(className);
        reminder.setSignCode(signCode);
        reminder.setTeacherId(teacherId);
        reminder.setStatus("pending");
        reminder.setReminderType("sign");

        signReminderMapper.insert(reminder);

        Map<String, Object> data = new HashMap<>();
        data.put("id", reminder.getId());
        data.put("courseName", courseName);
        data.put("className", className);
        data.put("signCode", signCode);

        return Result.success("签到提醒已创建", data);
    }

    @GetMapping("/pending")
    public Result<List<SignReminder>> getPendingReminders(@RequestParam String studentId) {
        scheduleReminderService.generateUpcomingClassRemindersNow();
        List<SignReminder> reminders = signReminderMapper.selectPendingByStudentId(studentId);
        return Result.success("获取成功", reminders);
    }

    @PostMapping("/signCompleted")
    public Result<Void> signCompleted(@RequestBody Map<String, Object> params) {
        Integer id = RequestParams.getInteger(params, "id");

        if (id == null) {
            return Result.error("参数不完整");
        }

        signReminderMapper.updateStatusById(id, "signed");
        return Result.success("已标记为已签到");
    }

    @PostMapping("/dismiss")
    public Result<Void> dismissReminder(@RequestBody Map<String, Object> params) {
        Integer id = RequestParams.getInteger(params, "id");

        if (id == null) {
            return Result.error("参数不完整");
        }

        signReminderMapper.updateStatusById(id, "dismissed");
        return Result.success("已忽略提醒");
    }
}
