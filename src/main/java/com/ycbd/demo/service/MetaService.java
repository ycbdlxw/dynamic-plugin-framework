package com.ycbd.demo.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ycbd.demo.mapper.SystemMapper;
import com.ycbd.demo.utils.Tools;

/**
 * 元数据读取服务。统一负责从 table_attribute / column_attribute 等配置表
 * 拉取并做大小写、空值处理，供其他业务层调用。
 *
 * 如需扩展更多元数据表，只在此服务增加方法即可，保证单一职责。
 */
@Service
public class MetaService {

    @Autowired
    private SystemMapper systemMapper;

    /**
     * 获取表级属性（已做 key 小写化）。
     */
    public Map<String, Object> getTableAttr(String table) {
        Map<String, Object> raw = systemMapper.getAttributeData(table);
        return Tools.toLowerCaseKeyMap(raw);
    }

    /**
     * 获取字段属性列表（全部 key 小写化，name/querytype/columntype 等列也转为小写）。
     */
    public List<Map<String, Object>> getColumnAttrs(String table) {
        List<Map<String, Object>> raw = systemMapper.getColumnAttributes(table, null);
        List<Map<String, Object>> normalized = new ArrayList<>();
        for (Map<String, Object> item : raw) {
            Map<String, Object> lower = Tools.toLowerCaseKeyMap(item);
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
            normalized.add(lower);
        }
        return normalized;
    }
} 