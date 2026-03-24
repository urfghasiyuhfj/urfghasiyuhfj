package com.ppm.dto;

import lombok.Data;

/**
 * PPM 汇总查询参数
 */
@Data
public class PpmSummaryQueryDto {

    private String ppmMonth;
    private String supplierCode;
    private String supplierName;
    private Integer page = 1;
    private Integer size = 20;
}
