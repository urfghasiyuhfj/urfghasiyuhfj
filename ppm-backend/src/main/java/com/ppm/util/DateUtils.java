package com.ppm.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 日期工具类
 */
public class DateUtils {

    public static final String DATE_PATTERN = "yyyy-MM-dd";
    public static final String DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    public static final String MONTH_PATTERN = "yyyyMM";
    public static final String MONTH_PATTERN_HYPHEN = "yyyy-MM";

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_PATTERN);
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern(DATETIME_PATTERN);
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern(MONTH_PATTERN);
    private static final DateTimeFormatter MONTH_FORMATTER_HYPHEN = DateTimeFormatter.ofPattern(MONTH_PATTERN_HYPHEN);

    private DateUtils() {
    }

    /**
     * 格式化日期为 yyyy-MM-dd
     */
    public static String formatDate(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.format(DATE_FORMATTER);
    }

    /**
     * 格式化日期时间为 yyyy-MM-dd HH:mm:ss
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(DATETIME_FORMATTER);
    }

    /**
     * 格式化月份为 yyyyMM
     */
    public static String formatMonth(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.format(MONTH_FORMATTER);
    }

    /**
     * 解析日期字符串
     */
    public static LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("日期格式错误，应为 yyyy-MM-dd: " + dateStr);
        }
    }

    /**
     * 解析月份字符串 yyyyMM
     */
    public static String parseMonth(String monthStr) {
        if (monthStr == null || monthStr.isBlank()) {
            return null;
        }
        // 去除可能的 .0 后缀
        String normalized = monthStr.trim().replace(".0", "");
        if (!normalized.matches("\\d{6}")) {
            throw new IllegalArgumentException("月份格式错误，应为 yyyyMM: " + monthStr);
        }
        return normalized;
    }

    /**
     * 获取当前年月字符串 yyyyMM
     */
    public static String getCurrentMonth() {
        return LocalDate.now().format(MONTH_FORMATTER);
    }

    /**
     * 获取上一个月
     */
    public static String getPreviousMonth(String monthStr) {
        String month = parseMonth(monthStr);
        int year = Integer.parseInt(month.substring(0, 4));
        int monthVal = Integer.parseInt(month.substring(4, 6));
        if (monthVal == 1) {
            year--;
            monthVal = 12;
        } else {
            monthVal--;
        }
        return String.format("%04d%02d", year, monthVal);
    }

    /**
     * 获取下一个月
     */
    public static String getNextMonth(String monthStr) {
        String month = parseMonth(monthStr);
        int year = Integer.parseInt(month.substring(0, 4));
        int monthVal = Integer.parseInt(month.substring(4, 6));
        if (monthVal == 12) {
            year++;
            monthVal = 1;
        } else {
            monthVal++;
        }
        return String.format("%04d%02d", year, monthVal);
    }
}
