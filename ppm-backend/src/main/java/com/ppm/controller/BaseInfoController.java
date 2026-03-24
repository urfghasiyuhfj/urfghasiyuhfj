package com.ppm.controller;

import com.ppm.dto.BaseInfoDto;
import com.ppm.dto.Result;
import com.ppm.entity.BaseInfo;
import com.ppm.service.BaseInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "基地字典")
@RestController
@RequestMapping("/base")
@RequiredArgsConstructor
public class BaseInfoController {

    private final BaseInfoService baseInfoService;

    @Operation(summary = "基地列表")
    @GetMapping("/list")
    public Result<List<BaseInfo>> list() {
        return Result.ok(baseInfoService.listAll());
    }

    @Operation(summary = "按 ID 获取基地")
    @GetMapping("/{id}")
    public Result<BaseInfo> getById(@PathVariable Long id) {
        BaseInfo entity = baseInfoService.getById(id);
        if (entity == null) {
            return Result.fail(404, "基地不存在");
        }
        return Result.ok(entity);
    }

    @Operation(summary = "按编码获取基地")
    @GetMapping("/code/{baseCode}")
    public Result<BaseInfo> getByCode(@PathVariable String baseCode) {
        BaseInfo entity = baseInfoService.getByCode(baseCode);
        if (entity == null) {
            return Result.fail(404, "基地不存在");
        }
        return Result.ok(entity);
    }

    @Operation(summary = "新增基地")
    @PostMapping
    public Result<BaseInfo> create(@Valid @RequestBody BaseInfoDto dto) {
        return Result.ok(baseInfoService.create(dto));
    }

    @Operation(summary = "更新基地")
    @PutMapping("/{id}")
    public Result<BaseInfo> update(@PathVariable Long id, @Valid @RequestBody BaseInfoDto dto) {
        return Result.ok(baseInfoService.update(id, dto));
    }

    @Operation(summary = "删除基地")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        baseInfoService.delete(id);
        return Result.ok();
    }
}
