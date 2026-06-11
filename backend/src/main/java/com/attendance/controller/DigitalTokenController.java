package com.attendance.controller;

import com.attendance.common.RequestParams;
import com.attendance.common.Result;
import com.attendance.entity.DigitalToken;
import com.attendance.service.DigitalTokenService;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/digital")
public class DigitalTokenController {
    private final DigitalTokenService digitalTokenService;

    public DigitalTokenController(DigitalTokenService digitalTokenService) {
        this.digitalTokenService = digitalTokenService;
    }

    @PostMapping("/generate")
    public Result<Map<String, Object>> generateToken(@RequestBody Map<String, Object> params) {
        Integer courseId = RequestParams.getInteger(params, "courseId");
        String teacherId = RequestParams.getString(params, "teacherId");
        Integer expireSecondsParam = RequestParams.getInteger(params, "expireSeconds");
        int expireSeconds = expireSecondsParam != null ? expireSecondsParam : 60;

        if (courseId == null || teacherId == null) {
            return Result.error("参数不完整");
        }

        DigitalToken token = digitalTokenService.generateToken(courseId, teacherId, expireSeconds);
        
        Map<String, Object> data = new HashMap<>();
        data.put("id", token.getId());
        data.put("token", token.getToken());
        data.put("courseId", token.getCourseId());
        data.put("expireTime", token.getExpireTime());
        return Result.success("口令生成成功", data);
    }

    @GetMapping("/active")
    public Result<Map<String, Object>> getActiveToken(@RequestParam Integer courseId) {
        DigitalToken token = digitalTokenService.getActiveToken(courseId);
        if (token == null) {
            return Result.error("暂无有效口令");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("id", token.getId());
        data.put("token", token.getToken());
        data.put("courseId", token.getCourseId());
        data.put("expireTime", token.getExpireTime());
        return Result.success("获取成功", data);
    }

    @PostMapping("/signIn")
    public Result<Map<String, Object>> signInByToken(@RequestBody Map<String, Object> params) {
        String token = RequestParams.getString(params, "token");
        String studentId = RequestParams.getString(params, "studentId");

        if (token == null || studentId == null) {
            return Result.error("参数不完整");
        }

        DigitalTokenService.DigitalSignInResult result = digitalTokenService.signInByToken(token, studentId);
        
        if (result.success) {
            Map<String, Object> data = new HashMap<>();
            data.put("courseId", result.courseId);
            return Result.success(result.message, data);
        } else {
            Map<String, Object> data = new HashMap<>();
            if (result.courseId != null) {
                data.put("courseId", result.courseId);
            }
            return Result.error(result.message, data);
        }
    }

    @PostMapping("/expire")
    public Result<Void> expireToken(@RequestBody Map<String, Object> params) {
        Integer courseId = RequestParams.getInteger(params, "courseId");
        if (courseId == null) {
            return Result.error("课程ID不能为空");
        }
        digitalTokenService.expireToken(courseId);
        return Result.success("口令已失效");
    }
}
