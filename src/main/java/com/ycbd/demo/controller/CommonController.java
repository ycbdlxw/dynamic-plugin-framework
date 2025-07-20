package com.ycbd.demo.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ycbd.demo.service.CommonService;
import com.ycbd.demo.utils.ApiResponse;

import cn.hutool.core.map.MapUtil;

@RestController
@RequestMapping("/api/common")
public class CommonController {

    private static final Logger logger = LoggerFactory.getLogger(CommonController.class);

    @Autowired
    private CommonService commonService;

    @PostMapping("/save")
    public ApiResponse<Map<String, Object>> save(
            @RequestParam String targetTable,
            @RequestBody Map<String, Object> data) {
        try {
            return  commonService.saveData(targetTable, data);
           
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.failed("保存失败: " + e.getMessage());
        }
    }

    @GetMapping("/list")
    public ApiResponse<Map<String, Object>> list(
            @RequestParam String targetTable,
            @RequestParam Map<String, Object> allParams) {
        try {
            // 添加日志输出
            logger.info("获取列表请求: targetTable={}, params={}", targetTable, allParams);
            return commonService.getList(targetTable, allParams);
        } catch (Exception e) {
            logger.error("查询失败", e);
            return ApiResponse.failed("查询失败: " + e.getMessage());
        }
    }

    @PostMapping("/list")
    public ApiResponse<Map<String, Object>> listPost(
            @RequestBody Map<String, Object> requestBody) {
        try {
           String targetTable=MapUtil.getStr(requestBody, "targetTable");
              return commonService.getList(targetTable, requestBody);
        } catch (Exception e) {
            logger.error("POST方式查询失败", e);
            return ApiResponse.failed("查询失败: " + e.getMessage());
        }
    }

    @PostMapping("/delete")
    public ApiResponse<Object> delete(
            @RequestParam String targetTable,
            @RequestParam Integer id) {
        try {
            
            return   commonService.deleteData(targetTable, id);
        } catch (Exception e) {
            return ApiResponse.failed("删除失败: " + e.getMessage());
        }
    }

    /**
     * 健康检查端点
     */
    @GetMapping("/health")
    @CrossOrigin
    public ApiResponse<String> healthCheck() {
        return ApiResponse.success("Service is running");
    }
}
