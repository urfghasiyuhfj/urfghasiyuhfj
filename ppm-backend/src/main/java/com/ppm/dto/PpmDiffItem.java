package com.ppm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * PPM 计算结果与原数据的差异项。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PpmDiffItem {

    private String baseCode;
    private String baseName;
    private String supplierCode;
    private String supplierName;
    /** 原不合格数 */
    private Integer oldDefectCount;
    /** 新不合格数 */
    private Integer newDefectCount;
    /** 原供货量 */
    private Integer oldSupplyQty;
    /** 新供货量 */
    private Integer newSupplyQty;
    /** 原 PPM */
    private BigDecimal oldPpm;
    /** 新 PPM */
    private BigDecimal newPpm;
}
