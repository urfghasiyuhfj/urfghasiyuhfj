package com.ppm.controller;

import com.ppm.dto.ImportFromFolderResult;
import com.ppm.dto.ImportResult;
import com.ppm.dto.Result;
import com.ppm.dto.SupplyVolumeConflictVO;
import com.ppm.service.ImportService;
import com.ppm.service.TemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;

@Tag(name = "Excel 导入")
@RestController
@RequestMapping("/import")
@RequiredArgsConstructor
public class ImportController {

    private final ImportService importService;
    private final TemplateService templateService;

    @Value("${ppm.import-folder:D:\\codespace\\ppm\\ppm}")
    private String defaultImportFolder;

    @Operation(summary = "导入供应商 PPM Excel")
    @PostMapping("/supplier-ppm")
    public Result<ImportResult> importSupplierPpm(@RequestParam("file") MultipartFile file) {
        return Result.ok(importService.importSupplierPpm(file));
    }

    @Operation(summary = "导入可疑物料统计 Excel（导入后自动计算当月 PPM）")
    @PostMapping("/suspicious-material")
    public Result<ImportResult> importSuspiciousMaterial(@RequestParam("file") MultipartFile file) {
        return Result.ok(importService.importSuspiciousMaterial(file));
    }

    @Operation(summary = "导入供货量 Excel（导入后自动计算当月 PPM）")
    @PostMapping("/supply-volume")
    public Result<ImportResult> importSupplyVolume(@RequestParam("file") MultipartFile file) {
        return Result.ok(importService.importSupplyVolume(file));
    }

    @Operation(summary = "检测供货量导入冲突（仅检测不导入）")
    @PostMapping("/supply-volume/check-conflicts")
    public Result<List<SupplyVolumeConflictVO>> checkSupplyVolumeConflicts(@RequestParam("file") MultipartFile file) {
        List<SupplyVolumeConflictVO> conflicts = importService.checkSupplyVolumeConflicts(file);
        return Result.ok(conflicts);
    }

    @Operation(summary = "覆盖导入供货量 Excel（当存在冲突时使用）")
    @PostMapping("/supply-volume/override")
    public Result<ImportResult> importSupplyVolumeWithOverride(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "overwrite", defaultValue = "true") boolean overwrite) {
        return Result.ok(importService.importSupplyVolumeWithOverride(file, overwrite));
    }

    @Operation(summary = "下载导入模板")
    @GetMapping("/template/{type}")
    public void downloadTemplate(@PathVariable String type, jakarta.servlet.http.HttpServletResponse response)
            throws java.io.IOException {
        templateService.downloadTemplate(type, response);
    }

    @Operation(summary = "从 ppm 目录批量导入 Excel 到 MySQL")
    @PostMapping("/from-ppm-folder")
    public Result<ImportFromFolderResult> importFromPpmFolder(
            @RequestParam(value = "path", required = false) String path) {
        String folder = path != null && !path.isBlank() ? path.trim() : defaultImportFolder;
        ImportFromFolderResult result = importService.importFromPpmFolder(Path.of(folder));
        return Result.ok(result);
    }
}
