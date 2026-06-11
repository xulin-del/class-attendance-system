package com.attendance.controller;

import com.attendance.common.RequestParams;
import com.attendance.common.Result;
import com.attendance.entity.AttendanceRecord;
import com.attendance.entity.LocationSign;
import com.attendance.service.AttendanceService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {
    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @PostMapping("/start")
    public Result<Map<String, Object>> startAttendance(@RequestBody Map<String, Object> params) {
        Integer courseId = RequestParams.getInteger(params, "courseId");
        String className = RequestParams.getString(params, "className");

        if (courseId == null || className == null) {
            return Result.error("参数不完整");
        }

        AttendanceService.StartResult startResult = attendanceService.startAttendance(courseId, className);
        Map<String, Object> data = new HashMap<>();
        data.put("signCode", startResult.signCode);
        data.put("courseId", courseId);
        return Result.success("签到已发起", data);
    }

    @PostMapping("/signIn")
    public Result<Void> signIn(@RequestBody Map<String, Object> params) {
        Integer courseId = RequestParams.getInteger(params, "courseId");
        String studentId = RequestParams.getString(params, "studentId");

        if (courseId == null || studentId == null) {
            return Result.error("参数不完整");
        }

        boolean success = attendanceService.signIn(courseId, studentId);
        if (success) {
            return Result.success("签到成功");
        } else {
            return Result.error("签到失败，可能已签到或未发起签到");
        }
    }

    @PostMapping("/signInByCode")
    public Result<Map<String, Object>> signInByCode(@RequestBody Map<String, Object> params) {
        String signCode = RequestParams.getString(params, "signCode");
        String studentId = RequestParams.getString(params, "studentId");

        if (signCode == null || studentId == null) {
            return Result.error("参数不完整");
        }

        Integer courseId = attendanceService.getCourseIdBySignCode(signCode);
        if (courseId == null) {
            return Result.error("签到码无效或已过期，请确认签到码是否正确或签到是否仍在进行中");
        }

        boolean success = attendanceService.signInByCode(signCode, studentId);
        if (success) {
            Map<String, Object> data = new HashMap<>();
            data.put("courseId", courseId);
            return Result.success("签到成功", data);
        } else {
            return Result.error("签到失败，可能已签到或不在签到名单中");
        }
    }

    @GetMapping("/signInfo")
    public Result<Map<String, Object>> getSignInfo(@RequestParam String signCode) {
        Integer courseId = attendanceService.getCourseIdBySignCode(signCode);
        if (courseId == null) {
            return Result.error("签到码无效或已过期");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("signCode", signCode);
        data.put("courseId", courseId);
        data.put("active", true);
        return Result.success("签到进行中", data);
    }

    @PostMapping("/end")
    public Result<Void> endAttendance(@RequestBody Map<String, Object> params) {
        Integer courseId = RequestParams.getInteger(params, "courseId");

        if (courseId == null) {
            return Result.error("课程ID不能为空");
        }

        attendanceService.endAttendance(courseId);
        return Result.success("签到已结束，未签到学生已标记为缺勤");
    }

    @GetMapping("/records")
    public Result<List<AttendanceRecord>> getAttendanceRecords(@RequestParam Integer courseId) {
        List<AttendanceRecord> records = attendanceService.getAttendanceRecords(courseId);
        return Result.success("获取成功", records);
    }

    @GetMapping("/studentRecords")
    public Result<List<AttendanceRecord>> getStudentAttendance(@RequestParam String studentId) {
        List<AttendanceRecord> records = attendanceService.getStudentAttendance(studentId);
        return Result.success("获取成功", records);
    }

    @GetMapping("/statistics")
    public Result<Map<String, Integer>> getStatistics(@RequestParam Integer courseId) {
        List<AttendanceRecord> records = attendanceService.getAttendanceRecords(courseId);
        int signed = 0, unsigned = 0, absent = 0;
        for (AttendanceRecord record : records) {
            if ("已签到".equals(record.getStatus())) {
                signed++;
            } else if ("未签到".equals(record.getStatus())) {
                unsigned++;
            } else if ("缺勤".equals(record.getStatus())) {
                absent++;
            }
        }
        Map<String, Integer> stats = new HashMap<>();
        stats.put("signed", signed);
        stats.put("unsigned", unsigned);
        stats.put("absent", absent);
        stats.put("total", records.size());
        return Result.success("获取成功", stats);
    }

    @PostMapping("/startLocation")
    public Result<Map<String, Object>> startLocationAttendance(@RequestBody Map<String, Object> params) {
        Integer courseId = RequestParams.getInteger(params, "courseId");
        String className = RequestParams.getString(params, "className");
        Double latitude = RequestParams.getDouble(params, "latitude");
        Double longitude = RequestParams.getDouble(params, "longitude");
        Integer radius = RequestParams.getInteger(params, "radius");
        if (radius == null) {
            radius = 100;
        }

        if (courseId == null || className == null || latitude == null || longitude == null) {
            return Result.error("参数不完整");
        }

        AttendanceService.LocationSignResult locationResult = attendanceService.startLocationAttendance(courseId, className, latitude, longitude, radius);
        Map<String, Object> data = new HashMap<>();
        data.put("signCode", locationResult.signCode);
        data.put("courseId", courseId);
        data.put("latitude", locationResult.latitude);
        data.put("longitude", locationResult.longitude);
        data.put("radius", locationResult.radius);
        return Result.success("定位签到已发起", data);
    }

    @PostMapping("/signInByLocation")
    public Result<Map<String, Object>> signInByLocation(@RequestBody Map<String, Object> params) {
        Integer courseId = RequestParams.getInteger(params, "courseId");
        String studentId = RequestParams.getString(params, "studentId");
        Double latitude = RequestParams.getDouble(params, "latitude");
        Double longitude = RequestParams.getDouble(params, "longitude");

        if (courseId == null || studentId == null || latitude == null || longitude == null) {
            return Result.error("参数不完整");
        }

        AttendanceService.LocationSignInResult signInResult = attendanceService.signInByLocation(courseId, studentId, latitude, longitude);
        Map<String, Object> data = new HashMap<>();
        data.put("distance", signInResult.distance);

        if (signInResult.success) {
            return Result.success(signInResult.message, data);
        } else {
            return Result.error(signInResult.message, data);
        }
    }

    @GetMapping("/activeLocationSign")
    public Result<Map<String, Object>> getActiveLocationSign(@RequestParam Integer courseId) {
        LocationSign locationSign = attendanceService.getActiveLocationSign(courseId);
        if (locationSign == null) {
            return Result.error("该课程暂无进行中的定位签到");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("id", locationSign.getId());
        data.put("courseId", locationSign.getCourseId());
        data.put("signCode", locationSign.getSignCode());
        data.put("latitude", locationSign.getLatitude());
        data.put("longitude", locationSign.getLongitude());
        data.put("radius", locationSign.getRadius());
        data.put("status", locationSign.getStatus());
        data.put("createTime", locationSign.getCreateTime());
        return Result.success("获取成功", data);
    }

    @GetMapping("/weeklyStats")
    public Result<List<AttendanceService.DailyAttendanceStats>> getWeeklyStats(@RequestParam String teacherId) {
        List<AttendanceService.DailyAttendanceStats> stats = attendanceService.getWeeklyAttendanceStats(teacherId);
        return Result.success("获取成功", stats);
    }

    @GetMapping("/historyByDate")
    public Result<List<Map<String, Object>>> getHistoryByDate(@RequestParam String teacherId, @RequestParam String date) {
        List<Map<String, Object>> records = attendanceService.getRecordsByTeacherIdAndDate(teacherId, date);
        return Result.success("获取成功", records);
    }

    @GetMapping("/studentStats")
    public Result<Map<String, Object>> getStudentStats(@RequestParam String studentId) {
        List<AttendanceRecord> records = attendanceService.getStudentAttendance(studentId);
        int signed = 0, late = 0, absent = 0;
        for (AttendanceRecord record : records) {
            if ("已签到".equals(record.getStatus())) signed++;
            else if ("迟到".equals(record.getStatus())) late++;
            else if ("缺勤".equals(record.getStatus())) absent++;
        }
        int total = records.size();
        double rate = total > 0 ? (signed * 100.0 / total) : 0;
        
        Map<String, Object> data = new HashMap<>();
        data.put("total", total);
        data.put("signed", signed);
        data.put("late", late);
        data.put("absent", absent);
        data.put("rate", Math.round(rate * 10) / 10.0);
        return Result.success("获取成功", data);
    }

    @GetMapping("/studentRecordsPage")
    public Result<Map<String, Object>> getStudentRecordsPage(
            @RequestParam String studentId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        List<Map<String, Object>> records = attendanceService.getStudentRecordsWithCourse(studentId, page, pageSize);
        Integer total = attendanceService.countStudentRecords(studentId);
        
        Map<String, Object> data = new HashMap<>();
        data.put("records", records);
        data.put("total", total);
        data.put("page", page);
        data.put("pageSize", pageSize);
        return Result.success("获取成功", data);
    }

    @GetMapping("/weeklyDayStats")
    public Result<List<AttendanceService.WeeklyDayStats>> getWeeklyDayStats(@RequestParam String teacherId) {
        List<AttendanceService.WeeklyDayStats> stats = attendanceService.getWeeklyDayStats(teacherId);
        return Result.success("获取成功", stats);
    }

    @GetMapping("/teacherStats")
    public Result<Map<String, Object>> getTeacherStats(@RequestParam String teacherId) {
        Map<String, Object> stats = attendanceService.getTeacherOverallStats(teacherId);
        return Result.success("获取成功", stats);
    }

    @GetMapping("/teacherDashboard")
    public Result<Map<String, Object>> getTeacherDashboard(@RequestParam String teacherId) {
        Map<String, Object> stats = attendanceService.getTeacherDashboardStats(teacherId);
        return Result.success("获取成功", stats);
    }
}
