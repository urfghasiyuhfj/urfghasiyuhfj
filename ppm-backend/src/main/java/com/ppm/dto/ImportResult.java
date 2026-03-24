package com.ppm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 单文件导入结果，含 PPM 自动计算、差异信息及校验错误。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImportResult {

    /** 导入行数 */
    private int rowsImported;
    /** 触发 PPM 自动计算的月份结果列表（导入供货量/可疑物料时可能有） */
    private List<PpmCalculateResult> ppmRecalculations = new ArrayList<>();
    /** 校验错误（行号、字段、描述），无效行已跳过 */
    private List<ImportErrorItem> validationErrors = new ArrayList<>();
    /** 供货量导入冲突列表（检测到重复数据时） */
    private List<SupplyVolumeConflictVO> supplyVolumeConflicts = new ArrayList<>();
    /** 是否有冲突需要用户确认 */
    private boolean hasConflicts = false;

    /**
     * 3参数构造器，用于不需要冲突信息的导入场景
     */
    public ImportResult(int rowsImported, List<PpmCalculateResult> ppmRecalculations, List<ImportErrorItem> validationErrors) {
        this.rowsImported = rowsImported;
        this.ppmRecalculations = ppmRecalculations != null ? ppmRecalculations : new ArrayList<>();
        this.validationErrors = validationErrors != null ? validationErrors : new ArrayList<>();
        this.supplyVolumeConflicts = new ArrayList<>();
        this.hasConflicts = false;
    }
}
