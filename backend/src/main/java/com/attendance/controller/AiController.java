package com.attendance.controller;

import com.attendance.common.Result;
import com.attendance.dto.AiQueryRequest;
import com.attendance.entity.AttendanceRecord;
import com.attendance.entity.Course;
import com.attendance.entity.Student;
import com.attendance.entity.Teacher;
import com.attendance.mapper.AttendanceRecordMapper;
import com.attendance.mapper.CourseMapper;
import com.attendance.mapper.StudentMapper;
import com.attendance.mapper.TeacherMapper;
import com.attendance.service.DeepSeekService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.*;

@RestController
@RequestMapping("/api/attendance/ai")
public class AiController {
    private static final Logger log = LoggerFactory.getLogger(AiController.class);

    @Autowired
    private DeepSeekService deepSeekService;
    @Autowired
    private AttendanceRecordMapper attendanceRecordMapper;
    @Autowired
    private CourseMapper courseMapper;
    @Autowired
    private StudentMapper studentMapper;
    @Autowired
    private TeacherMapper teacherMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/query")
    public Result<String> query(@RequestBody AiQueryRequest request) {
        String question = request.getQuestion();
        if (question == null || question.trim().isEmpty()) {
            return Result.error("问题不能为空");
        }

        try {
            // 收集数据库上下文信息
            String dbContext = buildDatabaseContext();

            // 构建系统提示词
            String systemPrompt = "你是课堂考勤系统的智能助手。你可以回答用户关于考勤的任何问题，也可以闲聊。\n\n" +
                    "以下是当前数据库中的真实数据，请基于这些数据回答问题：\n" +
                    dbContext + "\n\n" +
                    "回答要求：\n" +
                    "1. 如果问题与考勤数据相关，请根据上面的数据给出准确回答\n" +
                    "2. 如果数据中没有相关信息，如实告知\n" +
                    "3. 如果问题与考勤无关（如闲聊、常识问题），正常回答即可\n" +
                    "4. 用中文回答，简洁友好";

            // 一次调用DeepSeek，让它自由回答
            String answer = deepSeekService.chat(systemPrompt, question);
            if (answer == null) {
                return Result.error("AI服务暂时不可用，请稍后重试");
            }

            return Result.success("查询成功", answer);
        } catch (Exception e) {
            log.error("AI查询失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 收集数据库中的上下文信息，作为AI回答的数据依据
     */
    private String buildDatabaseContext() {
        StringBuilder sb = new StringBuilder();

        try {
            // 学生信息
            List<Student> allStudents = new ArrayList<>();
            // 通过已知班级查询学生
            List<Course> courses = courseMapper.selectList(null);
            Set<String> classNames = new LinkedHashSet<>();
            for (Course course : courses) {
                if (course.getClassName() != null && !course.getClassName().isEmpty()) {
                    classNames.add(course.getClassName());
                }
            }
            for (String className : classNames) {
                List<Student> students = studentMapper.selectByClassName(className);
                allStudents.addAll(students);
            }

            sb.append("【学生信息】共").append(allStudents.size()).append("名学生：\n");
            for (Student s : allStudents) {
                sb.append("  - ").append(s.getStudentName())
                  .append("(学号:").append(s.getStudentId())
                  .append(", 班级:").append(s.getClassName()).append(")\n");
            }

            // 课程信息
            sb.append("\n【课程信息】共").append(courses.size()).append("门课程：\n");
            for (Course c : courses) {
                sb.append("  - ").append(c.getCourseName())
                  .append("(课程ID:").append(c.getCourseId())
                  .append(", 教师:").append(c.getTeacherId())
                  .append(", 班级:").append(c.getClassName()).append(")\n");
            }

            // 考勤记录
            List<AttendanceRecord> records = attendanceRecordMapper.selectList(null);
            sb.append("\n【考勤记录】共").append(records.size()).append("条记录：\n");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            for (AttendanceRecord r : records) {
                sb.append("  - 课程ID:").append(r.getCourseId())
                  .append(", 学号:").append(r.getStudentId())
                  .append(", 状态:").append(r.getStatus());
                if (r.getSignTime() != null) {
                    sb.append(", 签到时间:").append(sdf.format(r.getSignTime()));
                }
                sb.append("\n");
            }

            // 统计汇总
            int total = records.size();
            int signed = 0, absent = 0, unsigned = 0;
            for (AttendanceRecord r : records) {
                if ("已签到".equals(r.getStatus())) signed++;
                else if ("缺勤".equals(r.getStatus())) absent++;
                else if ("未签到".equals(r.getStatus())) unsigned++;
            }
            sb.append("\n【考勤统计汇总】\n");
            sb.append("  总记录: ").append(total).append("\n");
            sb.append("  已签到: ").append(signed).append("\n");
            sb.append("  缺勤: ").append(absent).append("\n");
            sb.append("  未签到: ").append(unsigned).append("\n");
            if (total > 0) {
                sb.append("  签到率: ").append(Math.round(signed * 100.0 / total * 10) / 10.0).append("%\n");
            }

        } catch (Exception e) {
            log.error("构建数据库上下文失败", e);
            sb.append("数据库读取异常");
        }

        return sb.toString();
    }
}
