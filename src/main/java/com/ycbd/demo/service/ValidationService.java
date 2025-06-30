package com.ycbd.demo.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
public class ValidationService {
    private static final Logger logger = LoggerFactory.getLogger(ValidationService.class);

    
    @Autowired
    private BaseService baseService;
    
private final ObjectMapper objectMapper = new ObjectMapper();

    @Cacheable(value = "validation_rules", key = "#tableName")
    public List<Map<String, Object>> getValidationRules(String tableName) {
        logger.info("从DB加载校验规则: {}", tableName);
        return baseService.getValidationRules(tableName);
    }

    public List<String> validate(String tableName, Map<String, Object> data) {
        List<String> errors = new ArrayList<>();
        List<Map<String, Object>> rules = getValidationRules(tableName);

        for (Map<String, Object> rule : rules) {
            String checkColumn = (String) rule.get("check_column");
            String checkMode = (String) rule.get("check_mode");
            Object value = data.get(checkColumn);

            if (value == null || !StringUtils.hasText(value.toString())) {
                continue; // 空值不校验，由IsRequired控制
            }
            
boolean isValid = true;
            try {
                switch (checkMode) {
                    case "isNotExit":
                        isValid = checkNotExist(rule, value);
                        break;
                    case "isExit":
                        isValid = checkExist(rule, value);
                        break;
                    case "isRang":
                        isValid = checkRange(rule, value);
                        break;
                    case "MutiReapeat":
                        isValid = checkMultiRepeat(rule, data);
                        break;
                    default:
                        logger.warn("未知的校验模式: {}", checkMode);
                }
            } catch (Exception e) {
                logger.error("校验时发生错误, rule id: {}", rule.get("id"), e);
                errors.add("校验规则[" + checkColumn + "]执行失败");
            }
            
if (!isValid) {
                errors.add((String) rule.get("errorMsg"));
            }
        }
        return errors;
    }
    
private boolean checkNotExist(Map<String, Object> rule, Object value) {
        String targetTable = (String) rule.get("target_table");
        String checkColumn = (String) rule.get("check_column");
        long count = baseService.count(targetTable, null, String.format("%s = '%s'", checkColumn, value));
        return count == 0;
    }
    
private boolean checkExist(Map<String, Object> rule, Object value) {
        return !checkNotExist(rule, value);
    }

    private boolean checkRange(Map<String, Object> rule, Object value) throws Exception {
        String paramsJson = (String) rule.get("params");
        Map<String, Integer> params = objectMapper.readValue(paramsJson, new TypeReference<Map<String, Integer>>() {});
        int numValue = Integer.parseInt(value.toString());
        
        return numValue >= params.get("min") && numValue <= params.get("max");
    }

    private boolean checkMultiRepeat(Map<String, Object> rule, Map<String, Object> data) {
        String targetTable = (String) rule.get("target_table");
        String[] columns = ((String) rule.get("check_column")).split(",");
        String where = Arrays.stream(columns)
                .map(col -> String.format("%s = '%s'", col, data.get(col)))
                .collect(Collectors.joining(" AND "));
        long count = baseService.count(targetTable, null, where);
        return count == 0;
    }
} 
