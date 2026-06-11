package com.attendance.controller;

import com.attendance.common.RequestParams;
import com.attendance.common.Result;
import com.attendance.entity.Schedule;
import com.attendance.service.ScheduleService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/schedule")
public class ScheduleController {
    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @GetMapping("/list")
    public Result<List<Schedule>> getSchedules(@RequestParam String teacherId) {
        List<Schedule> schedules = scheduleService.getSchedulesByTeacherId(teacherId);
        return Result.success("获取成功", schedules);
    }

    @PostMapping("/add")
    public Result<Void> addSchedule(@RequestBody Schedule schedule) {
        boolean success = scheduleService.addSchedule(schedule);
        if (success) {
            return Result.success("添加成功");
        }
        return Result.error("添加失败");
    }

    @PostMapping("/update")
    public Result<Void> updateSchedule(@RequestBody Schedule schedule) {
        boolean success = scheduleService.updateSchedule(schedule);
        if (success) {
            return Result.success("更新成功");
        }
        return Result.error("更新失败");
    }

    @PostMapping("/delete")
    public Result<Void> deleteSchedule(@RequestBody Map<String, Object> params) {
        Integer id = RequestParams.getInteger(params, "id");
        if (id == null) {
            return Result.error("ID不能为空");
        }
        boolean success = scheduleService.deleteSchedule(id);
        if (success) {
            return Result.success("删除成功");
        }
        return Result.error("删除失败");
    }
}
