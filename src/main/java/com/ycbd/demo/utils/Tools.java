package com.ycbd.demo.utils;

import java.util.List;
import java.util.Map;

import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 工具类门面，提供对其他具体工具类的访问 为保持向后兼容性，部分方法仍然保留，但内部实现委托给了专门的工具类
 */
public class Tools {

    /**
     * 将各种格式的日期字符串转换为标准格式
     */
    public static String convertToStandardDateTime(String dateString) {
        return DateUtils.convertToStandardDateTime(dateString);
    }

    /**
     * 判断是否为移动设备
     */
    public static boolean isMobileDevice(ServletRequestAttributes requestAttributes) {
        if (requestAttributes == null) {
            return false;
        }
        String userAgent = requestAttributes.getRequest().getHeader("User-Agent");
        if (userAgent == null) {
            return false;
        }
        String[] mobileKeywords = {"Mobile", "Android", "iPhone", "iPad", "Windows Phone"};

        for (String keyword : mobileKeywords) {
            if (userAgent.contains(keyword)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 获取客户端IP地址
     */
    public static String getIpAddr(ServletRequestAttributes requestAttributes) {
        if (requestAttributes == null) {
            return "";
        }
        String ip = requestAttributes.getRequest().getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0) {
            ip = requestAttributes.getRequest().getHeader("Proxy-Client-IP");
        } else if ("unknown".equalsIgnoreCase(ip)) {
            ip = requestAttributes.getRequest().getHeader("WL-Proxy-Client-IP");
        } else {
            ip = requestAttributes.getRequest().getRemoteAddr();
        }
        return ip;
    }

    /**
     * 根据文件名获取可能的应用模型
     *
     * @deprecated 请使用 FileUtils.getModel() 代替
     */
    @Deprecated
    public static String getModel(String filename) {
        return FileUtils.getModel(filename);
    }

    /**
     * 型号转换工具，符合需要转换品牌进行型号转换
     */
    public static String getConverModel(String make, String model) {
        switch (make) {
            case "Motorola":
                if (model.contains("MB525")) {
                    return "MB525";
                } else {
                    return "E398";
                }
            case "EASTMAN KODAK COMPANY":
                return "Z1508";
            case "SAMSUNG":
                return "ES55,ES57";
            default:
                break;
        }
        return "";
    }

    /**
     * 提取文件路径中的中文部分
     *
     * @deprecated 请使用 FileUtils.extractChineseFromPath() 代替
     */
    @Deprecated
    public static String extractChineseFromPath(String path) {
        return FileUtils.extractChineseFromPath(path);
    }

    /**
     * 拆分日期时间字符串为年月日数组
     *
     * @deprecated 请使用 DateUtils.splitDateTime() 代替
     */
    @Deprecated
    public static String[] splitDateTime(String dateTimeStr) {
        return DateUtils.splitDateTime(dateTimeStr);
    }

    /**
     * 获取目标文件路径
     *
     * @deprecated 请使用 FileUtils.getTargetFilePath() 代替
     */
    @Deprecated
    public static String getTargetFilePath(String make, String model, String originalDate) {
        return FileUtils.getTargetFilePath(make, model, originalDate);
    }

    /**
     * 获取两个Map之间的差异
     *
     * @deprecated 请使用 MapUtils.getDifferences() 代替
     */
    @Deprecated
    public static Map<String, Object> getDifferences(Map<String, Object> map1, Map<String, Object> map2) {
        return MapUtils.getDifferences(map1, map2);
    }

    /**
     * 合并字符串内容，去除重复项
     *
     * @deprecated 请使用 MapUtils.getUpdateContent() 代替
     */
    @Deprecated
    public static String getUpdateContent(String str1, String str2) {
        return MapUtils.getUpdateContent(str1, str2);
    }

    /**
     * 从文件名中提取日期时间
     *
     * @deprecated 请使用 DateUtils.extractDateTime() 代替
     */
    @Deprecated
    public static String extractDateTime(String inputString) {
        return DateUtils.extractDateTime(inputString);
    }

    /**
     * 从文件名中提取日期时间，支持时间戳格式
     *
     * @deprecated 请使用 DateUtils.extractDateTimeFromFileName() 代替
     */
    @Deprecated
    public static String extractDateTimeFromFileName(String filename) {
        return DateUtils.extractDateTimeFromFileName(filename);
    }

    /**
     * 从路径中提取日期
     *
     * @deprecated 请使用 DateUtils.extractDateFromPath() 代替
     */
    @Deprecated
    public static String extractDateFromPath(String path) {
        return DateUtils.extractDateFromPath(path);
    }

    /**
     * 检查字符串是否为有效的日期格式
     *
     * @deprecated 请使用 DateUtils.isValidDate() 代替
     */
    @Deprecated
    public static boolean isValidDate(String dateStr) {
        return DateUtils.isValidDate(dateStr);
    }

    /**
     * 根据文件创建日期时间获取
     *
     * @deprecated 请使用 DateUtils.extractDateTimeByFile() 代替
     */
    @Deprecated
    public static String extractDateTimeByFile(String filepath) {
        return DateUtils.extractDateTimeByFile(filepath);
    }

    /**
     * 根据文件路径生成关键信息Map
     *
     * @deprecated 请使用 FileUtils.getListString() 代替
     */
    @Deprecated
    public static Map<String, Object> getListString(String filePath, String rootString, String imagesExt, String videoExt) {
        return FileUtils.getListString(filePath, rootString, imagesExt, videoExt);
    }

    /**
     * 将文件绝对路径拆分为root与path两部分
     *
     * @deprecated 请使用 FileUtils.splitRootAndPath() 代替
     */
    @Deprecated
    public static Map<String, String> splitRootAndPath(String filePath) {
        return FileUtils.splitRootAndPath(filePath);
    }

    /**
     * 工具方法：尝试从文件名、文件路径或文件属性中获取日期时间。
     *
     * @deprecated 请使用 DateUtils.extractDateByFileOrPath() 代替
     */
    @Deprecated
    public static String extractDateByFileOrPath(String filePath) {
        return DateUtils.extractDateByFileOrPath(filePath);
    }

    /**
     * 将Map的key统一转换为小写（非递归）
     *
     * @deprecated 请使用 MapUtils.toLowerCaseKeyMap() 代替
     */
    @Deprecated
    public static Map<String, Object> toLowerCaseKeyMap(Map<String, Object> source) {
        return MapUtils.toLowerCaseKeyMap(source);
    }

    /**
     * 预处理数据，确保所有值都是MyBatis可以处理的类型
     *
     * @deprecated 请使用 MapUtils.processMapForMyBatis() 代替
     */
    @Deprecated
    public static Map<String, Object> processMapForMyBatis(Map<String, Object> data) {
        return MapUtils.processMapForMyBatis(data);
    }

    /**
     * 批量处理数据列表，确保所有值都是MyBatis可以处理的类型
     *
     * @deprecated 请使用 MapUtils.processMapListForMyBatis() 代替
     */
    @Deprecated
    public static List<Map<String, Object>> processMapListForMyBatis(List<Map<String, Object>> dataList) {
        return MapUtils.processMapListForMyBatis(dataList);
    }

    /**
     * 根据查询类型将参数值转换为字符串格式
     *
     * @deprecated 请使用 MapUtils.convertValueToString() 代替
     */
    @Deprecated
    public static String convertValueToString(Object valueObj, String queryType) {
        return MapUtils.convertValueToString(valueObj, queryType);
    }
}
