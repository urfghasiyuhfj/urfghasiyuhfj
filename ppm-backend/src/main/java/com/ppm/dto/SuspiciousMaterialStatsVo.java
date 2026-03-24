package com.ppm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SuspiciousMaterialStatsVo {

    private List<NameCount> byPlant;
    private List<NameCount> byFaultType;
    private List<NameCount> byFailureDesc;
    private List<NameCount> bySupplier;
    private List<NameCount> byMonth;
    private long totalCount;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NameCount {
        private String name;
        private long value;
    }
}
