package com.ppm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 供货量导入冲突信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SupplyVolumeConflictVO {

    /**
     * 冲突唯一标识（用于前端确认）
     */
    private String conflictKey;

    /**
     * 供应商代码
     */
    private String supplierCode;

    /**
     * 供应商名称
     */
    private String supplierName;

    /**
     * 零件号
     */
    private String partCode;

    /**
     * 零件名称
     */
    private String partName;

    /**
     * 基地代码
     */
    private String baseCode;

    /**
     * 工厂编码
     */
    private String plantId;

    /**
     * 财年
     */
    private Integer fiscalYear;

    /**
     * 月份（1-12）
     */
    private Integer month;

    /**
     * 原供货量
     */
    private Integer oldSupplyQty;

    /**
     * 新供货量
     */
    private Integer newSupplyQty;

    /**
     * 差异数量
     */
    private Integer diffQty;
}
