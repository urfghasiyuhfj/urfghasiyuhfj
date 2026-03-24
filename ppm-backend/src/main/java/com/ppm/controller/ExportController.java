package com.ppm.controller;

import com.ppm.dto.PpmSummaryQueryDto;
import com.ppm.dto.QuerySupplyDto;
import com.ppm.dto.QuerySuspiciousDto;
import com.ppm.service.ExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;

@Tag(name = "Excel 导出")
@RestController
@RequestMapping("/export")
@RequiredArgsConstructor
public class ExportController {

    private final ExportService exportService;

    @Operation(summary = "导出 PPM 汇总")
    @GetMapping("/ppm-summary")
    public void exportPpmSummary(PpmSummaryQueryDto dto, HttpServletResponse response) throws IOException {
        exportService.exportPpmSummary(dto, response);
    }

    @Operation(summary = "导出可疑物料")
    @GetMapping("/suspicious-material")
    public void exportSuspicious(
            @RequestParam(required = false) String plant,
            @RequestParam(required = false) String supplierCode,
            @RequestParam(required = false) String partCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate recordDateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate recordDateTo,
            HttpServletResponse response) throws IOException {
        QuerySuspiciousDto dto = new QuerySuspiciousDto();
        dto.setPlant(plant);
        dto.setSupplierCode(supplierCode);
        dto.setPartCode(partCode);
        dto.setRecordDateFrom(recordDateFrom);
        dto.setRecordDateTo(recordDateTo);
        exportService.exportSuspicious(dto, response);
    }

    @Operation(summary = "导出供货量")
    @GetMapping("/supply-volume")
    public void exportSupply(
            @RequestParam(required = false) Integer fiscalYear,
            @RequestParam(required = false) String ppmMonth,
            @RequestParam(required = false) String plantId,
            @RequestParam(required = false) String supplierCode,
            HttpServletResponse response) throws IOException {
        QuerySupplyDto dto = new QuerySupplyDto();
        dto.setFiscalYear(fiscalYear);
        dto.setPpmMonth(ppmMonth);
        dto.setPlantId(plantId);
        dto.setSupplierCode(supplierCode);
        exportService.exportSupply(dto, response);
    }
}
