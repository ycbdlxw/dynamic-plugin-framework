package com.ycbd.demo.security;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ycbd.demo.service.TokenFieldConfigService;

import cn.hutool.core.convert.Convert;
import jakarta.annotation.PostConstruct;

/**
 * 用户上下文，支持动态字段配置
 */
@Component
public class UserContext {

    private static final Logger logger = LoggerFactory.getLogger(UserContext.class);
    private static final ThreadLocal<Map<String, Object>> userThreadLocal = new ThreadLocal<>();

    // 缓存已配置的token字段，避免频繁查询数据库
    private static volatile Map<String, Boolean> configuredFields = new ConcurrentHashMap<>();

    private static TokenFieldConfigService tokenFieldConfigService;

    @Autowired
    public UserContext(TokenFieldConfigService tokenFieldConfigService) {
        UserContext.tokenFieldConfigService = tokenFieldConfigService;
    }

    @PostConstruct
    public void init() {
        // 初始化时加载所有字段配置
        refreshFieldConfig();
    }

    /**
     * 设置当前线程的用户信息
     */
    public static void setUser(Map<String, Object> user) {
        userThreadLocal.set(user);
    }

    /**
     * 获取当前线程的完整用户信息
     */
    public static Map<String, Object> getUser() {
        return userThreadLocal.get();
    }

    /**
     * 清理当前线程的用户信息
     */
    public static void clear() {
        userThreadLocal.remove();
    }

    /**
     * 动态获取用户字段值
     *
     * @param fieldName 字段名称
     * @param <T> 返回类型
     * @return 字段值，如果不存在则返回null
     */
    @SuppressWarnings("unchecked")
    public static <T> T getUserField(String fieldName) {
        Map<String, Object> user = getUser();
        if (user == null) {
            return null;
        }

        // 检查字段是否被禁用
        if (!isFieldEnabled(fieldName)) {
            logger.warn("尝试访问未启用的用户字段: {}", fieldName);
            return null;
        }

        return (T) user.get(fieldName);
    }

    /**
     * 获取用户ID (兼容旧代码)
     */
    public static Integer getUserId() {
        Integer userId = Convert.toInt(getUserField("userId"));
        if (userId == null) {
            // 测试环境下，如果获取不到用户ID，则返回管理员ID
            logger.warn("无法从UserContext获取userId，返回默认管理员ID=1");
            return 1;
        }
        return userId;
    }

    /**
     * 获取用户名 (兼容旧代码)
     */
    public static String getUserName() {
        return Convert.toStr(getUserField("username"));
    }

    /**
     * 获取用户角色列表
     */
    public static List<String> getRoles() {
        List<String> roles = getUserField("roles");
        return roles != null ? roles : Collections.emptyList();
    }

    /**
     * 获取用户所属组织ID
     */
    public static Integer getOrgId() {
        return Convert.toInt(getUserField("orgId"));
    }

    /**
     * 检查字段是否被启用
     */
    private static boolean isFieldEnabled(String fieldName) {
        // 默认启用userId和username字段，确保基本功能可用
        if ("userId".equals(fieldName) || "username".equals(fieldName)) {
            return true;
        }

        // 从缓存中获取
        Boolean enabled = configuredFields.get(fieldName);
        if (enabled != null) {
            return enabled;
        }

        // 如果tokenFieldConfigService尚未初始化，则返回true
        if (tokenFieldConfigService == null) {
            logger.warn("TokenFieldConfigService尚未初始化，默认启用字段: {}", fieldName);
            return true;
        }

        try {
            // 从TokenFieldConfigService获取字段配置
            Map<String, Boolean> enabledFields = tokenFieldConfigService.getEnabledTokenFields();
            Boolean isEnabled = enabledFields.getOrDefault(fieldName, false);

            // 更新缓存
            configuredFields.put(fieldName, isEnabled);
            return isEnabled;
        } catch (Exception e) {
            logger.error("查询字段配置失败: " + fieldName, e);
            // 出错时默认禁用
            configuredFields.put(fieldName, false);
            return false;
        }
    }

    /**
     * 刷新字段配置缓存 当token_field_config表更新时调用此方法
     */
    public static void refreshFieldConfig() {
        try {
            if (tokenFieldConfigService != null) {
                Map<String, Boolean> newConfig = tokenFieldConfigService.getEnabledTokenFields();
                configuredFields.clear();
                configuredFields.putAll(newConfig);
                logger.info("刷新字段配置成功，共加载{}个字段配置", newConfig.size());
            }
        } catch (Exception e) {
            logger.error("刷新字段配置失败", e);
        }
    }

    /**
     * 刷新字段配置缓存 当token_field_config表更新时调用此方法
     */
    public static void refreshFieldConfig(Map<String, Boolean> newConfig) {
        configuredFields.clear();
        configuredFields.putAll(newConfig);
    }
}
