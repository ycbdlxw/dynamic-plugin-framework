package com.ycbd.demo.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ycbd.demo.mapper.SystemMapper;
import com.ycbd.demo.utils.SqlWhereBuilder;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;

@Service
public class BaseService {

    @Autowired
    private SystemMapper systemMapper;

    /**
     * 查询列表
     */
    public List<Map<String, Object>> queryList(String table, int pageIndex, int pageSize, String columns, Map<String, Object> params, String joinString, String sortByAndType, String groupByString) {
        if (pageSize <= 0) {
            pageSize = 10;
        }

        String whereStr = buildWhereClause(table, params);

        List<Map<String, Object>> raw = systemMapper.getItemsData(table, columns, joinString, whereStr, groupByString, sortByAndType, pageSize, pageIndex * pageSize);
        // 将所有 key 统一转为小写，避免 H2 返回大写列名导致获取失败
        List<Map<String, Object>> normalized = new ArrayList<>(raw.size());
        for (Map<String, Object> item : raw) {
            normalized.add(toLowerCaseKeyMap(item));
        }
        return normalized;
    }

    /**
     * 获取单条记录
     */
    public Map<String, Object> getOne(String table, Map<String, Object> params) {
        List<Map<String, Object>> results = queryList(table, 0, 1, "*", params, null, null, null);
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * 获取表配置
     */
    public Map<String, Object> getTableConfig(String table) {
        Map<String, Object> params = new HashMap<>();
        params.put("dbtable", table);
        return getOne("table_attribute", params);
    }

    /**
     * 获取字段属性
     */
    public List<Map<String, Object>> getColumnAttributes(String table, String attributeType) {
        List<Map<String, Object>> raw = systemMapper.getColumnAttributes(table, attributeType);
        List<Map<String, Object>> normalized = new ArrayList<>(raw.size());
        for (Map<String, Object> item : raw) {
            Map<String, Object> lower = toLowerCaseKeyMap(item);
            // 关键字段取值也转小写，方便后续比较
            Object colName = lower.get("name");
            if (colName != null) {
                lower.put("name", colName.toString().toLowerCase());
            }
            Object qType = lower.get("querytype");
            if (qType != null) {
                lower.put("querytype", qType.toString().toLowerCase());
            }
            Object cType = lower.get("columntype");
            if (cType != null) {
                lower.put("columntype", cType.toString().toLowerCase());
            }
            normalized.add(lower);
        }
        return normalized;
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
        // 预处理数据，确保所有值都是MyBatis可以处理的类型
        Map<String, Object> processedData = new HashMap<>();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            Object value = entry.getValue();
            // 如果是复杂对象，转换为字符串
            if (value != null && (value.getClass().getName().equals("cn.hutool.core.convert.NumberWithFormat")
                    || value.getClass().isArray() || value instanceof List)) {
                processedData.put(entry.getKey(), String.valueOf(value));
            } else {
                processedData.put(entry.getKey(), value);
            }
        }

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

        // 预处理数据，确保所有值都是MyBatis可以处理的类型
        List<Map<String, Object>> processedData = new ArrayList<>();
        for (Map<String, Object> item : saveData) {
            Map<String, Object> processedItem = new HashMap<>();
            for (Map.Entry<String, Object> entry : item.entrySet()) {
                Object value = entry.getValue();
                // 如果是复杂对象，转换为字符串
                if (value != null && (value.getClass().getName().equals("cn.hutool.core.convert.NumberWithFormat")
                        || value.getClass().isArray() || value instanceof List)) {
                    processedItem.put(entry.getKey(), String.valueOf(value));
                } else {
                    processedItem.put(entry.getKey(), value);
                }
            }
            processedData.add(processedItem);
        }

        // 构建列名
        StringBuilder columnsBuilder = new StringBuilder();
        Map<String, Object> firstItem = processedData.get(0);
        for (String key : firstItem.keySet()) {
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

        // 预处理数据，确保所有值都是MyBatis可以处理的类型
        Map<String, Object> processedData = new HashMap<>();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            Object value = entry.getValue();
            // 如果是复杂对象，转换为字符串
            if (value != null && (value.getClass().getName().equals("cn.hutool.core.convert.NumberWithFormat")
                    || value.getClass().isArray() || value instanceof List)) {
                processedData.put(entry.getKey(), String.valueOf(value));
            } else {
                processedData.put(entry.getKey(), value);
            }
        }

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

        // 预处理数据，确保所有值都是MyBatis可以处理的类型
        Map<String, Object> processedData = new HashMap<>();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            Object value = entry.getValue();
            // 如果是复杂对象，转换为字符串
            if (value != null && (value.getClass().getName().equals("cn.hutool.core.convert.NumberWithFormat")
                    || value.getClass().isArray() || value instanceof List)) {
                processedData.put(entry.getKey(), String.valueOf(value));
            } else {
                processedData.put(entry.getKey(), value);
            }
        }

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

        // Step 1: 获取并转换字段属性
        List<Map<String, Object>> originAttrs = getColumnAttributes(table, null);
        List<Map<String, Object>> attributesForBuilder = new ArrayList<>();
        Map<String, Map<String, Object>> attrIndex = new HashMap<>();

        for (Map<String, Object> attr : originAttrs) {
            String prop = MapUtil.getStr(attr, "name");
            if (StrUtil.isBlank(prop)) {
                continue;
            }

            Map<String, Object> converted = new HashMap<>();
            converted.put("prop", prop);
            converted.put("queryType", MapUtil.getStr(attr, "querytype", "eq").toLowerCase());

            String showType = MapUtil.getStr(attr, "showtype");
            boolean isPri = MapUtil.getBool(attr, "ispri", false);
            int columnType = 1; // 1=字符串, 2=数值,3=布尔
            if ("number".equalsIgnoreCase(showType)) {
                columnType = 2;
            } else if ("switch".equalsIgnoreCase(showType)) {
                columnType = 3;
            } else if ("hidden".equalsIgnoreCase(showType) && isPri) {
                columnType = 2;
            }
            converted.put("columnType", columnType);

            attributesForBuilder.add(converted);
            attrIndex.put(prop, converted);
        }

        // Step 2: 处理参数，支持后缀 _like/_in/_between/_range
        Map<String, Object> paraMapForBuilder = new HashMap<>();

        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String originalKey = entry.getKey();
            Object valueObj = entry.getValue();
            if (valueObj == null || "".equals(valueObj)) {
                continue;
            }

            String columnName = originalKey;
            String overrideQueryType = null;

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
            }

            // 如果仍未确定查询类型，且值为列表或以逗号分隔，自动识别为 IN 查询（非布尔列）
            if (overrideQueryType == null) {
                if (valueObj instanceof List) {
                    overrideQueryType = "in";
                } else if (valueObj instanceof String && ((String) valueObj).contains(",")) {
                    overrideQueryType = "in";
                }
            }

            // 转换 value 为字符串，便于 SqlWhereBuilder 处理
            String valueStr = convertValueToString(valueObj, overrideQueryType);
            paraMapForBuilder.put(columnName, valueStr);

            // 覆盖属性中的queryType
            if (overrideQueryType != null) {
                Map<String, Object> attr = attrIndex.get(columnName);
                if (attr != null) {
                    attr.put("queryType", overrideQueryType);
                } else {
                    // 若不存在属性记录，则临时创建一个默认
                    Map<String, Object> tempAttr = new HashMap<>();
                    tempAttr.put("prop", columnName);
                    tempAttr.put("queryType", overrideQueryType);
                    tempAttr.put("columnType", 1);
                    attributesForBuilder.add(tempAttr);
                }
            }
        }

        StringBuilder whereBuilder = SqlWhereBuilder.build(table, paraMapForBuilder, attributesForBuilder, true);
        return whereBuilder.toString();
    }

    /**
     * 根据查询类型将参数值转换为 SqlWhereBuilder 期望的字符串格式。
     */
    private String convertValueToString(Object valueObj, String queryType) {
        if (valueObj instanceof List) {
            List<?> list = (List<?>) valueObj;
            if (list.isEmpty()) {
                return "";
            }

            if ("in".equalsIgnoreCase(queryType)) {
                return list.stream().map(Object::toString).collect(Collectors.joining(","));
            }

            if ("range".equalsIgnoreCase(queryType)) {
                if (list.size() >= 2) {
                    return list.get(0).toString() + "~" + list.get(1).toString();
                } else {
                    return list.get(0).toString();
                }
            }

            // 默认逗号分隔
            return list.stream().map(Object::toString).collect(Collectors.joining(","));
        }

        if (valueObj instanceof Boolean) {
            return ((Boolean) valueObj) ? "1" : "0";
        }

        // 针对字符串值：若当前 queryType 指定为 IN，但值中已包含逗号，则直接返回原始字符串（逗号分隔）
        if (queryType != null && "in".equalsIgnoreCase(queryType) && valueObj instanceof String) {
            return (String) valueObj;
        }

        return valueObj.toString();
    }

    /**
     * 将 Map 的 key 统一转换为小写（非递归）
     */
    private Map<String, Object> toLowerCaseKeyMap(Map<String, Object> source) {
        Map<String, Object> target = new HashMap<>();
        if (source == null) {
            return target;
        }
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String key = entry.getKey();
            if (key != null) {
                target.put(key.toLowerCase(), entry.getValue());
            }
        }
        return target;
    }
}
