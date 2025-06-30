package com.ycbd.demo.service;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import cn.hutool.core.util.StrUtil;

/**
 * 启动时自动为缺失的数据表写入 table_attribute / column_attribute 默认配置。 只处理 fileinfo 表，后续可扩展。
 */
@Component
@Order(5)
public class TableConfigInitializer implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(TableConfigInitializer.class);

    // 如需新增表，加入此列表即可，也支持在 application.yml 通过占位符注入
    private static final String[] TARGET_TABLES = {"fileinfo"};

    @Autowired
    private BaseService baseService;

    @Autowired
    private DataSource dataSource;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // 初始化AI处理器配置
        initAiProcessorConfigs();

        for (String table : TARGET_TABLES) {
            processOneTable(table.trim());
        }
    }

    private void processOneTable(String table) {
        try {
            Map<String, Object> exists = baseService.getOne("table_attribute", Map.of("dbtable", table));
            if (exists == null) {
                Map<String, Object> tableAttr = new HashMap<>();
                tableAttr.put("dbtable", table);
                tableAttr.put("tableName", table);
                tableAttr.put("mainKey", "id");
                tableAttr.put("sort", "id DESC");
                baseService.save("table_attribute", tableAttr);
                LOGGER.info("已写入 table_attribute: {}", table);
            }

            // 列处理：若不存在则新增，存在则批量更新部分字段（IsShowInList / searchFlag 等）
            try (Connection conn = dataSource.getConnection()) {
                DatabaseMetaData meta = conn.getMetaData();
                try (ResultSet rs = meta.getColumns(null, null, table, null)) {
                    int orderNo = 1;
                    while (rs.next()) {
                        String colName = rs.getString("COLUMN_NAME");
                        String typeName = rs.getString("TYPE_NAME");

                        Map<String, Object> existingCol = baseService.getOne("column_attribute",
                                Map.of("dbTableName", table, "name", colName));

                        boolean textType = StrUtil.containsAnyIgnoreCase(typeName, "char", "text", "clob");

                        if (existingCol == null) {
                            Map<String, Object> colAttr = new HashMap<>();
                            colAttr.put("dbTableName", table);
                            colAttr.put("name", colName);
                            colAttr.put("pagename", colName);
                            colAttr.put("IsShowInList", true);
                            colAttr.put("searchFlag", textType);
                            colAttr.put("editFlag", false);
                            colAttr.put("IsRequired", false);
                            colAttr.put("queryType", "=");
                            colAttr.put("showType", "input");
                            colAttr.put("OrderNo", orderNo++);

                            if ("id".equalsIgnoreCase(colName)) {
                                colAttr.put("IsShowInList", false);
                                colAttr.put("showType", "hidden");
                            }
                            baseService.save("column_attribute", colAttr);
                        } else {
                            // 批量更新部分状态：保持 searchFlag 与字段类型一致
                            Map<String, Object> updateMap = new HashMap<>();
                            updateMap.put("id", existingCol.get("id"));

                            // 动态规则：
                            // 1. searchFlag 与字段类型关联
                            updateMap.put("searchFlag", textType);

                            // 2. IsShowInList：主键或大字段(>1024)默认不显示
                            boolean showInList = !"id".equalsIgnoreCase(colName) && !"text".equalsIgnoreCase(typeName);
                            updateMap.put("IsShowInList", showInList);

                            // 3. editFlag：仅非主键且非只读字段允许编辑
                            updateMap.put("editFlag", !"id".equalsIgnoreCase(colName));

                            baseService.update("column_attribute", updateMap, existingCol.get("id"));
                        }
                    }
                }
            }

            // 业务校验：为 filename+url 唯一键自动插入 "isNotExit" 校验
            if ("fileinfo".equalsIgnoreCase(table)) {
                Map<String, Object> ruleExists = baseService.getOne("column_check_property",
                        Map.of("check_table", table, "check_column", "filename"));
                if (ruleExists == null) {
                    Map<String, Object> rule = new HashMap<>();
                    rule.put("check_table", table);
                    rule.put("target_table", table);
                    rule.put("check_column", "filename,url");
                    rule.put("check_mode", "isNotExit");
                    rule.put("check_order", 1);
                    rule.put("status", 1);
                    rule.put("errorMsg", "filename+url 已存在");
                    baseService.save("column_check_property", rule);
                    LOGGER.info("已为 {} 写入唯一性校验规则", table);
                }
            }
        } catch (Exception e) {
            LOGGER.warn("自动生成配置失败: {}", table, e);
        }
    }

    private void initAiProcessorConfigs() {
        try {
            String procName = "ImageDescriptionProcessor";
            Map<String, Object> existing = baseService.getOne("ai_processor_config", Map.of("processor_name", procName));
            if (existing == null) {
                Map<String, Object> row = new HashMap<>();
                row.put("processor_name", procName);
                row.put("class_name", "com.ycbd.demo.plugin.aiprocessor.processors.ImageDescriptionProcessor");
                row.put("description", "图片描述生成");
                row.put("is_active", true);
                baseService.save("ai_processor_config", row);
                LOGGER.info("已写入 ai_processor_config: {}", procName);
            }
        } catch (Exception e) {
            LOGGER.warn("初始化 ai_processor_config 失败", e);
        }
    }
}
