package com.ppm.dto.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 供应商 PPM Excel 行（供应商PPM2510 工作表）
 * 每行对应一供应商一月份，按基地拆成多行写入 supplier_ppm_summary。
 */
@Data
public class SupplierPpmExcelRow {

    @ExcelProperty("PPM月份")
    private String ppmMonth;

    @ExcelProperty("供应商编码")
    private String supplierCode;

    @ExcelProperty("供应商名称")
    private String supplierName;

    @ExcelProperty("月不合格数")
    private Double monthDefectCount;

    @ExcelProperty("月供货量")
    private Double monthSupplyQty;

    @ExcelProperty("供应商总PPM")
    private BigDecimal totalPpm;

    @ExcelProperty("河西可疑物料")
    private Double hexiDefect;

    @ExcelProperty("河西供货量")
    private Double hexiSupply;

    @ExcelProperty("河西供应商PPM")
    private BigDecimal hexiPpm;

    @ExcelProperty("宝骏可疑物料")
    private Double baojunDefect;

    @ExcelProperty("宝骏供货量")
    private Double baojunSupply;

    @ExcelProperty("宝骏供应商PPM")
    private BigDecimal baojunPpm;

    @ExcelProperty("青岛可疑物料")
    private Double qingdaoDefect;

    @ExcelProperty("青岛供货量")
    private Double qingdaoSupply;

    @ExcelProperty("青岛供应商PPM")
    private BigDecimal qingdaoPpm;

    @ExcelProperty("重庆可疑物料")
    private Double chongqingDefect;

    @ExcelProperty("重庆供货量")
    private Double chongqingSupply;

    @ExcelProperty("重庆供应商PPM")
    private BigDecimal chongqingPpm;
}
