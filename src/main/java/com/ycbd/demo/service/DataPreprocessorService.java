package com.ycbd.demo.service;

import java.sql.Timestamp;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ycbd.demo.security.UserContext;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;

/**
 * 数据预处理服务：负责在持久化前对 Map 数据进行默认值补充、必填校验、审计字段填充等。
 *
 * 插入/更新/批量插入 均通过该服务完成统一处理，保证规则集中，便于后续数据驱动扩展。
 */
@Service
public class DataPreprocessorService {

    private static final Logger logger = LoggerFactory.getLogger(DataPreprocessorService.class);

    @Autowired
    private MetaService metaService;

    /**
     * 单条保存前的预处理
     */
    public void preprocessForSave(String table, Map<String, Object> data) {
        preprocessInternal(table, data, false);
    }

    /**
     * 单条更新前的预处理
     */
    public void preprocessForUpdate(String table, Map<String, Object> data) {
        preprocessInternal(table, data, true);
    }

    /**
     * 批量保存前的预处理（列补齐）
     */
    public List<Map<String, Object>> preprocessBatchSave(String table, List<Map<String, Object>> list) {
        if (list == null || list.isEmpty()) {
            return list;
        }

        // 先逐条处理默认值 / 审计字段 / 校验
        for (Map<String, Object> item : list) {
            preprocessInternal(table, item, false);
        }

        // 统一列集合
        Set<String> allColumns = new LinkedHashSet<>();
        for (Map<String, Object> map : list) {
            allColumns.addAll(map.keySet());
        }

        // 构建属性映射，便于补默认值
        List<Map<String, Object>> attrs = metaService.getColumnAttrs(table);
        Map<String, Map<String, Object>> attrMap = attrs.stream()
                .collect(java.util.stream.Collectors.toMap(m -> MapUtil.getStr(m, "column_name"), m -> m));

        // 二次循环补齐缺失列
        for (Map<String, Object> map : list) {
            for (String col : allColumns) {
                if (!map.containsKey(col)) {
                    Map<String, Object> attr = attrMap.get(col);
                    map.put(col, getDefaultValueForAttr(attr));
                }
            }
        }
        return list;
    }

    // -------------------- 内部实现 ---------------------

    private void preprocessInternal(String table, Map<String, Object> data, boolean isUpdate) {
        List<Map<String, Object>> attrs = metaService.getColumnAttrs(table);
        Map<String, Map<String, Object>> attrMap = attrs.stream()
                .collect(java.util.stream.Collectors.toMap(m -> MapUtil.getStr(m, "column_name"), m -> m));

        // 1. 自动填充用户上下文字段（edit_flag=1的字段）
        autoFillUserContextFields(attrs, data);
        
        // 2. 默认值 & 必填校验
        processDefaultValuesAndValidation(attrs, data);

        // 3. 审计字段
        addAuditFields(attrMap, data, isUpdate);
    }
    
    /**
     * 自动从用户上下文中填充edit_flag=1的字段
     */
    private void autoFillUserContextFields(List<Map<String, Object>> attrs, Map<String, Object> data) {
        Map<String, Object> userContext = UserContext.getUser();
        if (userContext == null || userContext.isEmpty()) {
            return;
        }
        
        for (Map<String, Object> attr : attrs) {
            String columnName = MapUtil.getStr(attr, "column_name");
            Integer editFlag = MapUtil.getInt(attr, "edit_flag", 0);
            
            // 只处理edit_flag=1的字段
            if (editFlag != 1) {
                continue;
            }
            
            // 如果数据中已有该字段值，不覆盖
            if (data.containsKey(columnName) && data.get(columnName) != null) {
                continue;
            }
            
            // 尝试从用户上下文中获取同名字段
            if (userContext.containsKey(columnName)) {
                Object userValue = userContext.get(columnName);
                if (userValue != null) {
                    data.put(columnName, userValue);
                    logger.debug("从用户上下文自动填充字段: {}={}", columnName, userValue);
                }
            }
        }
    }
    
    /**
     * 处理默认值和必填校验
     */
    private void processDefaultValuesAndValidation(List<Map<String, Object>> attrs, Map<String, Object> data) {
        for (Map<String, Object> attr : attrs) {
            String col = MapUtil.getStr(attr, "column_name");
            boolean required = MapUtil.getBool(attr, "is_required", false);
            Object val = data.get(col);

            if (val == null || (val instanceof String && StrUtil.isBlank((String) val))) {
                // 无值，先补默认
                Object defVal = getDefaultValueForAttr(attr);
                if (defVal != null) {
                    data.put(col, defVal);
                    val = defVal;
                }
            }

            // 必填校验
            if (required) {
                if (val == null || (val instanceof String && StrUtil.isBlank((String) val))) {
                    throw new IllegalArgumentException("字段[" + col + "]为必填项，不能为空！");
                }
            }
        }
    }

    /**
     * 添加审计字段（创建人、创建时间、更新人、更新时间等）
     */
    private void addAuditFields(Map<String, Map<String, Object>> attrMap, Map<String, Object> data, boolean isUpdate) {
        Map<String, Object> userContext = UserContext.getUser();
        if (userContext == null) {
            return;
        }

        Integer userId = UserContext.getUserId();
        Timestamp now = new Timestamp(System.currentTimeMillis());
        
        // 标准审计字段映射 - 使用Object[]数组存储，避免类型转换问题
        Object[][] auditFields = {
            // 字段名, 是否仅插入时填充, 用户上下文字段, 默认值
            {"create_by", true, "userId", userId},
            {"created_at", true, null, now},
            {"created_time", true, null, now},
            {"create_time", true, null, now},
            {"update_by", false, "userId", userId},
            {"updated_at", false, null, now},
            {"updated_time", false, null, now},
            {"update_time", false, null, now},
            {"creator", true, "username", UserContext.getUserName()},
            {"updater", false, "username", UserContext.getUserName()},
            {"org_id", true, "orgId", UserContext.getOrgId()},
            {"tenant_id", true, "tenantId", userContext.get("tenantId")}
        };
        
        // 遍历所有可能的审计字段
        for (Object[] fieldInfo : auditFields) {
            String fieldName = (String) fieldInfo[0];
            boolean isInsertOnly = (Boolean) fieldInfo[1];
            String contextField = (String) fieldInfo[2];
            Object defaultValue = fieldInfo[3];
            
            // 跳过不存在的字段
            if (!attrMap.containsKey(fieldName)) {
                continue;
            }
            
            // 更新操作且字段仅在插入时填充，则跳过
            if (isUpdate && isInsertOnly) {
                continue;
            }
            
            // 从用户上下文获取值，如果没有则使用默认值
            Object value = null;
            if (contextField != null && userContext.containsKey(contextField)) {
                value = userContext.get(contextField);
            } else {
                value = defaultValue;
            }
            
            // 插入操作使用putIfAbsent，更新操作使用put
            if (value != null) {
                if (isUpdate) {
                    data.put(fieldName, value);
                } else {
                    data.putIfAbsent(fieldName, value);
                }
            }
        }
    }

    /**
     * 根据列属性返回默认值
     */
    private Object getDefaultValueForAttr(Map<String, Object> attr) {
        if (attr == null) {
            return null;
        }

        // 从column_attribute表的default_value字段获取默认值
        Object dv = attr.get("default_value");
        if (dv != null && !dv.toString().isEmpty()) {
            // 支持特殊表达式 now() uuid() 等
            String dvStr = dv.toString();
            if ("now()".equalsIgnoreCase(dvStr)) {
                return new Timestamp(System.currentTimeMillis());
            }
            if ("uuid()".equalsIgnoreCase(dvStr)) {
                return java.util.UUID.randomUUID().toString();
            }
            if ("userId()".equalsIgnoreCase(dvStr)) {
                return UserContext.getUserId();
            }
            if ("username()".equalsIgnoreCase(dvStr)) {
                return UserContext.getUserName();
            }
            if ("orgId()".equalsIgnoreCase(dvStr)) {
                return UserContext.getOrgId();
            }
            
            // 如果是数字类型的字符串，转换为Integer
            if (dvStr.matches("^\\d+$")) {
                try {
                    return Integer.parseInt(dvStr);
                } catch (NumberFormatException e) {
                    return dvStr;
                }
            }
            
            return dv;
        }
        
        // type 推断 (1=string default, 2=number, 3=bool/date?)
        int columnType = MapUtil.getInt(attr, "column_type", 1);
        switch (columnType) {
            case 2:
            case 3:
                return 0;
            default:
                return "";
        }
    }
} 