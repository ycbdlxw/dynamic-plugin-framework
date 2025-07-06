package com.ycbd.demo.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SystemMapper {

    /**
     * 获取数据列表
     */
    List<Map<String, Object>> getItemsData(
            @Param("table") String table,
            @Param("columns") String columns,
            @Param("joinString") String joinString,
            @Param("whereStr") String whereStr,
            @Param("groupByString") String groupByString,
            @Param("sortByAndType") String sortByAndType,
            @Param("pageSize") int pageSize,
            @Param("pageIndex") int pageIndex
    );

    /**
     * 获取数据总数
     */
    int getDataCount(
            @Param("table") String table,
            @Param("joinString") String joinString,
            @Param("whereStr") String whereStr
    );

    /**
     * 获取表属性
     */
    Map<String, Object> getAttributeData(@Param("table") String table);

    /**
     * 获取列属性
     */
    List<Map<String, Object>> getColumnAttributes(
            @Param("table") String table,
            @Param("attributeType") String attributeType
    );

    /**
     * 获取校验规则
     */
    List<Map<String, Object>> getValidationRules(@Param("tableName") String tableName);

    /**
     * 获取主键列名
     */
    String getPriKeyColumn(@Param("table") String table);

    /**
     * 插入数据
     */
    int insertData(
            @Param("table") String table,
            @Param("data") Map<String, Object> data
    );

    /**
     * 批量插入
     */
    int insertDataBath(
            @Param("table") String table,
            @Param("columns") String columns,
            @Param("saveData") List<Map<String, Object>> saveData
    );

    /**
     * 更新数据
     */
    int updateData(
            @Param("table") String table,
            @Param("data") Map<String, Object> data,
            @Param("primaryKey") String primaryKey,
            @Param("id") Object id
    );

    /**
     * 批量更新
     */
    int updateDataBatch(
            @Param("table") String table,
            @Param("data") Map<String, Object> data,
            @Param("primaryKey") String primaryKey,
            @Param("ids") List<Object> ids
    );

    /**
     * 删除数据
     */
    int deleteData(
            @Param("table") String table,
            @Param("primaryKey") String primaryKey,
            @Param("id") Object id
    );

    /**
     * 批量删除
     */
    int deleteDataBatch(
            @Param("table") String table,
            @Param("primaryKey") String primaryKey,
            @Param("ids") List<Object> ids
    );

    /**
     * 查询数据库元数据
     */
    List<Map<String, Object>> selectSchema(
            @Param("schemaName") String schemaName,
            @Param("tableName") String tableName
    );

    /**
     * 获取用户完整信息（包含角色、权限、组织机构等）
     */
    Map<String, Object> getUserWithDetails(@Param("username") String username);
}
