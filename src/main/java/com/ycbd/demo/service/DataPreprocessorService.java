package com.ycbd.demo.service;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ycbd.demo.security.UserContext;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;

/**
 * 数据预处理服务：负责在持久化前对 Map 数据进行必填校验、缺失字段自动填充、审计字段处理等。
 *
 * 核心业务逻辑： 1. 必填字段验证 - 如果必填字段不存在且无法自动填充，则抛出异常 2. 自动填充处理 - 对于空值字段，尝试从用户上下文或默认值填充
 * 3. 审计字段处理 - 自动填充审计相关字段
 *
 * 优先级规则： 1. 传入数据优先 - 如果传入数据中已包含字段值，则不会被覆盖 2. 用户上下文数据 - 对于空值字段，尝试从用户上下文获取 3.
 * 默认值填充 - 如果上下文中也没有值，则使用默认值 4. 数据库默认值 - 依赖数据库层面处理默认值
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
        // 插入时移除ID字段，让数据库自动生成
        if (data.containsKey("id") && isEmpty(data.get("id"))) {
            data.remove("id");
        }
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

        // 先逐条处理必填字段、默认值和审计字段
        for (Map<String, Object> item : list) {
            // 插入时移除ID字段，让数据库自动生成
            if (item.containsKey("id") && isEmpty(item.get("id"))) {
                item.remove("id");
            }
            preprocessInternal(table, item, false);
        }

        // 统一列集合（确保所有记录都有相同的字段集）
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
    /**
     * 预处理内部实现
     *
     * 处理流程： 1. 验证字段存在性 - 确保数据中的字段在列属性表中存在 2. 必填字段验证 - 先验证，确保不符合必填条件立即抛出异常 -
     * 如果字段有默认值、能从用户上下文获取或是审计字段，则不抛出异常 3. 自动填充处理 - 对空值字段进行填充 - 优先从用户上下文获取 -
     * 其次使用默认值 4. 审计字段处理 - 根据操作类型(插入/更新)填充审计字段
     */
    private void preprocessInternal(String table, Map<String, Object> data, boolean isUpdate) {
        List<Map<String, Object>> attrs = metaService.getColumnAttrs(table);
        Map<String, Map<String, Object>> attrMap = attrs.stream()
                .collect(java.util.stream.Collectors.toMap(m -> MapUtil.getStr(m, "column_name"), m -> m));

        // 验证数据中的字段是否都存在于列属性表中
        validateFieldsExist(table, data, attrMap);

        // 第一步：必填字段校验 - 先验证，确保不符合必填条件立即抛出异常
        validateRequiredFields(attrs, data);

        // 第二步：自动填充处理 - 包括用户上下文字段和默认值字段
        processEmptyFields(attrs, data);

        // 第三步：审计字段处理 - 只在没有值时填充
        processAuditFields(attrMap, data, isUpdate);
    }

    /**
     * 验证数据中的字段是否都存在于列属性表中 如果发现不存在的字段，记录警告日志并从数据中移除
     */
    private void validateFieldsExist(String table, Map<String, Object> data, Map<String, Map<String, Object>> attrMap) {
        // 获取列属性表中定义的所有字段名
        Set<String> validColumns = new HashSet<>(attrMap.keySet());

        // 添加标准审计字段到有效字段集合中
        addStandardAuditFields(validColumns);

        // 添加特殊系统字段，如id
        validColumns.add("id");

        // 检查数据中的每个字段是否存在于有效字段集合中
        Set<String> invalidFields = data.keySet().stream()
                .filter(key -> !validColumns.contains(key))
                .collect(Collectors.toSet());

        // 如果有无效字段，记录警告并从数据中移除
        if (!invalidFields.isEmpty()) {
            logger.warn("表[{}]中发现无效字段: {}，这些字段将被忽略", table, invalidFields);
            invalidFields.forEach(data::remove);
        }
    }

    /**
     * 添加标准审计字段到有效字段集合中
     */
    private void addStandardAuditFields(Set<String> validColumns) {
        // 标准审计字段列表
        String[] standardAuditFields = {
            "create_by", "created_at", "created_time", "create_time",
            "update_by", "updated_at", "updated_time", "update_time",
            "creator", "updater", "org_id", "tenant_id"
        };

        for (String field : standardAuditFields) {
            validColumns.add(field);
        }
    }

    /**
     * 必填字段校验
     *
     * 验证逻辑： 1. 检查必填字段是否存在且非空 2. 如果不存在或为空，检查是否为自动填充字段： - 有默认值的字段 - 能从用户上下文获取的字段
     * - 审计字段 3. 如果不是自动填充字段，则抛出异常 4. 如果是自动填充字段，则记录日志并继续处理
     */
    private void validateRequiredFields(List<Map<String, Object>> attrs, Map<String, Object> data) {
        Map<String, Object> userContext = UserContext.getUser();

        for (Map<String, Object> attr : attrs) {
            String columnName = MapUtil.getStr(attr, "column_name");
            boolean required = MapUtil.getBool(attr, "is_required", false);

            if (required && (!data.containsKey(columnName) || isEmpty(data.get(columnName)))) {
                // 检查字段是否有数据库默认值或代码中的默认值
                Object defaultValue = attr.get("default_value");
                boolean hasDefaultValue = defaultValue != null && !defaultValue.toString().isEmpty();

                // 检查字段是否可以从用户上下文获取
                boolean canGetFromUserContext = userContext != null
                        && userContext.containsKey(columnName)
                        && isNotEmpty(userContext.get(columnName));

                // 检查是否为审计字段
                boolean isAudit = isAuditField(columnName);

                // 如果字段没有默认值、不能从用户上下文获取且不是审计字段，则抛出异常
                if (!hasDefaultValue && !canGetFromUserContext && !isAudit) {
                    throw new IllegalArgumentException("字段 [" + columnName + "] 为必填项，不能为空");
                } else {
                    logger.debug("必填字段 [{}] 将通过自动填充处理", columnName);
                }
            }
        }
    }

    /**
     * 判断字段是否为审计字段
     */
    private boolean isAuditField(String fieldName) {
        // 标准审计字段列表
        String[] auditFields = {
            "create_by", "created_at", "created_time", "create_time",
            "update_by", "updated_at", "updated_time", "update_time",
            "creator", "updater", "org_id", "tenant_id"
        };

        for (String field : auditFields) {
            if (field.equals(fieldName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 处理空值字段
     *
     * 填充优先顺序： 1. 传入数据 - 如果数据中已有非空值，保留现有值 2. 用户上下文 - 如果数据中没有值，尝试从用户上下文获取 3. 默认值
     * - 如果上下文中也没有，使用列属性表中的默认值
     *
     * 注意： - 审计字段由专门的方法处理，这里会跳过 - 只处理空值字段，不会覆盖已有值
     */
    private void processEmptyFields(List<Map<String, Object>> attrs, Map<String, Object> data) {
        Map<String, Object> userContext = UserContext.getUser();

        for (Map<String, Object> attr : attrs) {
            String columnName = MapUtil.getStr(attr, "column_name");
            Integer editFlag = MapUtil.getInt(attr, "edit_flag", 0);

            // 如果数据中已有非空值，优先使用现有值 (最高优先级)
            if (data.containsKey(columnName) && isNotEmpty(data.get(columnName))) {
                continue;
            }

            // 跳过审计字段，它们将在processAuditFields中处理
            if (isAuditField(columnName)) {
                continue;
            }

            // 尝试从用户上下文填充 (第二优先级)
            if (userContext != null && userContext.containsKey(columnName) && isNotEmpty(userContext.get(columnName))) {
                data.put(columnName, userContext.get(columnName));
                logger.debug("从用户上下文填充字段: {}={}", columnName, userContext.get(columnName));
                continue;
            }

            // 尝试使用默认值填充 (第三优先级)
            // 这里处理的是列属性表中的默认值表达式，如userId()、now()等
            Object defaultValue = attr.get("default_value");
            if (defaultValue != null && !defaultValue.toString().isEmpty()) {
                Object defaultVal = getDefaultValueForAttr(attr);
                if (defaultVal != null) {
                    data.put(columnName, defaultVal);
                    logger.debug("使用默认值填充字段: {}={}", columnName, defaultVal);
                }
            }
        }
    }

    /**
     * 处理审计字段 - 只在没有值时填充
     */
    private void processAuditFields(Map<String, Map<String, Object>> attrMap, Map<String, Object> data, boolean isUpdate) {
        Map<String, Object> userContext = UserContext.getUser();
        if (userContext == null) {
            return;
        }

        Integer userId = UserContext.getUserId();
        Timestamp now = new Timestamp(System.currentTimeMillis());
        String username = UserContext.getUserName();
        Integer orgId = UserContext.getOrgId();
        Object tenantId = userContext.get("tenantId");

        // 标准审计字段数组: [字段名, 是否仅插入时填充, 填充值]
        Object[][] auditFields = {
            {"create_by", true, userId},
            {"created_at", true, now},
            {"created_time", true, now},
            {"create_time", true, now},
            {"update_by", false, userId},
            {"updated_at", false, now},
            {"updated_time", false, now},
            {"update_time", false, now},
            {"creator", true, username},
            {"updater", false, username},
            {"org_id", true, orgId},
            {"tenant_id", true, tenantId}
        };

        // 遍历所有可能的审计字段
        for (Object[] fieldInfo : auditFields) {
            String fieldName = (String) fieldInfo[0];
            boolean isInsertOnly = (Boolean) fieldInfo[1];
            Object value = fieldInfo[2];

            // 跳过不存在的字段
            if (!attrMap.containsKey(fieldName)) {
                continue;
            }

            // 更新操作且字段仅在插入时填充，则跳过
            if (isUpdate && isInsertOnly) {
                continue;
            }

            // 如果数据中已有非空值，优先使用现有值 (始终尊重用户提供的数据)
            if (data.containsKey(fieldName) && isNotEmpty(data.get(fieldName))) {
                continue;
            }

            // 如果有值才填充
            if (value != null) {
                data.put(fieldName, value);
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

    /**
     * 检查值是否为空
     */
    private boolean isEmpty(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof String) {
            return StrUtil.isBlank((String) value);
        }
        return false;
    }

    /**
     * 检查值是否非空
     */
    private boolean isNotEmpty(Object value) {
        return !isEmpty(value);
    }
}
