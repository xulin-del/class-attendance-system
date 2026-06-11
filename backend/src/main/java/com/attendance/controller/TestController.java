package com.attendance.controller;

import com.attendance.common.Result;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test")
public class TestController {
    
    @GetMapping("/ping")
    public Result<String> ping() {
        return Result.success("pong", "server is running");
    }
    
    @PostMapping("/echo")
    public Result<Object> echo(@RequestBody Object data) {
        return Result.success("received", data);
    }
}