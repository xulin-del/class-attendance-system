package com.attendance.controller;

import com.attendance.common.RequestParams;
import com.attendance.common.Result;
import com.attendance.entity.Course;
import com.attendance.service.CourseService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/course")
public class CourseController {
    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }
    
    @GetMapping("/list")
    public Result<List<Course>> getCoursesByTeacherId(@RequestParam String teacherId) {
        List<Course> courses = courseService.getCoursesByTeacherId(teacherId);
        return Result.success("获取成功", courses);
    }
    
    @GetMapping("/listByClass")
    public Result<List<Course>> getCoursesByClassName(@RequestParam String className) {
        List<Course> courses = courseService.getCoursesByClassName(className);
        return Result.success("获取成功", courses);
    }
    
    @GetMapping("/detail")
    public Result<Course> getCourseById(@RequestParam Integer courseId) {
        Course course = courseService.getCourseById(courseId);
        if (course != null) {
            return Result.success("获取成功", course);
        } else {
            return Result.error("课程不存在");
        }
    }
    
    @PostMapping("/add")
    public Result<Void> addCourse(@RequestBody Course course) {
        boolean success = courseService.addCourse(course);
        if (success) {
            return Result.success("添加成功");
        } else {
            return Result.error("添加失败");
        }
    }
    
    @PostMapping("/update")
    public Result<Void> updateCourse(@RequestBody Course course) {
        boolean success = courseService.updateCourse(course);
        if (success) {
            return Result.success("更新成功");
        } else {
            return Result.error("更新失败");
        }
    }
    
    @PostMapping("/delete")
    public Result<Void> deleteCourse(@RequestBody Map<String, Object> params) {
        Integer courseId = RequestParams.getInteger(params, "courseId");
        if (courseId == null) {
            return Result.error("课程ID不能为空");
        }
        boolean success = courseService.deleteCourse(courseId);
        if (success) {
            return Result.success("删除成功");
        } else {
            return Result.error("删除失败");
        }
    }
}
