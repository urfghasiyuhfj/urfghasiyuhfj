package com.ppm.dto.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 基地供货量 Excel 行（如重庆供货量2510 工作表）
 * <p>使用 String 类型接收数字字段，避免 Excel 格式问题导致的转换错误</p>
 */
@Data
public class SupplyVolumeExcelRow {

    @ExcelProperty("fiscal_year")
    private String fiscalYear;

    @ExcelProperty(value = "base_code")
    private String baseCode;

    @ExcelProperty("supplier_code")
    private String supplierCode;

    @ExcelProperty("supplier_name")
    private String supplierName;

    @ExcelProperty("part_code")
    private String partCode;

    @ExcelProperty("part_name")
    private String partName;

    @ExcelProperty("plant_id")
    private String plantId;

    @ExcelProperty("month_1_no")
    private String month1No;

    @ExcelProperty("month_2_no")
    private String month2No;

    @ExcelProperty("month_3_no")
    private String month3No;

    @ExcelProperty("month_4_no")
    private String month4No;

    @ExcelProperty("month_5_no")
    private String month5No;

    @ExcelProperty("month_6_no")
    private String month6No;

    @ExcelProperty("month_7_no")
    private String month7No;

    @ExcelProperty("month_8_no")
    private String month8No;

    @ExcelProperty("month_9_no")
    private String month9No;

    @ExcelProperty("month_10_no")
    private String month10No;

    @ExcelProperty("month_11_no")
    private String month11No;

    @ExcelProperty("month_12_no")
    private String month12No;

    @ExcelProperty("supplier_code&part_code")
    private String supplierPart;

    /**
     * 安全解析 Double 值，处理空值、空白字符串和非数字格式
     */
    public static Double parseDoubleSafe(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String cleaned = value.trim().replace(",", "");
        try {
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public Double getFiscalYearDouble() {
        return parseDoubleSafe(fiscalYear);
    }

    public Double getMonth1NoDouble() {
        return parseDoubleSafe(month1No);
    }

    public Double getMonth2NoDouble() {
        return parseDoubleSafe(month2No);
    }

    public Double getMonth3NoDouble() {
        return parseDoubleSafe(month3No);
    }

    public Double getMonth4NoDouble() {
        return parseDoubleSafe(month4No);
    }

    public Double getMonth5NoDouble() {
        return parseDoubleSafe(month5No);
    }

    public Double getMonth6NoDouble() {
        return parseDoubleSafe(month6No);
    }

    public Double getMonth7NoDouble() {
        return parseDoubleSafe(month7No);
    }

    public Double getMonth8NoDouble() {
        return parseDoubleSafe(month8No);
    }

    public Double getMonth9NoDouble() {
        return parseDoubleSafe(month9No);
    }

    public Double getMonth10NoDouble() {
        return parseDoubleSafe(month10No);
    }

    public Double getMonth11NoDouble() {
        return parseDoubleSafe(month11No);
    }

    public Double getMonth12NoDouble() {
        return parseDoubleSafe(month12No);
    }
}
