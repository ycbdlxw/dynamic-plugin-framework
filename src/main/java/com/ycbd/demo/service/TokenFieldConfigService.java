package com.ycbd.demo.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ycbd.demo.security.UserContext;

import cn.hutool.core.util.StrUtil;

/**
 * Token字段配置服务 负责从column_attribute表读取Token字段配置
 */
@Service
public class TokenFieldConfigService {

    private static final Logger logger = LoggerFactory.getLogger(TokenFieldConfigService.class);
    private static final String TOKEN_FIELD_FLAG = "isTokenField";
    private static final String TOKEN_FIELD_NAME = "tokenName";

    @Autowired
    private BaseService baseService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 获取所有启用的Token字段 从column_attribute表的other_info字段中读取配置
     */
    @Cacheable(value = "token_fields")
    public Map<String, Boolean> getEnabledTokenFields() {
        logger.info("从column_attribute表加载Token字段配置");

        Map<String, Boolean> result = new HashMap<>();
        try {
            // 查询sys_user表的列属性
            List<Map<String, Object>> userAttrs = baseService.getColumnAttributes("sys_user", null);
            logger.info("获取到的sys_user表列属性数量: {}", userAttrs != null ? userAttrs.size() : 0);

            processColumnAttributes(userAttrs, result);

            // 查询sys_user_role和sys_role表的列属性，用于角色相关字段
            List<Map<String, Object>> roleAttrs = baseService.getColumnAttributes("sys_role", null);
            logger.info("获取到的sys_role表列属性数量: {}", roleAttrs != null ? roleAttrs.size() : 0);

            processColumnAttributes(roleAttrs, result);

            // 查询sys_org表的列属性，用于组织机构相关字段
            List<Map<String, Object>> orgAttrs = baseService.getColumnAttributes("sys_org", null);
            logger.info("获取到的sys_org表列属性数量: {}", orgAttrs != null ? orgAttrs.size() : 0);

            processColumnAttributes(orgAttrs, result);

            // 如果没有找到任何配置，使用默认配置
            if (result.isEmpty()) {
                logger.warn("未找到任何Token字段配置，使用默认配置");
                addDefaultTokenFields(result);
            }
        } catch (Exception e) {
            logger.warn("读取Token字段配置失败，使用默认配置", e);
            addDefaultTokenFields(result);
        }

        // 确保userId和username字段始终启用
        result.put("userId", true);
        result.put("username", true);
        // roles字段默认启用
        result.putIfAbsent("roles", true);

        logger.info("最终启用的Token字段: {}", result);
        return result;
    }

    /**
     * 处理列属性，提取token字段配置
     */
    private void processColumnAttributes(List<Map<String, Object>> attrs, Map<String, Boolean> result) {
        if (attrs == null || attrs.isEmpty()) {
            return;
        }

        for (Map<String, Object> attr : attrs) {
            String otherInfo = (String) attr.get("other_info");
            if (StrUtil.isBlank(otherInfo)) {
                continue;
            }

            try {
                Map<String, Object> otherMap = objectMapper.readValue(otherInfo, Map.class);
                if (Boolean.TRUE.equals(otherMap.get(TOKEN_FIELD_FLAG))) {
                    String columnName = (String) attr.get("column_name");
                    String tokenName = otherMap.containsKey(TOKEN_FIELD_NAME)
                            ? (String) otherMap.get(TOKEN_FIELD_NAME) : columnName;

                    result.put(tokenName, true);
                    logger.info("添加Token字段: {} -> {}", columnName, tokenName);
                }
            } catch (Exception e) {
                logger.warn("解析other_info字段JSON失败: {}", otherInfo, e);
            }
        }
    }

    /**
     * 添加默认的Token字段配置
     */
    private void addDefaultTokenFields(Map<String, Boolean> result) {
        result.put("userId", true);
        result.put("username", true);
        result.put("real_name", true);
        result.put("roles", true);
        result.put("orgId", true);
        result.put("orgName", true);
    }

    /**
     * 刷新Token字段配置 同时更新UserContext中的缓存
     */
    @CacheEvict(value = "token_fields", allEntries = true)
    public void refreshTokenFieldConfig() {
        Map<String, Boolean> config = getEnabledTokenFields();
        UserContext.refreshFieldConfig(config);
        logger.info("Token字段配置已刷新，共 {} 个字段", config.size());
    }

    /**
     * 获取所有启用的字段名列表
     */
    public List<String> getEnabledFieldNames() {
        return getEnabledTokenFields().entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * 设置字段为Token字段
     *
     * @param tableName 表名
     * @param columnName 列名
     * @param tokenName Token中的字段名
     * @param enabled 是否启用
     */
    public void setTokenField(String tableName, String columnName, String tokenName, boolean enabled) {
        try {
            // 查询列属性
            Map<String, Object> params = new HashMap<>();
            params.put("db_table_name", tableName);
            params.put("column_name", columnName);

            Map<String, Object> attr = baseService.getOne("column_attribute", params);
            if (attr == null) {
                logger.warn("未找到列属性: {}.{}", tableName, columnName);
                return;
            }

            // 解析other_info字段
            String otherInfo = (String) attr.get("other_info");
            Map<String, Object> otherMap;
            if (StrUtil.isBlank(otherInfo)) {
                otherMap = new HashMap<>();
            } else {
                try {
                    otherMap = objectMapper.readValue(otherInfo, Map.class);
                } catch (Exception e) {
                    logger.warn("解析other_info字段JSON失败: {}", otherInfo, e);
                    otherMap = new HashMap<>();
                }
            }

            // 设置token字段标识
            otherMap.put(TOKEN_FIELD_FLAG, enabled);
            if (StrUtil.isNotBlank(tokenName) && !columnName.equals(tokenName)) {
                otherMap.put(TOKEN_FIELD_NAME, tokenName);
            } else {
                otherMap.remove(TOKEN_FIELD_NAME);
            }

            // 更新列属性
            String newOtherInfo = objectMapper.writeValueAsString(otherMap);
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("id", attr.get("id"));
            updateData.put("other_info", newOtherInfo);

            baseService.update("column_attribute", updateData, attr.get("id"));

            // 刷新缓存
            refreshTokenFieldConfig();

            logger.info("设置Token字段成功: {}.{} -> {}, enabled={}", tableName, columnName, tokenName, enabled);
        } catch (Exception e) {
            logger.error("设置Token字段失败", e);
        }
    }
}
