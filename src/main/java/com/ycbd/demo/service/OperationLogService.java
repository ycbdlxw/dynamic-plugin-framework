package com.ycbd.demo.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 操作日志服务：根据配置将请求日志写入 system_log 表。
 */
@Service
public class OperationLogService {

    @Value("${app.logging.save-operation-log:false}")
    private boolean saveOperationLog;

    @Autowired
    private CommonService commonService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 记录操作日志
     *
     * @param request HTTP请求
     * @param userMap 当前用户信息，可能为null
     * @param response 响应对象，可能为null
     * @param duration 请求耗时（毫秒）
     * @param result 控制器返回值，可为null
     */
    public void log(HttpServletRequest request, Map<String, Object> userMap, HttpServletResponse response,
            long duration, Object result) {
        if (!saveOperationLog) {
            return;
        }
        try {
            Map<String, Object> data = new HashMap<>();
            if (userMap != null) {
                data.put("user_id", userMap.get("userId"));
                data.put("username", userMap.get("username"));
            }
            data.put("method", request.getMethod());
            data.put("request_uri", request.getRequestURI());
            data.put("client_ip", getClientIp(request));
            // 尝试序列化参数
            try {
                String paramsJson = objectMapper.writeValueAsString(request.getParameterMap());
                data.put("params", paramsJson);
            } catch (Exception ignore) {
            }
            if (response != null) {
                data.put("status", response.getStatus());
            }
            data.put("duration_ms", (int) duration);
            if (result != null) {
                String respStr = objectMapper.writeValueAsString(result);
                // 若长度过长，只截取前2000字符
                if (respStr.length() > 2000) {
                    respStr = respStr.substring(0, 2000) + "...";
                }
                data.put("response_body", respStr);
            }
            // 新增：记录当前时间戳
            data.put("created_at", System.currentTimeMillis());
            commonService.saveData("system_log", data);
        } catch (Exception e) {
            // 日志记录失败不能影响主流程
            e.printStackTrace();
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
