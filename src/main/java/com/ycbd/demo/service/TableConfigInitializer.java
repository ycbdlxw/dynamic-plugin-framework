package com.ycbd.demo.service;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 表配置初始化器，用于在应用启动时初始化必要的表配置
 */
@Component
public class TableConfigInitializer {

    private static final Logger logger = LoggerFactory.getLogger(TableConfigInitializer.class);
    private static final String TOKEN_FIELD_FLAG = "isTokenField";
    private static final String TOKEN_FIELD_NAME = "tokenName";

    @Autowired
    private BaseService baseService;

    @Autowired
    private TokenFieldConfigService tokenFieldConfigService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @EventListener(ApplicationReadyEvent.class)
    public void initialize() {
        try {
            logger.info("开始初始化表配置...");
            initTokenFieldConfig();
            initSecurityConfig();
            initPluginConfig();
            logger.info("表配置初始化完成");
        } catch (Exception e) {
            logger.error("表配置初始化失败", e);
        }
    }

    /**
     * 初始化token字段配置
     */
    private void initTokenFieldConfig() {
        try {
            logger.info("初始化token字段配置");
            // 检查column_attribute表是否存在
            if (!isTableExists("column_attribute")) {
                logger.warn("COLUMN_ATTRIBUTE表不存在，跳过初始化");
                return;
            }

            // 确保基本字段存在
            ensureTokenFieldExists("sys_user", "id", "userId", "用户ID");
            ensureTokenFieldExists("sys_user", "username", "username", "用户名");
            ensureTokenFieldExists("sys_user", "real_name", "real_name", "真实姓名");
            ensureTokenFieldExists("sys_user", "org_id", "orgId", "组织ID");

            // 刷新token字段配置
            tokenFieldConfigService.refreshTokenFieldConfig();
        } catch (Exception e) {
            logger.error("初始化token字段配置失败", e);
        }
    }

    /**
     * 确保token字段存在
     */
    private void ensureTokenFieldExists(String tableName, String columnName, String tokenName, String description) {
        try {
            // 查询列属性
            Map<String, Object> params = new HashMap<>();
            params.put("db_table_name", tableName);
            params.put("column_name", columnName);

            Map<String, Object> attr = baseService.getOne("column_attribute", params);
            if (attr == null) {
                logger.warn("未找到列属性: {}.{}, 跳过token字段配置", tableName, columnName);
                return;
            }

            // 解析other_info字段
            String otherInfo = (String) attr.get("other_info");
            Map<String, Object> otherMap;
            if (otherInfo == null || otherInfo.isEmpty()) {
                otherMap = new HashMap<>();
            } else {
                try {
                    otherMap = objectMapper.readValue(otherInfo, Map.class);
                } catch (Exception e) {
                    logger.warn("解析other_info字段JSON失败: {}, 创建新的配置", otherInfo);
                    otherMap = new HashMap<>();
                }
            }

            // 设置token字段标识
            boolean updated = false;
            if (!Boolean.TRUE.equals(otherMap.get(TOKEN_FIELD_FLAG))) {
                otherMap.put(TOKEN_FIELD_FLAG, true);
                updated = true;
            }

            if (!tokenName.equals(columnName) && !tokenName.equals(otherMap.get(TOKEN_FIELD_NAME))) {
                otherMap.put(TOKEN_FIELD_NAME, tokenName);
                updated = true;
            }

            if (updated) {
                // 更新列属性
                String newOtherInfo = objectMapper.writeValueAsString(otherMap);
                Map<String, Object> updateData = new HashMap<>();
                updateData.put("id", attr.get("id"));
                updateData.put("other_info", newOtherInfo);

                baseService.update("column_attribute", updateData, attr.get("id"));
                logger.info("更新token字段配置: {}.{} -> {}", tableName, columnName, tokenName);
            } else {
                logger.info("token字段配置已存在: {}.{} -> {}", tableName, columnName, tokenName);
            }
        } catch (Exception e) {
            logger.error("确保token字段存在失败: " + tableName + "." + columnName, e);
        }
    }

    /**
     * 初始化安全配置
     */
    private void initSecurityConfig() {
        try {
            logger.info("初始化安全配置");
            // 检查security_config表是否存在
            if (!isTableExists("security_config")) {
                logger.warn("SECURITY_CONFIG表不存在，跳过初始化");
                return;
            }

            // 确保基本白名单配置存在
            ensureSecurityConfigExists("WHITELIST", "/api/core/register", true);
            ensureSecurityConfigExists("WHITELIST", "/api/core/login", true);
            ensureSecurityConfigExists("WHITELIST", "/h2-console/**", true);
            ensureSecurityConfigExists("WHITELIST", "/swagger-ui.html", true);
            ensureSecurityConfigExists("WHITELIST", "/swagger-ui/**", true);
            ensureSecurityConfigExists("WHITELIST", "/api-docs/**", true);
            ensureSecurityConfigExists("WHITELIST", "/api/common/health", true);
        } catch (Exception e) {
            logger.error("初始化安全配置失败", e);
        }
    }

    /**
     * 确保安全配置存在
     */
    private void ensureSecurityConfigExists(String type, String pattern, boolean isActive) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("type", type);
            params.put("pattern", pattern);

            Map<String, Object> existingConfig = baseService.getOne("security_config", params);
            if (existingConfig == null) {
                logger.info("添加安全配置: {} - {}", type, pattern);
                Map<String, Object> newConfig = new HashMap<>();
                newConfig.put("type", type);
                newConfig.put("pattern", pattern);
                newConfig.put("is_active", isActive);
                baseService.save("security_config", newConfig);
            } else {
                logger.info("安全配置已存在: {} - {}", type, pattern);
            }
        } catch (Exception e) {
            logger.error("确保安全配置存在失败: " + type + " - " + pattern, e);
        }
    }

    /**
     * 初始化插件配置
     */
    private void initPluginConfig() {
        try {
            logger.info("初始化插件配置");
            // 检查plugin_config表是否存在
            if (!isTableExists("plugin_config")) {
                logger.warn("PLUGIN_CONFIG表不存在，跳过初始化");
                return;
            }

            // 确保内置插件配置存在
            ensurePluginConfigExists("TestService", "com.ycbd.demo.plugin.TestServicePlugin", "测试服务插件", true);
            ensurePluginConfigExists("CommandExecutor", "com.ycbd.demo.plugin.commandexecutor.CommandExecutorPlugin", "命令执行插件", true);
        } catch (Exception e) {
            logger.error("初始化插件配置失败", e);
        }
    }

    /**
     * 确保插件配置存在
     */
    private void ensurePluginConfigExists(String pluginName, String className, String description, boolean isActive) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("plugin_name", pluginName);

            Map<String, Object> existingConfig = baseService.getOne("plugin_config", params);
            if (existingConfig == null) {
                logger.info("添加插件配置: {}", pluginName);
                Map<String, Object> newConfig = new HashMap<>();
                newConfig.put("plugin_name", pluginName);
                newConfig.put("class_name", className);
                newConfig.put("description", description);
                newConfig.put("is_active", isActive);
                baseService.save("plugin_config", newConfig);
            } else {
                logger.info("插件配置已存在: {}", pluginName);
            }
        } catch (Exception e) {
            logger.error("确保插件配置存在失败: " + pluginName, e);
        }
    }

    /**
     * 检查表是否存在 通过尝试查询表中的一条记录来判断表是否存在
     */
    private boolean isTableExists(String tableName) {
        try {
            // 尝试查询表中的一条记录，如果表不存在会抛出异常
            baseService.queryList(tableName, 0, 1, null, null, null, null, null);
            return true;
        } catch (Exception e) {
            logger.warn("表 {} 不存在或无法访问: {}", tableName, e.getMessage());
            return false;
        }
    }
}
