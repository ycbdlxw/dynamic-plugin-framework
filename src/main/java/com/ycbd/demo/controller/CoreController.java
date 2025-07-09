package com.ycbd.demo.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ycbd.demo.service.CommonService;
import com.ycbd.demo.utils.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/core")
@Tag(name = "核心接口", description = "用户注册与登录")
public class CoreController {

    @Autowired
    private CommonService commonService;

    @PostMapping("/register")
    @Operation(summary = "用户注册")
    public ApiResponse<Map<String, Object>> register(@RequestBody Map<String, Object> userData) {
        return commonService.saveData("sys_user", userData);
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public ApiResponse<Map<String, Object>> login(@RequestBody Map<String, Object> credentials) {
        return commonService.login(credentials);
    }
}
