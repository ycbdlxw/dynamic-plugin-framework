package com.ycbd.demo.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;

/**
 * 日期工具类，处理各种日期格式转换和提取
 */
public class DateUtils {

    private static final Logger logger = LoggerFactory.getLogger(DateUtils.class);

    /**
     * 将各种格式的日期字符串转换为标准格式 (yyyy-MM-dd HH:mm:ss)
     */
    public static String convertToStandardDateTime(String dateString) {
        Properties properties = new Properties();
        try (InputStream inputStream = ResourceUtil.getStream("dateformats.properties")) {
            properties.load(inputStream);
        } catch (IOException e) {
            logger.error("加载日期格式配置文件失败", e);
            return "";
        }

        for (String key : properties.stringPropertyNames()) {
            String pattern = properties.getProperty(key);
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
                LocalDateTime dateTime = LocalDateTime.parse(dateString, formatter);
                DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                return dateTime.format(outputFormatter);
            } catch (DateTimeParseException e) {
                // 忽略并尝试下一个格式
            }
        }

        // 如果没有匹配的格式，返回空字符串
        return "";
    }

    /**
     * 从文件名中提取日期时间
     */
    public static String extractDateTime(String inputString) {
        // 直接通过正则匹配日期时间
        String dateTimeString = "";

        // 定义正则表达式，匹配日期时间格式
        String dateTimePattern = "\\d{8}_\\d{6}";
        String formatStr = "yyyyMMdd_HHmmss";
        if (inputString.contains("-")) {
            dateTimePattern = "\\d{8}-\\d{6}";
            formatStr = "yyyyMMdd-HHmmss";
        }

        dateTimeString = inputString.replaceAll(".*(" + dateTimePattern + ").*", "$1");
        if (dateTimeString.equals(inputString)) {
            return "";
        }

        try {
            DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern(formatStr);
            DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime dateTime = LocalDateTime.parse(dateTimeString, inputFormatter);
            return dateTime.format(outputFormatter);
        } catch (Exception e) {
            // 如果解析失败，尝试直接使用 DateUtil
            try {
                return DateUtil.format(DateUtil.parse(dateTimeString, formatStr), "yyyy-MM-dd HH:mm:ss");
            } catch (Exception ex) {
                return "";
            }
        }
    }

    /**
     * 从文件名中提取日期时间，支持时间戳格式
     */
    public static String extractDateTimeFromFileName(String filename) {
        String dateTimeStr = extractDateTime(filename);
        if (StrUtil.isNotEmpty(dateTimeStr)) {
            return dateTimeStr;
        }
        // 正则表达式匹配13位或11位数字时间戳
        String timestampPattern = "(\\d{13}|\\d{11})";
        Pattern pattern = Pattern.compile(timestampPattern);
        Matcher matcher = pattern.matcher(filename);

        if (matcher.find()) {
            String timestampStr = matcher.group(1);
            try {
                // 将时间戳转换为日期时间
                // 由于时间戳可能是毫秒级或秒级，需要根据长度判断
                long timestamp = Long.parseLong(timestampStr);
                if (timestampStr.length() == 10) {
                    timestamp *= 1000; // 秒转毫秒
                }

                return DateUtil.format(new java.util.Date(timestamp), "yyyy-MM-dd HH:mm:ss");
            } catch (Exception e) {
                logger.error("时间戳转换失败", e);
                return "";
            }
        }

        return "";
    }

    /**
     * 从路径中提取日期
     */
    public static String extractDateFromPath(String path) {
        // 统一使用正斜杠分隔，兼容不同平台路径
        String normalizedPath = path.replace("\\", "/");
        String[] pathItems = StrUtil.splitToArray(normalizedPath, '/');

        for (String pathItem : pathItems) {
            if (isValidDate(pathItem)) {
                return pathItem;
            }
        }
        return "";
    }

    /**
     * 检查字符串是否为有效的日期格式
     */
    public static boolean isValidDate(String dateStr) {
        if (dateStr.length() == 8) {
            // 检查是否为有效的yyyyMMdd格式
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
            try {
                dateFormat.parse(dateStr);
                return true;
            } catch (ParseException e) {
                return false;
            }
        } else if (dateStr.length() == 6) {
            // 检查是否为有效的yyyyMM格式
            SimpleDateFormat monthFormat = new SimpleDateFormat("yyyyMM");
            try {
                monthFormat.parse(dateStr);
                return true;
            } catch (ParseException e) {
                return false;
            }
        } else if (dateStr.length() == 4) {
            // 检查是否为有效的年份
            try {
                int year = Integer.parseInt(dateStr);
                return year >= 1900 && year <= 2100; // 合理的年份范围
            } catch (NumberFormatException e) {
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * 根据文件创建日期时间获取
     */
    public static String extractDateTimeByFile(String filepath) {
        try {
            // 获取文件路径
            Path file = Path.of(filepath);
            // 读取文件属性
            BasicFileAttributes attributes = Files.readAttributes(file, BasicFileAttributes.class);
            // 获取文件创建时间
            LocalDateTime dateTime = LocalDateTime.ofInstant(attributes.creationTime().toInstant(),
                    ZoneId.systemDefault());
            // 设置日期时间格式
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            // 格式化日期时间
            return dateTime.format(formatter);
        } catch (Exception e) {
            logger.error("获取文件创建时间失败", e);
            return "";
        }
    }

    /**
     * 拆分日期时间字符串为年月日数组
     */
    public static String[] splitDateTime(String dateTimeStr) {
        // 检查字符串长度是否为8位
        if (dateTimeStr.length() != 8) {
            return new String[]{"1990", "00", "00"};
        }

        // 拆分年月日
        String year = dateTimeStr.substring(0, 4);
        String month = dateTimeStr.substring(4, 6);
        String day = dateTimeStr.substring(6, 8);

        return new String[]{year, month, day};
    }

    /**
     * 工具方法：尝试从文件名、文件路径或文件属性中获取日期时间。 优先级：文件名 -> 路径 -> 文件属性创建时间
     */
    public static String extractDateByFileOrPath(String filePath) {
        // 1. 从路径中的日期片段解析
        String dateFromPath = extractDateFromPath(filePath);
        if (StrUtil.isNotEmpty(dateFromPath) && dateFromPath.length() == 8) {
            // 规范到 yyyy-MM-dd HH:mm:ss
            String[] ymd = splitDateTime(dateFromPath);
            return StrUtil.join("-", ymd[0], ymd[1], ymd[2]) + " 00:00:00";
        }
        // 2. 使用文件创建时间
        String dateTime = extractDateTimeByFile(filePath);
        if (StrUtil.isNotEmpty(dateTime)) {
            return dateTime;
        }
        return "";
    }
}
