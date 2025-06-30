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

/**
 * Token字段配置服务 负责从column_attribute表读取Token字段配置
 */
@Service
public class TokenFieldConfigService {

    private static final Logger logger = LoggerFactory.getLogger(TokenFieldConfigService.class);

    @Autowired
    private BaseService baseService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 获取所有启用的Token字段 从column_attribute表的Other字段中读取配置
     */
    @Cacheable(value = "token_fields")
    public Map<String, Boolean> getEnabledTokenFields() {
        logger.info("从column_attribute表加载Token字段配置");

        Map<String, Boolean> result = new HashMap<>();
        try {
            List<Map<String, Object>> attrs = baseService.getColumnAttributes("sys_user", null);
            for (Map<String, Object> attr : attrs) {
                Object otherObj = attr.get("other");
                if (otherObj == null) {
                    continue;
                }
                Map<String, Object> otherMap = objectMapper.readValue(String.valueOf(otherObj), Map.class);
                if (Boolean.TRUE.equals(otherMap.get("isTokenField"))) {
                    String tokenName = otherMap.getOrDefault("tokenName", attr.get("name")).toString();
                    result.put(tokenName, true);
                }
            }
            // roles 字段不在sys_user表内，默认加入
            result.putIfAbsent("roles", true);
        } catch (Exception e) {
            logger.warn("读取Token字段配置失败，回退硬编码", e);
            // fallback
            result.put("userId", true);
            result.put("username", true);
            result.put("real_name", true);
            result.put("roles", true);
        }

        return result;
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
}
