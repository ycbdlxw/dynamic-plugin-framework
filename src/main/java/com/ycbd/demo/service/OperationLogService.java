package com.ycbd.demo.service;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ycbd.demo.utils.Tools;

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
    private static final Logger log = LoggerFactory.getLogger(OperationLogService.class);

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
                Object userId = userMap.get("userId");
                // 确保userId为整数类型
                if (userId != null) {
                    try {
                        if (userId instanceof String) {
                            // 如果是字符串，尝试转换为整数
                            data.put("user_id", Integer.parseInt((String) userId));
                        } else {
                            data.put("user_id", userId);
                        }
                    } catch (NumberFormatException e) {
                        // 转换失败时设置为null
                        data.put("user_id", null);
                    }
                } else {
                    data.put("user_id", null);
                }
                data.put("username", userMap.get("username"));
            } else {
                // 当userMap为null时，明确设置为null
                data.put("user_id", -1);
                data.put("username", "系统用户");
            }
            data.put("method", request.getMethod());
            data.put("request_uri", request.getRequestURI());
            data.put("client_ip", Tools.getIpAddr((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()));
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
            commonService.saveData("system_log", data);
        } catch (Exception e) {
            // 日志记录失败不能影响主流程
            e.printStackTrace();
            log.error("记录操作日志失败: {}", e.getMessage());
        }
    }
}
