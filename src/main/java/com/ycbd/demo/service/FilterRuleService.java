package com.ycbd.demo.service;

import java.util.Map;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.ycbd.demo.security.UserContext;

/**
 * 查询过滤规则服务。
 * 根据column_attribute.search_flag标记的可搜索字段，
 * 自动从用户上下文中填充过滤条件。
 */
@Service
public class FilterRuleService {

    @Autowired
    private MetaService metaService;

    /**
     * 根据元数据和用户上下文，对查询参数进行补充。
     * 如果search_flag=1的字段在传入参数中不存在，则尝试从用户上下文中获取并填充。
     */
    public void enhanceFilters(String table, Map<String, Object> currentParams) {
        if (currentParams == null) {
            return;
        }

        // 获取用户上下文信息
        Map<String, Object> userInfo = UserContext.getUser();
        if (userInfo == null || userInfo.isEmpty()) {
            return; // 没有用户信息，不进行自动填充
        }

        // 获取可搜索的列属性
        List<Map<String, Object>> columns = metaService.getColumnAttrs(table);
        if (columns == null || columns.isEmpty()) {
            return;
        }

        for (Map<String, Object> column : columns) {
            // 只处理search_flag为1的列
            Integer searchFlag = MapUtil.getInt(column, "search_flag", 0);
            if (searchFlag != 1) {
                continue;
            }

            String columnName = MapUtil.getStr(column, "column_name");
            if (StrUtil.isBlank(columnName)) {
                continue;
            }

            // 若调用方已显式传入该列，则不覆盖
            if (currentParams.containsKey(columnName)) {
                continue;
            }

            // 尝试从用户上下文中获取同名字段
            Object userValue = userInfo.get(columnName);
            if (userValue != null) {
                // 获取查询类型，默认为eq
                String queryType = MapUtil.getStr(column, "query_type", "eq");

                // 按 query_type 追加后缀，供 BaseService buildWhereClause 识别
                switch (queryType.toLowerCase()) {
                    case "like":
                        currentParams.put(columnName + "_like", userValue);
                        break;
                    case "in":
                        currentParams.put(columnName + "_in", userValue);
                        break;
                    case "range":
                        currentParams.put(columnName + "_range", userValue);
                        break;
                    default:
                        currentParams.put(columnName, userValue);
                }
            }
        }
    }
} 