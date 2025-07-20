package com.ycbd.demo.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ycbd.demo.mapper.SystemMapper;
import com.ycbd.demo.utils.SqlWhereBuilder;
import com.ycbd.demo.utils.Tools;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.core.lang.TypeReference;

@Service
public class BaseService {

    @Autowired
    private SystemMapper systemMapper;

    @Autowired
    private FilterRuleService filterRuleService;

    @Autowired
    private DataPreprocessorService dataPreprocessorService;


    /** 全局字段映射缓存 key->真实列名 */
    private static final Map<String, String> GLOBAL_FIELD_MAPPINGS_CACHE = new ConcurrentHashMap<>();

    /**
     * 加载全局字段映射（sys_dict_item.category_code='FIELD_MAPPING'），带简单缓存。
     */
    private Map<String, String> getGlobalFieldMappings() {
        if (!GLOBAL_FIELD_MAPPINGS_CACHE.isEmpty()) {
            return GLOBAL_FIELD_MAPPINGS_CACHE;
        }
        try {
            // 直接使用 SystemMapper 查询字典项
            List<Map<String, Object>> list = systemMapper.getItemsData(
                    "sys_dict_item", "item_key,item_value", null,
                    "sys_dict_item.category_code = 'FIELD_MAPPING'", null, null, 1000, 0);
            for (Map<String, Object> row : list) {
                String key = MapUtil.getStr(row, "item_key");
                String val = MapUtil.getStr(row, "item_value");
                if (StrUtil.isNotBlank(key) && StrUtil.isNotBlank(val)) {
                    GLOBAL_FIELD_MAPPINGS_CACHE.putIfAbsent(key, val);
                }
            }
        } catch (Exception ex) {
            org.slf4j.LoggerFactory.getLogger(BaseService.class).warn("加载全局字段映射失败", ex);
        }
        return GLOBAL_FIELD_MAPPINGS_CACHE;
    }

    /**
     * 查询列表
     */
    public List<Map<String, Object>> queryList(String table, int pageIndex, int pageSize, String columns, Map<String, Object> params, String joinString, String sortByAndType, String groupByString) {
        if (pageSize <= 0) {
            pageSize = 10;
        }

        // 先应用隐式过滤规则
        filterRuleService.enhanceFilters(table, params);
        String whereStr = buildWhereClause(table, params);

        List<Map<String, Object>> raw = systemMapper.getItemsData(table, columns, joinString, whereStr, groupByString, sortByAndType, pageSize, pageIndex * pageSize);
        // 将所有 key 统一转为小写，避免 H2 返回大写列名导致获取失败
        List<Map<String, Object>> normalized = new ArrayList<>(raw.size());
        for (Map<String, Object> item : raw) {
            normalized.add(Tools.toLowerCaseKeyMap(item));
        }
        return normalized;
    }

    /**
     * 获取单条记录
     */
    public Map<String, Object> getOne(String table, Map<String, Object> params) {
        // 传递 null 以避免在 MyBatis OGNL 表达式中出现 "*" 被解析为数值，导致 NumberFormatException
        List<Map<String, Object>> results = queryList(table, 0, 1, null, params, null, null, null);
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * 获取表配置
     */
    public Map<String, Object> getTableConfig(String table) {
        Map<String, Object> params = new HashMap<>();
        params.put("db_table", table);
        return getOne("table_attribute", params);
    }

    /**
     * 获取字段属性
     */
    public List<Map<String, Object>> getColumnAttributes(String table, String attributeType) {
        try {
            List<Map<String, Object>> raw = systemMapper.getColumnAttributes(table, attributeType);
            List<Map<String, Object>> normalized = new ArrayList<>(raw.size());
            for (Map<String, Object> item : raw) {
                Map<String, Object> lower = Tools.toLowerCaseKeyMap(item);
                // 关键字段取值也转小写，方便后续比较
                Object colName = lower.get("column_name");
                if (colName != null) {
                    lower.put("column_name", colName.toString().toLowerCase());
                }
                Object qType = lower.get("query_type");
                if (qType != null) {
                    lower.put("query_type", qType.toString().toLowerCase());
                }
                Object cType = lower.get("column_type");
                if (cType != null) {
                    lower.put("column_type", cType.toString().toLowerCase());
                }
                Object isPri = lower.get("is_pri");
                if (isPri != null) {
                    lower.put("is_pri", isPri.toString().toLowerCase());
                }
                normalized.add(lower);
            }
            return normalized;
        } catch (Exception ex) {
            // 捕获异常并打印警告，但不阻止业务流程
            org.slf4j.LoggerFactory.getLogger(BaseService.class).warn("获取列属性失败，返回空列表以继续流程", ex);
            return java.util.Collections.emptyList();
        }
    }

    /**
     * 获取校验规则
     */
    public List<Map<String, Object>> getValidationRules(String tableName) {
        return systemMapper.getValidationRules(tableName);
    }

    /**
     * 获取记录总数
     */
    public int count(String table, Map<String, Object> params, String joinString) {
        String whereStr = buildWhereClause(table, params);
        return systemMapper.getDataCount(table, joinString, whereStr);
    }

    /**
     * 保存数据
     */
    @Transactional
    public long save(String table, Map<String, Object> data) {
        
        // 数据预处理（默认值 / 审计字段 / 必填校验）
        dataPreprocessorService.preprocessForSave(table, data);
        // 预处理数据，确保所有值都是MyBatis可以处理的类型
        Map<String, Object> processedData = Tools.processMapForMyBatis(data);
        systemMapper.insertData(table, processedData);
        return MapUtil.getLong(processedData, "id");
    }

    /**
     * 批量保存
     */
    @Transactional
    public void saveBatch(String table, List<Map<String, Object>> saveData) {
        if (saveData == null || saveData.isEmpty()) {
            return;
        }

        // 由 DataPreprocessorService 处理批量逻辑
        saveData = dataPreprocessorService.preprocessBatchSave(table, saveData);

        // 预处理数据（MyBatis 类型兼容）
        List<Map<String, Object>> processedData = Tools.processMapListForMyBatis(saveData);

        // 构建列名（使用第一条记录的顺序）
        StringBuilder columnsBuilder = new StringBuilder();
        for (String key : processedData.get(0).keySet()) {
            if (columnsBuilder.length() > 0) {
                columnsBuilder.append(",");
            }
            columnsBuilder.append("`").append(key).append("`");
        }

        systemMapper.insertDataBath(table, columnsBuilder.toString(), processedData);
    }

    /**
     * 更新数据
     */
    @Transactional
    public void update(String table, Map<String, Object> data, Object id) {
        String primaryKey = systemMapper.getPriKeyColumn(table);
        if (primaryKey == null || primaryKey.isEmpty()) {
            primaryKey = "id";
        }

        // 数据预处理
        dataPreprocessorService.preprocessForUpdate(table, data);

        // 预处理数据，确保所有值都是MyBatis可以处理的类型
        Map<String, Object> processedData = Tools.processMapForMyBatis(data);
        systemMapper.updateData(table, processedData, primaryKey, id);
    }

    /**
     * 批量更新
     */
    @Transactional
    public void updateBatch(String table, Map<String, Object> data, List<Object> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        String primaryKey = systemMapper.getPriKeyColumn(table);
        if (primaryKey == null || primaryKey.isEmpty()) {
            primaryKey = "id";
        }

        // 数据预处理
        dataPreprocessorService.preprocessForUpdate(table, data);

        // 预处理数据，确保所有值都是MyBatis可以处理的类型
        Map<String, Object> processedData = Tools.processMapForMyBatis(data);
        systemMapper.updateDataBatch(table, processedData, primaryKey, ids);
    }

    /**
     * 删除数据
     */
    @Transactional
    public void delete(String table, Object id) {
        String primaryKey = systemMapper.getPriKeyColumn(table);
        if (primaryKey == null || primaryKey.isEmpty()) {
            primaryKey = "id";
        }

        systemMapper.deleteData(table, primaryKey, id);
    }

    /**
     * 批量删除
     */
    @Transactional
    public void deleteBatch(String table, List<Object> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        String primaryKey = systemMapper.getPriKeyColumn(table);
        if (primaryKey == null || primaryKey.isEmpty()) {
            primaryKey = "id";
        }

        systemMapper.deleteDataBatch(table, primaryKey, ids);
    }

    /**
     * 获取用户完整信息（包含角色、组织机构等）
     */
    public Map<String, Object> getUserWithDetails(String username) {
        return systemMapper.getUserWithDetails(username);
    }

    /**
     * 构建WHERE子句
     */
    private String buildWhereClause(String table, Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }

        // ---------- 1. 解析字段映射配置 ----------
        Map<String, String> tableFieldMappings = new HashMap<>();
        try {
            // 直接查询表配置，避免递归调用
            Map<String, Object> queryParams = new HashMap<>();
            queryParams.put("db_table", table);
            List<Map<String, Object>> tableCfgList = systemMapper.getItemsData(
                    "table_attribute", "*", null, 
                    "db_table = '" + table.replace("'", "''") + "'", 
                    null, null, 1, 0);
            Map<String, Object> tableCfg = tableCfgList.isEmpty() ? null : tableCfgList.get(0);
            String defin = MapUtil.getStr(tableCfg, "defin_columns");
            if (StrUtil.isNotBlank(defin)) {
                JSONObject jo = JSONUtil.parseObj(defin);
                if (jo.containsKey("field_mapping")) {
                    tableFieldMappings = jo.getJSONObject("field_mapping")
                            .toBean(new TypeReference<Map<String, String>>() {
                            });
                }
            }
        } catch (Exception ignored) {
        }
        Map<String, String> globalMappings = getGlobalFieldMappings();

        // ---------- 2. 构建可变参数 ----------
        Map<String, Object> mutableParams = new HashMap<>(params);
        // 跳过分页/排序保留参数
        mutableParams.remove("pageindex");
        mutableParams.remove("pagesize");
        mutableParams.remove("sortbyandtype");
        mutableParams.remove("offset");
        mutableParams.remove("limit");
        mutableParams.remove("columns");

        // 特定表的无效字段过滤
        if ("sys_user".equals(table)) {
            mutableParams.remove("is_deleted");
            mutableParams.remove("roles");
            mutableParams.remove("roles_in");
        }

        // Step 1: 获取并转换字段属性
        // 直接查询列属性，避免递归调用
        List<Map<String, Object>> columnAttributes = systemMapper.getColumnAttributes(table, null);
        // 将所有key转为小写
        List<Map<String, Object>> normalizedAttrs = new ArrayList<>();
        for (Map<String, Object> item : columnAttributes) {
            Map<String, Object> lower = new HashMap<>();
            for (Map.Entry<String, Object> entry : item.entrySet()) {
                lower.put(entry.getKey().toLowerCase(), entry.getValue());
            }
            // 关键字段取值也转小写，方便后续比较
            Object colName = lower.get("column_name");
            if (colName != null) {
                lower.put("column_name", colName.toString().toLowerCase());
            }
            Object qType = lower.get("query_type");
            if (qType != null) {
                lower.put("query_type", qType.toString().toLowerCase());
            }
            Object cType = lower.get("column_type");
            if (cType != null) {
                lower.put("column_type", cType.toString().toLowerCase());
            }
            Object isPri = lower.get("is_pri");
            if (isPri != null) {
                lower.put("is_pri", isPri.toString().toLowerCase());
            }
            normalizedAttrs.add(lower);
        }
        columnAttributes = normalizedAttrs;
        List<Map<String, Object>> attributesForBuilder = new ArrayList<>();
        Map<String, String> queryTypeOverrides = new HashMap<>();

        // Step 2: 处理参数，支持后缀 _like/_in/_between/_range
        Map<String, Object> processedParams = new HashMap<>();

        for (Map.Entry<String, Object> entry : mutableParams.entrySet()) {
            String originalKey = entry.getKey();
            Object valueObj = entry.getValue();
            if (valueObj == null || "".equals(valueObj)) {
                continue;
            }

            String columnName = originalKey;
            String overrideQueryType = null;

            // 处理特殊后缀
            if (originalKey.endsWith("_like")) {
                columnName = originalKey.substring(0, originalKey.length() - 5);
                overrideQueryType = "like";
            } else if (originalKey.endsWith("_in")) {
                columnName = originalKey.substring(0, originalKey.length() - 3);
                overrideQueryType = "in";
            } else if (originalKey.endsWith("_between") || originalKey.endsWith("_range")) {
                int idx = originalKey.lastIndexOf("_");
                columnName = originalKey.substring(0, idx);
                overrideQueryType = "range";
            } else if (valueObj instanceof List || (valueObj instanceof String && ((String) valueObj).contains(","))) {
                // 如果值为列表或以逗号分隔，自动识别为 IN 查询
                overrideQueryType = "in";
            }

            // ---------- 字段映射 ----------
            String mapped = tableFieldMappings.getOrDefault(columnName, globalMappings.get(columnName));
            if (StrUtil.isNotBlank(mapped)) {
                columnName = mapped;
            }

            // 转换值为字符串
            String valueStr = Tools.convertValueToString(valueObj, overrideQueryType);
            processedParams.put(columnName, valueStr);

            // 记录查询类型覆盖
            if (overrideQueryType != null) {
                queryTypeOverrides.put(columnName, overrideQueryType);
            }
        }

        // Step 3: 构建属性列表
        for (Map<String, Object> attr : columnAttributes) {
            String propName = MapUtil.getStr(attr, "column_name");
            if (StrUtil.isBlank(propName) || !processedParams.containsKey(propName)) {
                continue;
            }

            Map<String, Object> convertedAttr = new HashMap<>();
            convertedAttr.put("prop", propName);

            // 应用查询类型覆盖
            String queryType = queryTypeOverrides.containsKey(propName)
                    ? queryTypeOverrides.get(propName)
                    : MapUtil.getStr(attr, "query_type", MapUtil.getStr(attr, "querytype", "eq")).toLowerCase();
            convertedAttr.put("queryType", queryType);

            // 确定列类型
            String showType = MapUtil.getStr(attr, "show_type");
            boolean isPri = MapUtil.getBool(attr, "is_pri", false);
            int columnType = 1; // 1=字符串, 2=数值, 3=布尔
            if ("number".equalsIgnoreCase(showType)) {
                columnType = 2;
            } else if ("switch".equalsIgnoreCase(showType)) {
                columnType = 3;
            } else if ("hidden".equalsIgnoreCase(showType) && isPri) {
                columnType = 2;
            }
            convertedAttr.put("columnType", columnType);

            attributesForBuilder.add(convertedAttr);
        }

        // 处理未在属性表中定义的字段
        for (String propName : processedParams.keySet()) {
            boolean exists = attributesForBuilder.stream()
                    .anyMatch(attr -> propName.equals(MapUtil.getStr(attr, "prop")));

            if (!exists) {
                Map<String, Object> tempAttr = new HashMap<>();
                tempAttr.put("prop", propName);
                tempAttr.put("queryType", queryTypeOverrides.getOrDefault(propName, "eq"));
                tempAttr.put("columnType", 1); // 默认为字符串类型
                attributesForBuilder.add(tempAttr);
            }
        }

        // 调用SqlWhereBuilder构建WHERE子句
        return SqlWhereBuilder.build(table, processedParams, attributesForBuilder, true).toString();
    }
}
