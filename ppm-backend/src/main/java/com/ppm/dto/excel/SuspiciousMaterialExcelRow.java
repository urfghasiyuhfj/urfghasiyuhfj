package com.ppm.dto.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import lombok.Data;

import java.util.Date;

/**
 * 可疑物料统计 Excel 行（可疑物料统计2510 工作表）
 */
@Data
public class SuspiciousMaterialExcelRow {

    @ExcelProperty("区域工厂")
    private String plant;

    @ExcelProperty("录入日期")
    @DateTimeFormat("yyyy-MM-dd")
    private Date recordDate;

    @ExcelProperty("录入人员")
    private String recorder;

    @ExcelProperty("零件号")
    private String partCode;

    @ExcelProperty("零件名称")
    private String partName;

    @ExcelProperty("功能模块")
    private String functionModule;

    @ExcelProperty("供应商名称")
    private String supplierName;

    @ExcelProperty("供应商代码")
    private String supplierCode;

    @ExcelProperty("车型|机型")
    private String modelMachine;

    @ExcelProperty("失效描述")
    private String failureDesc;

    @ExcelProperty("故障类别")
    private String faultType;

    @ExcelProperty("数量")
    private Double quantity;

    @ExcelProperty("开单日期")
    @DateTimeFormat("yyyy-MM-dd")
    private Date orderDate;

    @ExcelProperty("班次|工段")
    private String shiftSection;

    @ExcelProperty("产生区域")
    private String prodArea;

    @ExcelProperty("是否供应商责任")
    private String supplierResp;

    @ExcelProperty("备注")
    private String remark;

    @ExcelProperty("供应商&零件")
    private String supplierPart;

    @ExcelProperty("可疑物料数量")
    private Double defectCount;

    @ExcelProperty("供货量")
    private Double supplyQty;

    @ExcelProperty("品牌")
    private String brand;
}
