package com.ppm.controller;

import com.ppm.dto.PageResult;
import com.ppm.dto.PpmBaseTrendVo;
import com.ppm.dto.PpmCalculateResult;
import com.ppm.dto.PpmGlobalMonthlyVo;
import com.ppm.dto.PpmSummaryQueryDto;
import com.ppm.dto.PpmSummaryVo;
import com.ppm.dto.PpmSupplierMonthlyTrendVo;
import com.ppm.dto.PpmTrendVo;
import com.ppm.dto.Result;
import com.ppm.service.PpmCalculateService;
import com.ppm.service.PpmSummaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "PPM 汇总与计算")
@RestController
@RequestMapping("/ppm")
@RequiredArgsConstructor
public class PpmController {

    private final PpmSummaryService ppmSummaryService;
    private final PpmCalculateService ppmCalculateService; 

    @Operation(summary = "分页查询 PPM 汇总")
    @GetMapping("/summary")
    public Result<PageResult<PpmSummaryVo>> summary(PpmSummaryQueryDto dto) {
        if (dto.getPage() == null) dto.setPage(1);
        if (dto.getSize() == null) dto.setSize(20);
        return Result.ok(ppmSummaryService.page(dto));
    }

    @Operation(summary = "按月份查询 PPM 列表（用于统计/图表）")
    @GetMapping("/list")
    public Result<List<PpmSummaryVo>> listByMonth(@RequestParam String ppmMonth) {
        return Result.ok(ppmSummaryService.listByMonth(ppmMonth));
    }

    @Operation(summary = "PPM 趋势数据（多月份，用于折线图）")
    @GetMapping("/trend")
    public Result<PpmTrendVo> trend(@RequestParam(defaultValue = "12") Integer limitMonths) {
        return Result.ok(ppmSummaryService.getTrend(limitMonths));
    }

    @Operation(summary = "基地月度 PPM 趋势")
    @GetMapping("/trend/by-base")
    public Result<PpmBaseTrendVo> trendByBase(
            @RequestParam String baseCode,
            @RequestParam(required = false, defaultValue = "12") Integer limitMonths) {
        return Result.ok(ppmSummaryService.getTrendByBase(baseCode, limitMonths));
    }

    @Operation(summary = "全局每月总 PPM")
    @GetMapping("/global-monthly")
    public Result<PpmGlobalMonthlyVo> globalMonthly(
            @RequestParam(required = false, defaultValue = "12") Integer limitMonths) {
        return Result.ok(ppmSummaryService.getGlobalMonthly(limitMonths));
    }

    @Operation(summary = "供应商月度 PPM TOP15 趋势")
    @GetMapping("/supplier-monthly-trend")
    public Result<PpmSupplierMonthlyTrendVo> supplierMonthlyTrend(
            @RequestParam(required = false, defaultValue = "12") Integer limitMonths) {
        return Result.ok(ppmSummaryService.getSupplierMonthlyTrend(limitMonths));
    }

    @Operation(summary = "获取可用的 PPM 月份列表")
    @GetMapping("/available-months")
    public Result<List<String>> availableMonths() {
        return Result.ok(ppmSummaryService.getAvailableMonths());
    }

    @Operation(summary = "触发 PPM 计算（手动）")
    @PostMapping("/calculate")
    public Result<PpmCalculateResult> calculate(@RequestParam String ppmMonth) {
        return Result.ok(ppmCalculateService.calculate(ppmMonth));
    }
}
