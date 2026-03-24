package com.ppm.controller;

import com.ppm.dto.Result;
import com.ppm.entity.SupplierInfo;
import com.ppm.service.SupplierInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 供应商信息管理
 */
@Tag(name = "供应商字典")
@RestController
@RequestMapping("/supplier")
@RequiredArgsConstructor
public class SupplierInfoController {

    private final SupplierInfoService supplierInfoService;

    @Operation(summary = "供应商列表")
    @GetMapping("/list")
    public Result<List<SupplierInfo>> list() {
        return Result.ok(supplierInfoService.listAll());
    }

    @Operation(summary = "按 ID 获取供应商")
    @GetMapping("/{id}")
    public Result<SupplierInfo> getById(@PathVariable Long id) {
        return supplierInfoService.getById(id)
                .map(Result::ok)
                .orElse(Result.fail(404, "供应商不存在"));
    }

    @Operation(summary = "按编码获取供应商")
    @GetMapping("/code/{supplierCode}")
    public Result<SupplierInfo> getByCode(@PathVariable String supplierCode) {
        return supplierInfoService.getByCode(supplierCode)
                .map(Result::ok)
                .orElse(Result.fail(404, "供应商不存在"));
    }

    @Operation(summary = "新增供应商")
    @PostMapping
    public Result<SupplierInfo> create(@Valid @RequestBody SupplierInfo supplierInfo) {
        return Result.ok(supplierInfoService.create(supplierInfo));
    }

    @Operation(summary = "更新供应商")
    @PutMapping("/{id}")
    public Result<SupplierInfo> update(@PathVariable Long id, @Valid @RequestBody SupplierInfo supplierInfo) {
        return Result.ok(supplierInfoService.update(id, supplierInfo));
    }

    @Operation(summary = "删除供应商")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        supplierInfoService.delete(id);
        return Result.ok();
    }
}
