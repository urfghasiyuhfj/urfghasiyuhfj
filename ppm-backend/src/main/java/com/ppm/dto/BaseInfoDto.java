package com.ppm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 基地创建/更新 DTO。
 */
@Data
@NoArgsConstructor
public class BaseInfoDto {

    @NotBlank(message = "基地编码不能为空")
    @Size(max = 32)
    private String baseCode;

    @NotBlank(message = "基地名称不能为空")
    @Size(max = 64)
    private String baseName;

    public BaseInfoDto(String baseCode, String baseName) {
        this.baseCode = baseCode != null ? baseCode.trim() : "";
        this.baseName = baseName != null ? baseName.trim() : "";
    }
}
