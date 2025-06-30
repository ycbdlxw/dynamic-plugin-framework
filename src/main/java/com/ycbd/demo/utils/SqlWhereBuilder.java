package com.ycbd.demo.utils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;

/**
 * SqlWhereBuilder 类用于构建 SQL WHERE 子句。 该类提供了一系列静态方法，用于根据给定的参数和属性列表生成 SQL 查询条件。
 * 它支持多种查询类型，包括等于、范围、模糊匹配、IN 查询等。
 *
 * @author ycbd
 * @version 1.0
 * @since [Date]
 */
public class SqlWhereBuilder {

    /**
     * 构建 SQL WHERE 子句。
     *
     * @param table 表名
     * @param paraMap 参数映射，包含字段名和对应的值
     * @param attributeList 属性列表，包含每个字段的查询类型和列类型
     * @param isExactMatch 是否使用精确匹配（AND）连接条件，false 则使用 OR
     * @return 构建好的 SQL WHERE 子句
     */
    public static StringBuilder build(String table, Map<String, Object> paraMap, List<Map<String, Object>> attributeList, boolean isExactMatch) {
        StringBuilder sqlBuilder = new StringBuilder();
        String logicalOperator = isExactMatch ? " AND " : " OR ";

        for (Map.Entry<String, Object> paraEntry : paraMap.entrySet()) {
            String key = paraEntry.getKey();
            String value = MapUtil.getStr(paraMap, key);
            if (StrUtil.isEmpty(value)) {
                continue;
            }

            Optional<Map<String, Object>> attributeOpt = attributeList.stream()
                    .filter(map -> MapUtil.getStr(map, "prop").equals(key))
                    .findFirst();

            if (attributeOpt.isPresent()) {
                Map<String, Object> attribute = attributeOpt.get();
                if (sqlBuilder.length() > 0) {
                    sqlBuilder.append(logicalOperator);
                }

                String columnName = table + ".`" + key + "`";
                String queryType = MapUtil.getStr(attribute, "queryType", "eq");
                int columnType = MapUtil.getInt(attribute, "columnType", -1);

                sqlBuilder.append(buildCondition(columnName, value, queryType, columnType));
            } else {
                System.out.println("Key not found in attribute list: " + key);
            }
        }

        return sqlBuilder;
    }

    /**
     * 根据查询类型构建单个条件。
     *
     * @param columnName 列名
     * @param value 值
     * @param queryType 查询类型
     * @param columnType 列类型
     * @return 构建好的单个条件字符串
     */
    private static String buildCondition(String columnName, String value, String queryType, int columnType) {
        QueryRuleEnum comparison = QueryRuleEnum.getByValue(queryType.toLowerCase());
        StringBuilder condition = new StringBuilder();

        switch (comparison) {
            case RANGE:
                condition.append(buildRangeCondition(columnName, value, columnType));
                break;
            case LIKE:
                condition.append(buildLikeCondition(columnName, value));
                break;
            case LEFT_LIKE:
                condition.append(columnName).append(comparison.getValue()).append("'%").append(value).append("'");
                break;
            case IN:
                condition.append(buildInCondition(columnName, value, columnType));
                break;
            default:
                condition.append(columnName).append(comparison.getValue());
                condition.append(formatValue(value, columnType));
        }

        return condition.toString();
    }

    /**
     * 构建范围查询条件。
     *
     * @param columnName 列名
     * @param value 范围值，格式为 "start~end" 或 "start至end"
     * @param columnType 列类型
     * @return 构建好的范围查询条件
     */
    private static String buildRangeCondition(String columnName, String value, int columnType) {
        String[] parts = value.split("[~至]", 2);
        String start = parts[0].trim();
        String end = parts.length > 1 ? parts[1].trim() : "";

        if (!start.isEmpty() && !end.isEmpty()) {
            return columnName + " BETWEEN " + formatValue(start, columnType) + " AND " + formatValue(end, columnType);
        } else if (!start.isEmpty()) {
            return columnName + " >= " + formatValue(start, columnType);
        } else if (!end.isEmpty()) {
            return columnName + " <= " + formatValue(end, columnType);
        }
        return "";
    }

    /**
     * 构建模糊查询条件。
     *
     * @param columnName 列名
     * @param value 查询值
     * @return 构建好的模糊查询条件
     */
    private static String buildLikeCondition(String columnName, String value) {
        if (value.contains(",")) {
            return generateSQL(value, columnName);
        } else {
            return columnName + " LIKE '%" + value + "%'";
        }
    }

    /**
     * 构建 IN 查询条件。
     *
     * @param columnName 列名
     * @param value 逗号分隔的多个值
     * @param columnType 列类型
     * @return 构建好的 IN 查询条件
     */
    private static String buildInCondition(String columnName, String value, int columnType) {
        String[] values = value.split(",");
        String formattedValues = Arrays.stream(values)
                .map(v -> formatValue(v.trim(), columnType))
                .collect(Collectors.joining(", "));
        return columnName + " IN (" + formattedValues + ")";
    }

    /**
     * 根据列类型格式化值。
     *
     * @param value 原始值
     * @param columnType 列类型
     * @return 格式化后的值
     */
    private static String formatValue(String value, int columnType) {
        // 数值(2) 或布尔(3) 类型: 仅当值本身是纯数字(含可选小数)时直接返回；
        // 否则按字符串处理并加引号，防止诸如 PENDING_CHECK 之类的枚举值被误认为列名导致 SQL 错误。
        if (columnType == 2 || columnType == 3) {
            // 判断是否为纯数字（支持整数和小数）
            if (value != null && value.matches("-?\\d+(\\.\\d+)?")) {
                return value;
            }
        }
        // 其余情况一律按字符串处理并转义单引号
        return "'" + value.replace("'", "''") + "'";
    }

    /**
     * 为逗号分隔的多个值生成 SQL 条件。
     *
     * @param values 逗号分隔的多个值
     * @param columnString 列名
     * @return 生成的 SQL 条件
     */
    private static String generateSQL(String values, String columnString) {
        StringBuilder sb = new StringBuilder();
        String[] valueArray = values.split(",");
        for (String value : valueArray) {
            String trimmedValue = value.trim();
            if (sb.length() > 0) {
                sb.append(" OR ");
            }
            sb.append(columnString).append(" LIKE '%").append(trimmedValue).append("%'");
        }
        return sb.toString();
    }
}
