package com.ppm.dto.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 供货量导出 Excel 行
 */
@Data
public class SupplyExportRow {

    @ExcelProperty("年份")
    private Short fiscalYear;

    @ExcelProperty("基地")
    private String baseCode;

    @ExcelProperty("工厂编码")
    private String plantId;

    @ExcelProperty("供应商代码")
    private String supplierCode;

    @ExcelProperty("供应商名称")
    private String supplierName;

    @ExcelProperty("零件号")
    private String partCode;

    @ExcelProperty("零件名称")
    private String partName;

    @ExcelProperty("1月供货量")
    private Integer month1No;

    @ExcelProperty("2月供货量")
    private Integer month2No;

    @ExcelProperty("3月供货量")
    private Integer month3No;

    @ExcelProperty("4月供货量")
    private Integer month4No;

    @ExcelProperty("5月供货量")
    private Integer month5No;

    @ExcelProperty("6月供货量")
    private Integer month6No;

    @ExcelProperty("7月供货量")
    private Integer month7No;

    @ExcelProperty("8月供货量")
    private Integer month8No;

    @ExcelProperty("9月供货量")
    private Integer month9No;

    @ExcelProperty("10月供货量")
    private Integer month10No;

    @ExcelProperty("11月供货量")
    private Integer month11No;

    @ExcelProperty("12月供货量")
    private Integer month12No;
}
