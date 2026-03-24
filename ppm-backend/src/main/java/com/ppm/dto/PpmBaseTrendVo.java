package com.ppm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 基地月度 PPM 趋势：按月份返回该基地的 defectCount、supplyQty、ppm。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PpmBaseTrendVo {

    private List<String> months;
    private List<Integer> defectCounts;
    private List<Integer> supplyQtys;
    private List<java.math.BigDecimal> ppmValues;
}
