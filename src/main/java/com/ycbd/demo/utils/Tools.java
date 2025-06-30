package com.ycbd.demo.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.web.context.request.ServletRequestAttributes;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;

public class Tools {

    public static String convertToStandardDateTime(String dateString) {
        Properties properties = new Properties();
        try (InputStream inputStream = ResourceUtil.getStream("dateformats.properties")) {
            properties.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
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
                // Ignore and continue to the next formatter
            }
        }

        // Return empty string if none of the formatters match
        return "";
    }

    public static boolean isMobileDevice(ServletRequestAttributes requestAttributes) {

        if (requestAttributes == null) {
            return false;
        }
        HttpServletRequest request = requestAttributes.getRequest();
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null) {
            return false; // or handle the null case as appropriate for your use case
        }
        String[] mobileKeywords = {"Mobile", "Android", "iPhone", "iPad", "Windows Phone"};

        for (String keyword : mobileKeywords) {
            if (userAgent.contains(keyword)) {
                return true;
            }
        }

        return false;
    }

    public static String getIpAddr(ServletRequestAttributes requestAttributes) {
        if (requestAttributes == null) {
            return "";
        }
        HttpServletRequest request = requestAttributes.getRequest();
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0) {
            // ip 为 null 或空字符串
            ip = request.getHeader("Proxy-Client-IP");
        } else if ("unknown".equalsIgnoreCase(ip)) {
            // ip 等于 "unknown"
            ip = request.getHeader("WL-Proxy-Client-IP");
        } else {
            // ip 不为 null、空字符串或 "unknown"
            ip = request.getRemoteAddr();
        }
        return ip;
    }
    //model为空时，则根据目录中相关的信息获取model,如果目录中含有APP_PREFIXES相关信息，则以APP_PREFIXES为model
    //如果model还为空，则根据目录的层次，获取目录倒数第二层的目录名称为model
    //如果model还为空，则返回其它
    private static final String[] APP_PREFIXES = {"WeiXin", "Screenshots", "qq", "down", "Pictures"};

    public static String getModel(String filename) {
        String lowercaseFilename = filename.toLowerCase();
        for (String prefix : APP_PREFIXES) {
            String lowercasePrefix = prefix.toLowerCase();
            if (lowercaseFilename.contains(lowercasePrefix)) {
                switch (lowercasePrefix) {
                    case "weixin":
                        return "微信";
                    case "screenshots":
                        return "截屏";
                    case "qq":
                        return "QQ";
                    case "down":
                        return "下载";
                    case "pictures":
                        return "图片";
                    case "jiangji":
                        return "剪映";
                    default:
                        return prefix;
                }
            }
        }
        return "";
    }

    //型号转换工具，符合需要转换品牌进行型号转换
    private static final String[] converMake = {"Motorola", "EASTMAN KODAK COMPANY", "SAMSUNG"};

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
    private static final Pattern CHINESE_PATTERN = Pattern.compile("[\u4e00-\u9fa5]+");

    // 提取文件路径中的中文部分
    public static String extractChineseFromPath(String path) {
        List<String> chineseParts = new ArrayList<>();
        String normalizedPath2 = path.replace("\\", "/");
        List<String> pathElements = StrUtil.split(normalizedPath2, "/");

        for (String element : pathElements) {
            if (element.contains(".")) {
                continue;
            }
            Matcher matcher = CHINESE_PATTERN.matcher(element);
            if (matcher.find()) {
                // 提取匹配到的中文部分
                chineseParts.add(matcher.group());
            }
        }

        return String.join(",", chineseParts);
    }

    public static String[] splitDateTime(String dateTimeStr) {
        // 检查字符串长度是否为8位
        if (dateTimeStr.length() != 8) {
            return new String[]{"1990", "00", "00"};
            // 抛出异常或返回错误信息
            // throw new IllegalArgumentException("Invalid date format. Expected 'yyyyMMdd'. "+dateTimeStr);
        }

        // 拆分年月日
        String year = dateTimeStr.substring(0, 4);
        String month = dateTimeStr.substring(4, 6);
        String day = dateTimeStr.substring(6, 8);

        return new String[]{year, month, day};
    }

    public static String getTargetFilePath(String make, String model, String originalDate) {
        String result = "";
        if (StrUtil.isEmpty(originalDate)) {
            return "";
        }
        if (originalDate.trim().length() < 10) {
            return "";
        }
        String year = originalDate.substring(0, 4);
        String month = originalDate.substring(5, 7);
        // 创建目录路径，如果make为空，则不包含make目录
        String makeDir = make == null || make.trim().isEmpty() ? "" : make + "/";
        // 创建目录路径，如果model为空，则不包含model目录
        String modelDir = model == null || model.trim().isEmpty() ? "" : model + "/";

        // 整理文件路径
        String processedPath = String.format("%s%s%s", makeDir, modelDir, year);
        // 修改文件名称
        originalDate = StrUtil.replace(originalDate, ":", "-");
        originalDate = StrUtil.replace(originalDate, "-", "");
        originalDate = StrUtil.replace(originalDate, " ", "-");
        result = processedPath + File.separator + month + File.separator + originalDate;
        return result;
    }

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
     * 根据文件名称获取对应的日期时间
     *
     * @param inputString
     * @return
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

                Date date = new Date(timestamp);
                // 将日期时间式化为指定格式
                SimpleDateFormat outputFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                return outputFormatter.format(date);
            } catch (Exception e) {
                e.printStackTrace();
                return "";
            }
        }

        return "";
    }

    public static String extractDateFromPath(String path) {
        // 统一使用正斜杠分隔，兼容不同平台路径
        String normalizedPath = path.replace("\\", "/");
        List<String> pathList = StrUtil.split(normalizedPath, "/");
        List<String> dateTimeList = new ArrayList<>();

        for (String pathItem : pathList) {
            if (isValidDate(pathItem)) {
                dateTimeList.add(pathItem);
            }
        }
        if (dateTimeList.size() < 1) {
            return "";
        }
        ListUtil.sort(dateTimeList, (a, b) -> b.length() - a.length());
        return dateTimeList.get(0);
    }

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
                return year > 0;
            } catch (NumberFormatException e) {
                return false;
            }
        } else {
            return false;
        }
    }

    private static String normalizePath(String filePath) {
        // 将Windows路径分隔符转换为Linux路径分隔符，以便正则表达式可以正确匹配
        return filePath.replace("\\", "/");
    }

    /**
     * 根据文件创建日期时间获取
     *
     * @param filepath
     * @return
     */
    public static String extractDateTimeByFile(String filepath) {
        try {
            // 获取文件路径
            Path file = Path.of(filepath);
            // 读取文件属性
            BasicFileAttributes attributes = Files.readAttributes(file, BasicFileAttributes.class
            );
            // 获取文件创建时间
            LocalDateTime dateTime = LocalDateTime.ofInstant(attributes.creationTime().toInstant(),
                    ZoneId.systemDefault());
            // 设置日期时间格式
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            // 格式化日期时间
            String formattedDateTime = dateTime.format(formatter);
            return formattedDateTime;
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 根据文件路径生成关键信息 Map，而不再使用实体类。 keys: sourceFile, fileName, fileExt, fileType,
     * targetPath, targetFile
     */
    public static Map<String, Object> getListString(String filePath, String rootString,
            String imagesExt, String videoExt) {
        Map<String, Object> info = new HashMap<>(8);

        java.io.File src = new java.io.File(filePath);
        String fileName = src.getName();
        String fileExt = "";
        int dot = fileName.lastIndexOf('.');
        if (dot > -1 && dot < fileName.length() - 1) {
            fileExt = fileName.substring(dot + 1).toLowerCase();
        }

        // 计算文件类型
        String fileType = isImageOrVideoFile(fileName, imagesExt, videoExt);

        // 规范 rootString
        if (!rootString.endsWith(java.io.File.separator)) {
            rootString = rootString + java.io.File.separator;
        }

        // 相对路径（去掉前缀）
        String relative = filePath.startsWith(rootString) ? filePath.substring(rootString.length()) : fileName;
        String dir = new java.io.File(relative).getParent();
        if (dir == null) {
            dir = "";
        }

        String targetPath = rootString + "thumbnails" + java.io.File.separator + dir;
        String targetFile = targetPath + java.io.File.separator + fileName;

        info.put("sourceFile", filePath);
        info.put("fileName", fileName);
        info.put("fileExt", fileExt);
        info.put("fileType", fileType);
        info.put("targetPath", targetPath);
        info.put("targetFile", targetFile);

        return info;
    }

    /**
     * 判断文件类型，imagesExt 与 videoExt 逗号分隔，如 "jpg,png,gif", "mp4,avi"。
     */
    private static String isImageOrVideoFile(String fileName, String imagesExt, String videoExt) {
        String ext = "";
        int dotIdx = fileName.lastIndexOf('.');
        if (dotIdx > -1 && dotIdx < fileName.length() - 1) {
            ext = fileName.substring(dotIdx + 1).toLowerCase();
        }
        if (StrUtil.split(imagesExt, ",").contains(ext)) {
            return "image";
        }
        if (StrUtil.split(videoExt, ",").contains(ext)) {
            return "video";
        }
        return "other";
    }

    /**
     * 将文件绝对路径拆分为 root 与 path 两部分，其中 root 为挂载点目录（UNIX 取前两级目录，Windows 取盘符）， path
     * 为去掉 root 后的剩余部分。
     */
    public static Map<String, String> splitRootAndPath(String filePath) {
        Map<String, String> result = new HashMap<>(2);
        if (StrUtil.isBlank(filePath)) {
            result.put("root", "");
            result.put("path", "");
            return result;
        }
        String normalized = filePath.replace("\\", "/");
        // Windows 路径，如 C:/Users/xxx/file.jpg
        int colonIdx = normalized.indexOf(":/");
        if (colonIdx > 0) {
            String drive = normalized.substring(0, colonIdx + 2); // 包含盘符和斜杠，如 C:/
            String remain = normalized.substring(colonIdx + 2);
            int firstSlash = remain.indexOf('/');
            if (firstSlash > -1) {
                result.put("root", drive + remain.substring(0, firstSlash));
                result.put("path", remain.substring(firstSlash + 1));
            } else {
                result.put("root", drive + remain);
                result.put("path", "");
            }
            return result;
        }
        // Unix 路径：/mnt/usb/photo/xxx.jpg  取前两级作为 root
        java.util.List<String> parts = StrUtil.split(normalized, "/");
        if (parts != null && parts.size() >= 2) {
            String root = "/" + parts.get(0) + "/" + parts.get(1);
            StringBuilder sb = new StringBuilder();
            for (int i = 2; i < parts.size(); i++) {
                sb.append(parts.get(i));
                if (i != parts.size() - 1) {
                    sb.append("/");
                }
            }
            result.put("root", root);
            result.put("path", sb.toString());
        } else {
            // Fallback
            result.put("root", filePath);
            result.put("path", "");
        }
        return result;
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
