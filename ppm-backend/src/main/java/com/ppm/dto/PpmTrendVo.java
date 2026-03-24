package com.ppm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * PPM 趋势数据，用于折线图。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PpmTrendVo {

    /** 月份列表，如 ["202501","202502",...] */
    private List<String> months;
    /** 按基地的序列：每个基地对应一组 PPM 均值 */
    private List<BaseSeries> byBase;
    /** 全局月度平均 PPM */
    private List<BigDecimal> avgPpmByMonth;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BaseSeries {
        private String baseName;
        private List<BigDecimal> ppmValues;
    }
}
