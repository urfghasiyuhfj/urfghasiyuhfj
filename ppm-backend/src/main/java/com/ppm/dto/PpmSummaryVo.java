package com.ppm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * PPM 汇总列表项
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PpmSummaryVo {

    private Long id;
    private String ppmMonth;
    private String baseCode;
    private String baseName;
    private String supplierCode;
    private String supplierName;
    private Integer defectCount;
    private Integer supplyQty;
    private BigDecimal ppm;
    private LocalDateTime createdAt;
}
