package com.ycbd.demo.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.hutool.core.util.StrUtil;

/**
 * Map工具类，处理Map相关的操作
 */
public class MapUtils {

    private static final Logger logger = LoggerFactory.getLogger(MapUtils.class);

    /**
     * 获取两个Map之间的差异
     */
    public static Map<String, Object> getDifferences(Map<String, Object> map1, Map<String, Object> map2) {
        Map<String, Object> differences = new HashMap<>();

        // 遍历第一个映射
        for (Map.Entry<String, Object> entry : map1.entrySet()) {
            String key = entry.getKey();
            Object value1 = entry.getValue();

            // 检查键和值是否都存在于第二个映射中
            if (map2.containsKey(key) && !map2.get(key).equals(value1)) {
                differences.put(key, value1 + ":" + map2.get(key));
            }
        }

        // 遍历第二个映射，找出只存在于第二个映射的键
        for (Map.Entry<String, Object> entry : map2.entrySet()) {
            if (!map1.containsKey(entry.getKey())) {
                differences.put(entry.getKey(), "Key not found in map1");
            }
        }

        return differences;
    }

    /**
     * 合并字符串内容，去除重复项
     */
    public static String getUpdateContent(String str1, String str2) {
        List<String> strList = StrUtil.split(str1, ",");
        String content = str1;
        for (String key : strList) {
            if (!str2.contains(key)) {
                content += key + ",";
            }
        }
        // 使用HashSet去除重复元素
        Set<String> set = new HashSet<>(StrUtil.split(content, ","));
        // 将set再转换回List
        List<String> listWithoutDuplicates = new ArrayList<>(set);

        // 使用String.join()将列表转换为逗号分隔的字符串
        return String.join(",", listWithoutDuplicates);
    }

    /**
     * 将Map的key统一转换为小写（非递归）
     */
    public static Map<String, Object> toLowerCaseKeyMap(Map<String, Object> source) {
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

    /**
     * 预处理数据，确保所有值都是MyBatis可以处理的类型
     */
    public static Map<String, Object> processMapForMyBatis(Map<String, Object> data) {
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
        return processedData;
    }

    /**
     * 批量处理数据列表，确保所有值都是MyBatis可以处理的类型
     */
    public static List<Map<String, Object>> processMapListForMyBatis(List<Map<String, Object>> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            return new ArrayList<>();
        }

        List<Map<String, Object>> processedData = new ArrayList<>();
        for (Map<String, Object> item : dataList) {
            processedData.add(processMapForMyBatis(item));
        }
        return processedData;
    }

    /**
     * 根据查询类型将参数值转换为字符串格式
     */
    public static String convertValueToString(Object valueObj, String queryType) {
        if (valueObj == null) {
            return "";
        }

        // 处理List类型
        if (valueObj instanceof List) {
            List<?> list = (List<?>) valueObj;
            if (list.isEmpty()) {
                return "";
            }

            if ("range".equalsIgnoreCase(queryType)) {
                if (list.size() >= 2) {
                    return list.get(0).toString() + "~" + list.get(1).toString();
                } else if (list.size() == 1) {
                    return list.get(0).toString();
                }
                return "";
            }

            // 默认使用逗号分隔（适用于in查询和其他类型）
            return list.stream()
                    .filter(item -> item != null)
                    .map(Object::toString)
                    .collect(Collectors.joining(","));
        }

        // 处理布尔值
        if (valueObj instanceof Boolean) {
            return ((Boolean) valueObj) ? "1" : "0";
        }

        // 处理数组
        if (valueObj.getClass().isArray()) {
            Object[] array = (Object[]) valueObj;
            if (array.length == 0) {
                return "";
            }

            if ("range".equalsIgnoreCase(queryType)) {
                if (array.length >= 2) {
                    return array[0].toString() + "~" + array[1].toString();
                } else if (array.length == 1) {
                    return array[0].toString();
                }
                return "";
            }

            // 默认使用逗号分隔
            return Arrays.stream(array)
                    .filter(item -> item != null)
                    .map(Object::toString)
                    .collect(Collectors.joining(","));
        }

        // 处理特殊类型
        if (valueObj.getClass().getName().equals("cn.hutool.core.convert.NumberWithFormat")) {
            return valueObj.toString();
        }

        // 处理字符串类型
        String valueStr = valueObj.toString();

        // 针对字符串值：若当前 queryType 指定为 range，但值中已包含分隔符，则直接返回原始字符串
        if ("range".equalsIgnoreCase(queryType) && (valueStr.contains("~") || valueStr.contains("至") || valueStr.contains(","))) {
            return valueStr;
        }

        // 针对字符串值：若当前 queryType 指定为 IN，但值中已包含逗号，则直接返回原始字符串
        if ("in".equalsIgnoreCase(queryType) && valueStr.contains(",")) {
            return valueStr;
        }

        return valueStr;
    }
}
