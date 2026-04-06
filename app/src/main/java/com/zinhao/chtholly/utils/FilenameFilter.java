package com.zinhao.chtholly.utils;

import java.util.regex.Pattern;

public class FilenameFilter {

    // Windows 保留字符: < > : " / \ | ? *
    private static final String WINDOWS_INVALID_CHARS = "<>:\"/\\|?*";

    // 控制字符 (0x00-0x1F) 和 0x7F
    private static final Pattern CONTROL_CHARS = Pattern.compile("[\\x00-\\x1f\\x7f]");

    // Windows 保留设备名
    private static final String[] WINDOWS_RESERVED_NAMES = {
            "CON", "PRN", "AUX", "NUL",
            "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
            "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"
    };

    /**
     * 基础过滤 - 移除所有无效字符（跨平台安全）
     */
    public static String sanitizeBasic(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "unnamed";
        }

        StringBuilder sb = new StringBuilder();
        for (char c : filename.toCharArray()) {
            // 过滤控制字符和 Windows 保留字符
            if (c < 0x20 || c == 0x7f || WINDOWS_INVALID_CHARS.indexOf(c) >= 0) {
                continue;
            }
            sb.append(c);
        }

        String result = sb.toString();

        // 处理 Windows 保留设备名
        String upper = result.toUpperCase();
        for (String reserved : WINDOWS_RESERVED_NAMES) {
            if (upper.equals(reserved) || upper.startsWith(reserved + ".")) {
                result = "_" + result;
                break;
            }
        }

        // 去除首尾空格和点
        result = result.trim().replaceAll("^[.]+|[.]+$", "");

        // 空文件名处理
        if (result.isEmpty()) {
            return "unnamed";
        }

        return result;
    }

    /**
     * 智能过滤 - 将无效字符替换为指定字符（默认下划线）
     */
    public static String sanitizeWithReplacement(String filename, char replacement) {
        if (filename == null || filename.isEmpty()) {
            return "unnamed";
        }

        StringBuilder sb = new StringBuilder();
        for (char c : filename.toCharArray()) {
            if (c < 0x20 || c == 0x7f || WINDOWS_INVALID_CHARS.indexOf(c) >= 0) {
                sb.append(replacement);
            } else {
                sb.append(c);
            }
        }

        String result = sb.toString();

        // 处理保留名
        String upper = result.toUpperCase();
        for (String reserved : WINDOWS_RESERVED_NAMES) {
            if (upper.equals(reserved)) {
                result = result + replacement;
                break;
            }
        }

        result = result.trim();
        if (result.isEmpty()) {
            return "unnamed";
        }

        return result;
    }

    /**
     * 严格过滤 - 仅保留字母数字、中文、常用符号
     */
    public static String sanitizeStrict(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "unnamed";
        }

        // 保留: 字母数字、中文、日文、韩文、常用符号、空格
        String result = filename.replaceAll("[^\\w\\u4e00-\\u9fa5\\u3040-\\u309f\\u30a0-\\u30ff\\uac00-\\ud7af\\-\\s.]", "_");

        // 去除连续下划线
        result = result.replaceAll("_+", "_");

        // 处理首尾
        result = result.trim().replaceAll("^[._]+|[._]+$", "");

        if (result.isEmpty()) {
            return "unnamed";
        }

        return result;
    }

    /**
     * 路径安全过滤 - 同时过滤路径分隔符，防止目录遍历
     */
    public static String sanitizePathSafe(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "unnamed";
        }

        // 过滤所有路径相关字符
        String result = filename
                .replace('\\', '_')
                .replace('/', '_')
                .replace("..", "__");

        return sanitizeBasic(result);
    }

}
