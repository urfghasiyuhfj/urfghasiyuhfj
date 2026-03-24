package com.ppm.util;

import java.util.Collection;
import java.util.regex.Pattern;

/**
 * 字符串工具类
 */
public class StringUtils {

    private static final Pattern NUMBER_PATTERN = Pattern.compile("^\\d+$");
    private static final Pattern MONTH_PATTERN = Pattern.compile("^20\\d{2}(0[1-9]|1[0-2])$");

    private StringUtils() {
    }

    /**
     * 判断字符串是否为空
     */
    public static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

    /**
     * 判断字符串是否不为空
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    /**
     * 判断字符串是否为空或空白
     */
    public static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * 判断字符串是否不为空且不为空白
     */
    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }

    /**
     * 去除字符串两端空白，如果为空则返回null
     */
    public static String trimToNull(String str) {
        if (str == null) {
            return null;
        }
        String trimmed = str.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * 去除字符串两端空白，如果为空则返回空字符串
     */
    public static String trimToEmpty(String str) {
        return str == null ? "" : str.trim();
    }

    /**
     * 截断字符串到最大长度
     */
    public static String truncate(String str, int maxLength) {
        if (str == null) {
            return null;
        }
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength);
    }

    /**
     * 判断字符串是否为纯数字
     */
    public static boolean isNumeric(String str) {
        if (isEmpty(str)) {
            return false;
        }
        return NUMBER_PATTERN.matcher(str).matches();
    }

    /**
     * 判断字符串是否为有效的月份格式 (yyyyMM)
     */
    public static boolean isValidMonth(String str) {
        if (isEmpty(str)) {
            return false;
        }
        // 去除可能的 .0 后缀
        String normalized = str.trim().replace(".0", "");
        return MONTH_PATTERN.matcher(normalized).matches();
    }

    /**
     * 标准化月份字符串
     */
    public static String normalizeMonth(String monthStr) {
        if (monthStr == null) {
            return null;
        }
        String trimmed = monthStr.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        // 去除可能的 .0 后缀
        return trimmed.replace(".0", "");
    }

    /**
     * 将字符串首字母大写
     */
    public static String capitalize(String str) {
        if (isEmpty(str)) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    /**
     * 将字符串首字母小写
     */
    public static String uncapitalize(String str) {
        if (isEmpty(str)) {
            return str;
        }
        return str.substring(0, 1).toLowerCase() + str.substring(1);
    }

    /**
     * 拼接字符串集合
     */
    public static String join(Collection<?> collection, String separator) {
        if (collection == null || collection.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Object item : collection) {
            if (!first) {
                sb.append(separator);
            }
            sb.append(item);
            first = false;
        }
        return sb.toString();
    }

    /**
     * 格式化供应商信息
     */
    public static String formatSupplierInfo(String supplierCode, String supplierName) {
        if (isNotEmpty(supplierName)) {
            return supplierName + "(" + supplierCode + ")";
        }
        return supplierCode != null ? supplierCode : "未知";
    }
}
