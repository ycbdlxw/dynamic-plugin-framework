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
            Object valueObj = paraEntry.getValue();
            if (valueObj == null || "".equals(valueObj)) {
                continue;
            }

            String value = valueObj instanceof String ? (String) valueObj : String.valueOf(valueObj);

            Optional<Map<String, Object>> attributeOpt = attributeList.stream()
                    .filter(map -> MapUtil.getStr(map, "prop").equals(key))
                    .findFirst();

            if (attributeOpt.isPresent()) {
                Map<String, Object> attribute = attributeOpt.get();
                if (sqlBuilder.length() > 0) {
                    sqlBuilder.append(logicalOperator);
                }

                // 优化：如果key包含点（如ur.user_id），直接用key，否则拼接table名
                String columnName;
                if (key.contains(".")) {
                    columnName = key;
                } else {
                    columnName = table + "." + key;
                }
                String queryType = MapUtil.getStr(attribute, "queryType", "eq");
                int columnType = MapUtil.getInt(attribute, "columnType", 1); // 默认为字符串类型

                String condition = buildCondition(columnName, value, queryType, columnType);
                if (StrUtil.isNotEmpty(condition)) {
                    sqlBuilder.append(condition);
                }
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
        if (StrUtil.isEmpty(value)) {
            return "";
        }

        QueryRuleEnum comparison = QueryRuleEnum.getByValue(queryType.toLowerCase());
        if (comparison == null) {
            // 默认使用等于查询
            comparison = QueryRuleEnum.EQ;
        }

        StringBuilder condition = new StringBuilder();

        switch (comparison) {
            case RANGE:
                condition.append(buildRangeCondition(columnName, value, columnType));
                break;
            case LIKE:
                condition.append(buildLikeCondition(columnName, value));
                break;
            case LEFT_LIKE:
                condition.append(columnName).append(" LIKE ").append("'%").append(escapeValue(value)).append("'");
                break;
            case RIGHT_LIKE:
                condition.append(columnName).append(" LIKE ").append("'").append(escapeValue(value)).append("%'");
                break;
            case IN:
                condition.append(buildInCondition(columnName, value, columnType));
                break;
            case GT:
            case GE:
            case LT:
            case LE:
            case EQ:
            case NE:
                condition.append(columnName).append(comparison.getValue());
                condition.append(formatValue(value, columnType));
                break;
            case SQL_RULES:
                // 自定义SQL片段，直接使用
                condition.append(value);
                break;
            default:
                condition.append(columnName).append(" = ");
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
        if (StrUtil.isEmpty(value)) {
            return "";
        }

        String[] parts;
        if (value.contains("~")) {
            parts = value.split("~", 2);
        } else if (value.contains("至")) {
            parts = value.split("至", 2);
        } else if (value.contains(",")) {
            parts = value.split(",", 2);
        } else {
            // 单值处理为大于等于
            return columnName + " >= " + formatValue(value.trim(), columnType);
        }

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
        if (StrUtil.isEmpty(value)) {
            return "";
        }

        if (value.contains(",")) {
            return buildMultiLikeCondition(columnName, value);
        } else {
            return columnName + " LIKE '%" + escapeValue(value) + "%'";
        }
    }

    /**
     * 构建多值模糊查询条件（OR连接）。
     *
     * @param columnName 列名
     * @param value 逗号分隔的多个值
     * @return 构建好的多值模糊查询条件
     */
    private static String buildMultiLikeCondition(String columnName, String value) {
        String[] values = value.split(",");
        StringBuilder sb = new StringBuilder("(");

        for (int i = 0; i < values.length; i++) {
            String trimmedValue = values[i].trim();
            if (StrUtil.isEmpty(trimmedValue)) {
                continue;
            }

            if (i > 0) {
                sb.append(" OR ");
            }
            sb.append(columnName).append(" LIKE '%").append(escapeValue(trimmedValue)).append("%'");
        }

        sb.append(")");
        return sb.toString();
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
        if (StrUtil.isEmpty(value)) {
            return "";
        }

        String[] values = value.split(",");
        // 过滤空值
        List<String> validValues = Arrays.stream(values)
                .map(String::trim)
                .filter(v -> !v.isEmpty())
                .collect(Collectors.toList());

        if (validValues.isEmpty()) {
            return "";
        }

        String formattedValues = validValues.stream()
                .map(v -> formatValue(v, columnType))
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
        if (value == null) {
            return "NULL";
        }

        // 数值(2) 或布尔(3) 类型: 仅当值本身是纯数字(含可选小数)时直接返回；
        // 否则按字符串处理并加引号，防止诸如 PENDING_CHECK 之类的枚举值被误认为列名导致 SQL 错误。
        if (columnType == 2 || columnType == 3) {
            // 判断是否为纯数字（支持整数和小数）
            if (value.matches("-?\\d+(\\.\\d+)?")) {
                return value;
            }

            // 布尔值特殊处理
            if (columnType == 3) {
                if ("true".equalsIgnoreCase(value) || "1".equals(value) || "yes".equalsIgnoreCase(value)) {
                    return "1";
                } else if ("false".equalsIgnoreCase(value) || "0".equals(value) || "no".equalsIgnoreCase(value)) {
                    return "0";
                }
            }
        }

        // 其余情况一律按字符串处理并转义单引号
        return "'" + escapeValue(value) + "'";
    }

    /**
     * 转义SQL字符串中的特殊字符
     *
     * @param value 原始值
     * @return 转义后的值
     */
    private static String escapeValue(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("'", "''");
    }
}
