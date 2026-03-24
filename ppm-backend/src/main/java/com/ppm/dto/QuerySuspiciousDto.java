package com.ppm.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class QuerySuspiciousDto {
    private String plant;
    private String supplierCode;
    private String supplierName;
    private String partCode;
    private String partName;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate recordDateFrom;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate recordDateTo;
    private Integer page = 1;
    private Integer size = 20;
}
