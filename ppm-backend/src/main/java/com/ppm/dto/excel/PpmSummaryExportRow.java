package com.ppm.dto.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * PPM 汇总导出 Excel 行
 */
@Data
public class PpmSummaryExportRow {

    @ExcelProperty("月份")
    private String ppmMonth;

    @ExcelProperty("基地编码")
    private String baseCode;

    @ExcelProperty("基地名称")
    private String baseName;

    @ExcelProperty("供应商编码")
    private String supplierCode;

    @ExcelProperty("供应商名称")
    private String supplierName;

    @ExcelProperty("不合格数")
    private Integer defectCount;

    @ExcelProperty("供货量")
    private Integer supplyQty;

    @ExcelProperty("PPM")
    private BigDecimal ppm;
}
