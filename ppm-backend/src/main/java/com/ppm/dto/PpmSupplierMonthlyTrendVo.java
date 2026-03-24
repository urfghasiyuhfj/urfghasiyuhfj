package com.ppm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 供应商月度 PPM TOP15 趋势：months + 每个供应商的 ppmValues 与 months 一一对应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PpmSupplierMonthlyTrendVo {

    private List<String> months;
    private List<SupplierSeries> series;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SupplierSeries {
        private String supplierCode;
        private String supplierName;
        private List<BigDecimal> ppmValues;
    }
}
