package com.ppm.dto;

import lombok.Data;

@Data
public class QuerySupplyDto {
    private Integer fiscalYear;
    /** 供货量月份（yyyyMM），用于按月筛选 */
    private String ppmMonth;
    /** 基地编码/名称（如河西/宝骏/青岛/重庆），用于按基地筛选 */
    private String baseCode;
    private String plantId;
    private String supplierCode;
    /** 供应商名称，用于筛选 */
    private String supplierName;
    /** 零件号，用于筛选 */
    private String partCode;
    /** 零件名称，用于筛选 */
    private String partName;
    /** 排序字段 */
    private String sortField;
    /** 排序方向：asc/desc */
    private String sortOrder;
    private Integer page = 1;
    private Integer size = 20;
}
