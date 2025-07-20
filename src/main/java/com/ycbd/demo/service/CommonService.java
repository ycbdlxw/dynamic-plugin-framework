package com.ycbd.demo.service;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ycbd.demo.utils.ApiResponse;
import com.ycbd.demo.utils.ResultCode;
import com.ycbd.demo.mapper.SystemMapper;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;

@Service
public class CommonService {

    private static final Logger logger = LoggerFactory.getLogger(CommonService.class);

    @Autowired
    private BaseService baseService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ValidationService validationService;
    
    @Autowired
    private TokenFieldConfigService tokenFieldConfigService;
 

    public ApiResponse<Map<String, Object>> getList(String targetTable, Map<String, Object> params) {
        if (StrUtil.isEmpty(targetTable)) {
            return ApiResponse.failed("targetTable不能为空");
        }
        Map<String, Object> tableConfig = baseService.getTableConfig(targetTable);
        String sortByAndType = MapUtil.getStr(params, "sortByAndType", MapUtil.getStr(tableConfig, "sort"));

        // 获取字段列表，避免硬编码
        String columns = "*"; // 默认查询所有字段

        // 从参数中移除非查询条件的参数
        Map<String, Object> queryParams = new HashMap<>(params);
        queryParams.remove("targetTable");
        queryParams.remove("sortByAndType");
        queryParams.remove("pageIndex");
        queryParams.remove("pageSize");

        // 获取分页参数
        int pageIndex = MapUtil.getInt(params, "pageIndex", 0);
        int pageSize = MapUtil.getInt(params, "pageSize", 100);

        List<Map<String, Object>> items = baseService.queryList(targetTable, pageIndex, pageSize, columns, queryParams,
                "", sortByAndType, null);
        int total = baseService.count(targetTable, queryParams, "");
        Map<String, Object> data = MapUtil.newHashMap();
        data.put("items", items);
        data.put("total", total);
        return ApiResponse.success(data);
    }

    @Transactional
    public ApiResponse<Map<String, Object>> saveData(String targetTable, Map<String, Object> data) {
        validateAttributes(targetTable, data);
        Integer id = MapUtil.getInt(data, "id");
        boolean isUpdate = id != null && id > 0;

        // 数据校验
        List<String> errors = validationService.validate(targetTable, data);
        if (!errors.isEmpty()) {
            return ApiResponse.failed(String.join("; ", errors));
        }

        // 预处理
        preProcessData(targetTable, data, isUpdate);

        if (isUpdate) {
            // 自动补全所有 NOT NULL 字段，防止部分字段未传导致数据库报错
            Map<String, Object> old = baseService.getOne(targetTable, MapUtil.of("id", id));
            if (old != null) {
                for (Map.Entry<String, Object> entry : old.entrySet()) {
                    String key = entry.getKey();
                    if (!data.containsKey(key) && entry.getValue() != null) {
                        data.put(key, entry.getValue());
                    }
                }
            }
            baseService.update(targetTable, data, id);
            Map<String, Object> result = MapUtil.newHashMap();
            result.put("id", id);
            return ApiResponse.success(result);
        } else {
            long newId = baseService.save(targetTable, data);
            Map<String, Object> result = MapUtil.newHashMap();
            result.put("id", newId);
            return ApiResponse.success(result);
        }
    }

    @Transactional
    public ApiResponse<Object> deleteData(String targetTable, Integer id) {
        if (id == null || id <= 0) {
            return ApiResponse.failed("ID无效");
        }
        baseService.delete(targetTable, id);
        return ApiResponse.success();
    }

    @Transactional
    public ApiResponse<Object> batchSaveData(String targetTable, List<Map<String, Object>> saveData) {
        if (saveData == null || saveData.isEmpty()) {
            return ApiResponse.success();
        }
        for (Map<String, Object> data : saveData) {
            logger.debug("[batchSaveData] 校验数据: {}", data);
            validateAttributes(targetTable, data);
            // 数据校验
            List<String> errors = validationService.validate(targetTable, data);
            if (!errors.isEmpty()) {
                logger.warn("[batchSaveData] 业务校验失败: {}", errors);
                return ApiResponse.failed(String.join("; ", errors));
            }
            preProcessData(targetTable, data, false);
        }
        baseService.saveBatch(targetTable, saveData);
        return ApiResponse.success();
    }

    /**
     * 批量删除
     */
    @Transactional
    public ApiResponse<Object> deleteBatchData(String targetTable, List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return ApiResponse.success();
        }
        for (Integer id : ids) {
            deleteData(targetTable, id);
        }
        return ApiResponse.success();
    }

    /**
     * 用户登录 支持动态Token字段
     */
    public ApiResponse<Map<String, Object>> login(Map<String, Object> credentials) {
        String username = MapUtil.getStr(credentials, "username");
        String password = MapUtil.getStr(credentials, "password");
        if (StrUtil.isEmpty(username) || StrUtil.isEmpty(password)) {
            return ApiResponse.failed("用户名和密码不能为空");
        }
        try {
            // 从数据库查询用户信息
           credentials.remove("password");
            credentials.put("status", 1);
            Map<String, Object> user=baseService.getOne("sys_user",credentials);
            if (user == null) {
                logger.warn("用户不存在: {}", username);
                return ApiResponse.failed("用户名错误");
            }
            String hashedPassword = MapUtil.getStr(user, "password");
            boolean passwordValid = !StrUtil.isEmpty(hashedPassword) && BCrypt.checkpw(password, hashedPassword);
            if (!passwordValid) {
                logger.warn("密码验证失败: {}", username);
                return ApiResponse.failed("密码错误");
            }
            List<Map<String, Object>> userAttrs = baseService.getColumnAttributes("sys_user", null);
            //通过tokenFieldConfigService 获取动态的用于组成token信息的字段，然后将这些字段内容提取出来为tokenUserInfo数据，生成token
            Map<String, Object> tokenUserInfo = tokenFieldConfigService.processTokenValue(userAttrs,user);
            // 生成JWT令牌
            String token = jwtService.generateToken(tokenUserInfo);
            user.put("token", token);
            return ApiResponse.success(user);
        } catch (Exception e) {
            logger.error("登录过程中发生错误", e);
            // 创建一个包含错误信息的Map
            Map<String, Object> errorData = MapUtil.newHashMap();
            errorData.put("error", "登录过程中发生错误，请稍后再试");
            errorData.put("errorDetail", e.getMessage());
            return ApiResponse.of(ResultCode.INTERNAL_ERROR, errorData);
        }
    }

    private void preProcessData(String targetTable, Map<String, Object> data, boolean isUpdate) {
        // 密码加密
        if (data.containsKey("password") && StrUtil.isNotEmpty(MapUtil.getStr(data, "password"))) {
            data.put("password", BCrypt.hashpw(MapUtil.getStr(data, "password")));
        } else {
            data.remove("password"); // 更新时不传密码则不修改
        }

       
    }

    /**
     * 字段属性校验，用于保存和更新数据时校验字段属性
     *
     * @param table
     * @param data
     */
    public void validateAttributes(String table, Map<String, Object> data) {
        List<Map<String, Object>> attrs = baseService.getColumnAttributes(table, null);
        for (Map<String, Object> attr : attrs) {
            String field = MapUtil.getStr(attr, "column_name");
            boolean required = Boolean.TRUE.equals(attr.get("is_required")) || "1".equals(String.valueOf(attr.get("is_required")));
            String dataType = MapUtil.getStr(attr, "field_type");
            Integer maxLength = attr.get("len") != null ? Integer.parseInt(attr.get("len").toString()) : null;

            Object value = data.get(field);
            logger.debug("字段校验: {} 必填={} 当前值={}", field, required, value);

            if (required && (value == null || (value instanceof String && ((String) value).trim().isEmpty()))) {
                throw new IllegalArgumentException("字段[" + field + "]为必填项，不能为空！");
            }
            // 类型校验
            if (value != null && !value.toString().isEmpty()) {
                if ("number".equalsIgnoreCase(dataType)) {
                    try {
                        new java.math.BigDecimal(value.toString());
                    } catch (Exception e) {
                        throw new IllegalArgumentException("字段[" + field + "]必须为数字！");
                    }
                }
                if ("date".equalsIgnoreCase(dataType) || "datetime".equalsIgnoreCase(dataType)) {
                    try {
                        java.time.Instant.parse(value.toString());
                    } catch (Exception e) {
                        throw new IllegalArgumentException("字段[" + field + "]必须为日期格式(ISO8601)！");
                    }
                }
                // 可扩展更多类型
            }
            // 长度校验
            if (maxLength != null && value != null && value.toString().length() > maxLength) {
                throw new IllegalArgumentException("字段[" + field + "]长度不能超过" + maxLength + "！");
            }
        }
    }
}
