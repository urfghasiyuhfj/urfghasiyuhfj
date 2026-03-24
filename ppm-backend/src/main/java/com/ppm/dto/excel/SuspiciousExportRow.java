package com.ppm.dto.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import lombok.Data;

import java.util.Date;

/**
 * 可疑物料导出 Excel 行
 */
@Data
public class SuspiciousExportRow {

    @ExcelProperty("区域工厂")
    private String plant;

    @ExcelProperty("录入日期")
    @DateTimeFormat("yyyy-MM-dd")
    private Date recordDate;

    @ExcelProperty("零件号")
    private String partCode;

    @ExcelProperty("零件名称")
    private String partName;

    @ExcelProperty("供应商代码")
    private String supplierCode;

    @ExcelProperty("供应商名称")
    private String supplierName;

    @ExcelProperty("数量")
    private Integer quantity;

    @ExcelProperty("故障类别")
    private String faultType;

    @ExcelProperty("开单日期")
    @DateTimeFormat("yyyy-MM-dd")
    private Date orderDate;

    @ExcelProperty("供应商责任")
    private String supplierResp;
}
