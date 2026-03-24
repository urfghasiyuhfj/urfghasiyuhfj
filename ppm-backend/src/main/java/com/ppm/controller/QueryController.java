package com.ppm.controller;

import com.ppm.dto.PageResult;
import com.ppm.dto.QuerySupplyDto;
import com.ppm.dto.QuerySuspiciousDto;
import com.ppm.dto.Result;
import com.ppm.dto.SupplyVolumeVo;
import com.ppm.dto.SuspiciousMaterialStatsVo;
import com.ppm.entity.SuspiciousMaterial;
import com.ppm.entity.SuspiciousMaterial;
import com.ppm.service.QueryService;
import com.ppm.service.SuspiciousStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Tag(name = "综合查询")
@RestController
@RequestMapping("/query")
@RequiredArgsConstructor
public class QueryController {

    private final QueryService queryService;
    private final SuspiciousStatsService suspiciousStatsService;

    @Operation(summary = "可疑物料多条件分页查询")
    @GetMapping("/suspicious-material")
    public Result<PageResult<SuspiciousMaterial>> suspiciousMaterial(
            @RequestParam(required = false) String plant,
            @RequestParam(required = false) String supplierCode,
            @RequestParam(required = false) String supplierName,
            @RequestParam(required = false) String partCode,
            @RequestParam(required = false) String partName,
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate recordDateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate recordDateTo,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size) {
        QuerySuspiciousDto dto = new QuerySuspiciousDto();
        dto.setPlant(plant);
        dto.setSupplierCode(supplierCode);
        dto.setSupplierName(supplierName);
        dto.setPartCode(partCode);
        dto.setPartName(partName);
        dto.setRecordDateFrom(recordDateFrom);
        dto.setRecordDateTo(recordDateTo);
        dto.setPage(page);
        dto.setSize(size);
        return Result.ok(queryService.pageSuspicious(dto));
    }

    @Operation(summary = "供货量多条件分页查询")
    @GetMapping("/supply-volume")
    public Result<PageResult<SupplyVolumeVo>> supplyVolume(@ModelAttribute QuerySupplyDto dto) {
        return Result.ok(queryService.pageSupply(dto));
    }

    @Operation(summary = "可疑物料统计（按故障类别、区域工厂、供应商、月份）")
    @GetMapping("/suspicious-material/stats")
    public Result<SuspiciousMaterialStatsVo> suspiciousMaterialStats(
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate recordDateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate recordDateTo) {
        return Result.ok(suspiciousStatsService.getStats(recordDateFrom, recordDateTo));
    }
}
