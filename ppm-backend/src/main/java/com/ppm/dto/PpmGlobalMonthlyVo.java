package com.ppm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 全局每月总 PPM：按 ppm_month 汇总后的列表。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PpmGlobalMonthlyVo {

    private List<Item> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private String ppmMonth;
        private Integer defectCount;
        private Integer supplyQty;
        private BigDecimal ppm;
    }
}
