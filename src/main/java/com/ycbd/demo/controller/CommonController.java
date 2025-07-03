package com.ycbd.demo.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ycbd.demo.security.UserContext;
import com.ycbd.demo.service.BaseService;
import com.ycbd.demo.utils.ApiResponse;

@RestController
@RequestMapping("/api/common")
public class CommonController {

    @Autowired
    private BaseService baseService;

    @PostMapping("/save")
    public ApiResponse<Map<String, Object>> save(
            @RequestParam String targetTable,
            @RequestBody Map<String, Object> data) {
        try {
            // 安全检查：获取当前用户ID
            Integer userId = UserContext.getUserId();
            if (userId == null) {
                return ApiResponse.failed("未授权的操作");
            }

            // 简单检查ID是否存在，确定是创建还是更新操作
            Object id = data.get("id");
            Map<String, Object> result = new HashMap<>();

            if (id != null) {
                // 使用BaseService进行更新
                baseService.update(targetTable, data, id);
                result.put("id", id);
            } else {
                // 使用BaseService进行保存
                long newId = baseService.save(targetTable, data);
                result.put("id", newId);
            }

            return ApiResponse.success(result);
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
            // 安全检查：获取当前用户ID
            Integer userId = UserContext.getUserId();
            if (userId == null) {
                return ApiResponse.failed("未授权的操作");
            }

            // 移除targetTable参数，它不应该作为查询条件
            allParams.remove("targetTable");

            // 获取分页参数
            int pageIndex = 0;
            int pageSize = 100;
            if (allParams.containsKey("pageIndex")) {
                pageIndex = Integer.parseInt(allParams.get("pageIndex").toString());
                allParams.remove("pageIndex");
            }
            if (allParams.containsKey("pageSize")) {
                pageSize = Integer.parseInt(allParams.get("pageSize").toString());
                allParams.remove("pageSize");
            }
            // 过滤所有分页、排序、通用保留参数，避免进入WHERE
            String[] reservedKeys = {"orderBy", "sortByAndType", "columns", "offset", "limit"};
            for (String key : reservedKeys) {
                allParams.remove(key);
            }

            // 使用BaseService查询列表
            List<Map<String, Object>> items = baseService.queryList(
                    targetTable, pageIndex, pageSize, null, allParams, null, "id ASC", null);

            // 获取总数
            int total = baseService.count(targetTable, allParams, null);

            Map<String, Object> result = new HashMap<>();
            result.put("items", items);
            result.put("total", total);

            return ApiResponse.success(result);
        } catch (Exception e) {
            return ApiResponse.failed("查询失败: " + e.getMessage());
        }
    }

    @PostMapping("/delete")
    public ApiResponse<Object> delete(
            @RequestParam String targetTable,
            @RequestParam Integer id) {
        try {
            // 安全检查：获取当前用户ID
            Integer userId = UserContext.getUserId();
            if (userId == null) {
                return ApiResponse.failed("未授权的操作");
            }

            // 使用BaseService进行删除
            baseService.delete(targetTable, id);

            Map<String, Object> result = new HashMap<>();
            result.put("affected", 1);

            return ApiResponse.success(result);
        } catch (Exception e) {
            return ApiResponse.failed("删除失败: " + e.getMessage());
        }
    }

    /**
     * 健康检查端点
     */
    @GetMapping("/health")
    public ApiResponse<String> healthCheck() {
        return ApiResponse.success("Service is running");
    }
}
