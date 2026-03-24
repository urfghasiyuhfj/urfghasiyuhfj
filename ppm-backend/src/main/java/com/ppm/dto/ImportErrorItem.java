package com.ppm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 导入校验错误项。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImportErrorItem {

    /** Excel 行号（从 1 开始，含表头） */
    private int row;
    /** 字段名或列名 */
    private String field;
    /** 错误描述 */
    private String message;
    /** 来源文件（批量导入时填写） */
    private String sourceFile;

    public ImportErrorItem(int row, String field, String message) {
        this(row, field, message, null);
    }
}
