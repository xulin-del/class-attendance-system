package com.attendance.controller;

import com.attendance.common.RequestParams;
import com.attendance.common.Result;
import com.attendance.entity.AttendanceApplication;
import com.attendance.service.ApplicationService;
import org.springframework.web.bind.annotation.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/application")
public class ApplicationController {
    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping("/submit")
    public Result<Void> submitApplication(@RequestBody Map<String, Object> params) {
        Integer recordId = RequestParams.getInteger(params, "recordId");
        Integer courseId = RequestParams.getInteger(params, "courseId");
        String studentId = RequestParams.getString(params, "studentId");
        String courseName = RequestParams.getString(params, "courseName");
        String absenceDateStr = RequestParams.getString(params, "absenceDate");
        String reason = RequestParams.getString(params, "reason");
        String images = RequestParams.getString(params, "images");

        if (recordId == null || courseId == null || studentId == null) {
            return Result.error("参数不完整");
        }

        java.util.Date absenceDate = null;
        if (absenceDateStr != null && !absenceDateStr.isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                absenceDate = sdf.parse(absenceDateStr);
            } catch (ParseException e) {
                absenceDate = new java.util.Date();
            }
        } else {
            absenceDate = new java.util.Date();
        }

        ApplicationService.ApplicationResult result = applicationService.submitApplication(
            recordId, courseId, studentId, courseName, absenceDate, reason, images
        );

        if (result.success) {
            return Result.success(result.message);
        } else {
            return Result.error(result.message);
        }
    }

    @GetMapping("/pending")
    public Result<List<AttendanceApplication>> getPendingApplications(@RequestParam String teacherId) {
        List<AttendanceApplication> applications = applicationService.getPendingApplications(teacherId);
        return Result.success("获取成功", applications);
    }

    @GetMapping("/pendingCount")
    public Result<Map<String, Object>> getPendingCount(@RequestParam String teacherId) {
        int count = applicationService.countPendingApplications(teacherId);
        Map<String, Object> data = new HashMap<>();
        data.put("count", count);
        return Result.success("获取成功", data);
    }

    @PostMapping("/approve")
    public Result<Void> approveApplication(@RequestBody Map<String, Object> params) {
        Integer applicationId = RequestParams.getInteger(params, "applicationId");

        if (applicationId == null) {
            return Result.error("申请ID不能为空");
        }

        ApplicationService.ApplicationResult result = applicationService.approveApplication(applicationId);

        if (result.success) {
            return Result.success(result.message);
        } else {
            return Result.error(result.message);
        }
    }

    @PostMapping("/reject")
    public Result<Void> rejectApplication(@RequestBody Map<String, Object> params) {
        Integer applicationId = RequestParams.getInteger(params, "applicationId");
        String rejectReason = RequestParams.getString(params, "rejectReason");

        if (applicationId == null) {
            return Result.error("申请ID不能为空");
        }

        ApplicationService.ApplicationResult result = applicationService.rejectApplication(applicationId, rejectReason);

        if (result.success) {
            return Result.success(result.message);
        } else {
            return Result.error(result.message);
        }
    }

    @GetMapping("/checkRecord")
    public Result<Map<String, Object>> checkRecordApplication(@RequestParam Integer recordId) {
        AttendanceApplication app = applicationService.getApplicationByRecordId(recordId);
        Map<String, Object> data = new HashMap<>();
        if (app != null) {
            data.put("exists", true);
            data.put("status", app.getStatus());
        } else {
            data.put("exists", false);
        }
        return Result.success("获取成功", data);
    }

    @GetMapping("/studentList")
    public Result<List<AttendanceApplication>> getStudentApplications(@RequestParam String studentId) {
        List<AttendanceApplication> applications = applicationService.getApplicationsByStudentId(studentId);
        return Result.success("获取成功", applications);
    }
}
