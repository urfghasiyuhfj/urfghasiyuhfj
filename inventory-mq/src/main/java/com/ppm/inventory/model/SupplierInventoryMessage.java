package com.ppm.inventory.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

/**
 * 供应商库存消息体（MQ 传递的 JSON 结构）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierInventoryMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 供应商编码 */
    private String supplierCode;
    /** 物料/零件编码 */
    private String partCode;
    /** 库存数量 */
    private Integer quantity;
    /** 仓库/库位（可选） */
    private String warehouseCode;
    /** 数据更新时间（毫秒时间戳或 ISO 字符串均可，消费端解析） */
    private String updatedAt;
    /** 业务流水号，用于幂等 */
    private String bizId;

    public long getUpdatedAtMillis() {
        if (updatedAt == null || updatedAt.isBlank()) return System.currentTimeMillis();
        try {
            return Long.parseLong(updatedAt);
        } catch (NumberFormatException e) {
            try {
                return Instant.parse(updatedAt).toEpochMilli();
            } catch (Exception ignored) {
                return System.currentTimeMillis();
            }
        }
    }
}
