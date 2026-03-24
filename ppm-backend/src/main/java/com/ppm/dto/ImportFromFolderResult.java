package com.ppm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.ppm.dto.ImportErrorItem;

import java.util.ArrayList;
import java.util.List;

/**
 * 从 ppm 目录批量导入 Excel 的结果统计，含 PPM 自动计算及差异信息。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImportFromFolderResult {

    private int supplierPpmRows;
    private int suspiciousMaterialRows;
    private int supplyVolumeRows;
    /** 触发 PPM 自动计算的月份结果列表（导入可疑物料/供货量时产生） */
    private List<PpmCalculateResult> ppmRecalculations = new ArrayList<>();
    /** 校验错误（批量导入时汇总） */
    private List<ImportErrorItem> validationErrors = new ArrayList<>();

    public ImportFromFolderResult(int supplierPpmRows, int suspiciousMaterialRows, int supplyVolumeRows) {
        this.supplierPpmRows = supplierPpmRows;
        this.suspiciousMaterialRows = suspiciousMaterialRows;
        this.supplyVolumeRows = supplyVolumeRows;
        this.ppmRecalculations = new ArrayList<>();
    }
}
