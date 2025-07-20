package com.ycbd.demo.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



import cn.hutool.core.util.StrUtil;

/**
 * 文件工具类，处理文件路径、类型判断等操作
 */
public class FileUtils {


    // 常用应用目录前缀
    private static final String[] APP_PREFIXES = {"WeiXin", "Screenshots", "qq", "down", "Pictures"};

    // 中文字符匹配模式
    private static final Pattern CHINESE_PATTERN = Pattern.compile("[\u4e00-\u9fa5]+");

    /**
     * 根据文件名获取可能的应用模型
     */
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

    /**
     * 提取文件路径中的中文部分
     */
    public static String extractChineseFromPath(String path) {
        List<String> chineseParts = new ArrayList<>();
        String normalizedPath = path.replace("\\", "/");
        String[] pathElements = StrUtil.splitToArray(normalizedPath, '/');

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

    /**
     * 获取目标文件路径
     */
    public static String getTargetFilePath(String make, String model, String originalDate) {
        if (StrUtil.isEmpty(originalDate) || originalDate.trim().length() < 10) {
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

        return processedPath + File.separator + month + File.separator + originalDate;
    }

    /**
     * 将文件路径规范化（统一使用正斜杠）
     */
    public static String normalizePath(String filePath) {
        // 将Windows路径分隔符转换为Linux路径分隔符，以便正则表达式可以正确匹配
        return filePath.replace("\\", "/");
    }

    /**
     * 根据文件路径生成关键信息Map keys: sourceFile, fileName, fileExt, fileType,
     * targetPath, targetFile
     */
    public static Map<String, Object> getListString(String filePath, String rootString,
            String imagesExt, String videoExt) {
        Map<String, Object> info = new HashMap<>(8);

        File src = new File(filePath);
        String fileName = src.getName();
        String fileExt = "";
        int dot = fileName.lastIndexOf('.');
        if (dot > -1 && dot < fileName.length() - 1) {
            fileExt = fileName.substring(dot + 1).toLowerCase();
        }

        // 计算文件类型
        String fileType = isImageOrVideoFile(fileName, imagesExt, videoExt);

        // 规范 rootString
        if (!rootString.endsWith(File.separator)) {
            rootString = rootString + File.separator;
        }

        // 相对路径（去掉前缀）
        String relative = filePath.startsWith(rootString) ? filePath.substring(rootString.length()) : fileName;
        String dir = new File(relative).getParent();
        if (dir == null) {
            dir = "";
        }

        String targetPath = rootString + "thumbnails" + File.separator + dir;
        String targetFile = targetPath + File.separator + fileName;

        info.put("sourceFile", filePath);
        info.put("fileName", fileName);
        info.put("fileExt", fileExt);
        info.put("fileType", fileType);
        info.put("targetPath", targetPath);
        info.put("targetFile", targetFile);

        return info;
    }

    /**
     * 判断文件类型，imagesExt 与 videoExt 逗号分隔，如 "jpg,png,gif", "mp4,avi"
     */
    public static String isImageOrVideoFile(String fileName, String imagesExt, String videoExt) {
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
     * 将文件绝对路径拆分为root与path两部分 root为挂载点目录（UNIX取前两级目录，Windows取盘符）
     * path为去掉root后的剩余部分
     */
    public static Map<String, String> splitRootAndPath(String filePath) {
        Map<String, String> result = new HashMap<>(2);
        if (StrUtil.isBlank(filePath)) {
            result.put("root", "");
            result.put("path", "");
            return result;
        }

        String normalized = filePath.replace("\\", "/");

        // Windows路径，如C:/Users/xxx/file.jpg
        int colonIdx = normalized.indexOf(":/");
        if (colonIdx > 0) {
            String drive = normalized.substring(0, colonIdx + 2); // 包含盘符和斜杠，如C:/
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

        // Unix路径：/mnt/usb/photo/xxx.jpg 取前两级作为root
        String[] parts = StrUtil.splitToArray(normalized, '/');
        if (parts != null && parts.length >= 2) {
            String root = "/" + parts[0] + "/" + parts[1];
            StringBuilder sb = new StringBuilder();
            for (int i = 2; i < parts.length; i++) {
                sb.append(parts[i]);
                if (i != parts.length - 1) {
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
}
