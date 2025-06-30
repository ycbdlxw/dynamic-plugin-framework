package com.ycbd.demo.security;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.hutool.core.convert.Convert;

/**
 * 用户上下文，支持动态字段配置
 */
public class UserContext {

    private static final Logger logger = LoggerFactory.getLogger(UserContext.class);
    private static final ThreadLocal<Map<String, Object>> userThreadLocal = new ThreadLocal<>();

    // 缓存已配置的token字段，避免频繁查询数据库
    private static volatile Map<String, Boolean> configuredFields = new ConcurrentHashMap<>();

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
        return Convert.toInt(getUserField("userId"));
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
     * 检查字段是否被启用 注意：此方法应该从数据库中查询token_field_config表 为了避免频繁查询数据库，这里使用了内存缓存
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

        // 实际项目中，这里应该查询数据库
        // 为了演示，我们假设所有字段都是启用的
        configuredFields.put(fieldName, true);
        return true;
    }

    /**
     * 刷新字段配置缓存 当token_field_config表更新时调用此方法
     */
    public static void refreshFieldConfig(Map<String, Boolean> newConfig) {
        configuredFields.clear();
        configuredFields.putAll(newConfig);
    }
}
