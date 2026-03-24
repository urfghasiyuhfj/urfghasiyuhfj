package com.ppm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * PPM 自动计算结果，包含差异提醒信息。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PpmCalculateResult {

    /** 计算的月份，格式 yyyyMM */
    private String ppmMonth;
    /** 写入的记录数 */
    private int savedCount;
    /** 是否存在与原数据库的差异 */
    private boolean hasDifferences;
    /** 差异明细（当 hasDifferences 为 true 时不为空） */
    private List<PpmDiffItem> differences = new ArrayList<>();
}
