package com.ycbd.demo.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ycbd.demo.utils.ApiResponse;

@RestController
@RequestMapping("/api/common")
public class CommonController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostMapping("/save")
    public ApiResponse<Map<String, Object>> save(
            @RequestParam String targetTable,
            @RequestBody Map<String, Object> data) {
        try {
            // 简单检查ID是否存在，确定是创建还是更新操作
            Object id = data.get("id");
            Map<String, Object> result = new HashMap<>();

            if (id != null) {
                // 构建更新SQL
                StringBuilder sql = new StringBuilder("UPDATE ").append(targetTable).append(" SET ");
                List<String> setClauses = new ArrayList<>();

                for (Map.Entry<String, Object> entry : data.entrySet()) {
                    if (!"id".equals(entry.getKey())) {
                        setClauses.add(entry.getKey() + " = ?");
                    }
                }

                sql.append(String.join(", ", setClauses));
                sql.append(" WHERE id = ?");

                // 准备参数
                List<Object> params = new ArrayList<>();
                for (Map.Entry<String, Object> entry : data.entrySet()) {
                    if (!"id".equals(entry.getKey())) {
                        params.add(entry.getValue());
                    }
                }
                params.add(id);

                // 执行更新
                jdbcTemplate.update(sql.toString(), params.toArray());
                result.put("id", id);
            } else {
                // 构建插入SQL
                StringBuilder sql = new StringBuilder("INSERT INTO ").append(targetTable).append(" (");

                // 字段列表
                List<String> columns = new ArrayList<>(data.keySet());
                sql.append(String.join(", ", columns));

                // 值占位符
                sql.append(") VALUES (");
                List<String> placeholders = new ArrayList<>();
                for (int i = 0; i < columns.size(); i++) {
                    placeholders.add("?");
                }
                sql.append(String.join(", ", placeholders));
                sql.append(")");

                System.out.println("SQL: " + sql);
                System.out.println("Params: " + data.values());

                // 执行插入
                jdbcTemplate.update(sql.toString(), data.values().toArray());

                // 获取自增ID
                Long newId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
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
            // 移除targetTable参数，它不应该作为查询条件
            allParams.remove("targetTable");

            // 构建简单的查询条件
            StringBuilder whereClause = new StringBuilder();
            List<Object> params = new ArrayList<>();

            if (!allParams.isEmpty()) {
                whereClause.append(" WHERE ");
                List<String> conditions = new ArrayList<>();

                for (Map.Entry<String, Object> entry : allParams.entrySet()) {
                    conditions.add(entry.getKey() + " = ?");
                    params.add(entry.getValue());
                }

                whereClause.append(String.join(" AND ", conditions));
            }

            // 执行查询
            String sql = "SELECT * FROM " + targetTable + whereClause.toString() + " ORDER BY id ASC LIMIT 100";
            List<Map<String, Object>> items = jdbcTemplate.queryForList(sql, params.toArray());

            // 获取总数
            String countSql = "SELECT COUNT(*) FROM " + targetTable + whereClause.toString();
            int total = jdbcTemplate.queryForObject(countSql, params.toArray(), Integer.class);

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
            String sql = "DELETE FROM " + targetTable + " WHERE id = ?";
            int affected = jdbcTemplate.update(sql, id);

            Map<String, Object> result = new HashMap<>();
            result.put("affected", affected);

            return ApiResponse.success(result);
        } catch (Exception e) {
            return ApiResponse.failed("删除失败: " + e.getMessage());
        }
    }
}
