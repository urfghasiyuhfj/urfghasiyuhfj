package com.ppm.inventory.service;

import com.ppm.inventory.model.SupplierInventoryMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 库存存储（示例：内存存储；实际可改为写 MySQL / 调主系统 API）。
 */
@Slf4j
@Service
public class InventoryStore {

    /** 示例：内存 Map，key = supplierCode|partCode|warehouseCode */
    private final Map<String, SupplierInventoryMessage> store = new ConcurrentHashMap<>();

    public void upsert(SupplierInventoryMessage msg) {
        if (msg.getSupplierCode() == null || msg.getPartCode() == null) {
            log.warn("忽略无效消息: 缺少 supplierCode 或 partCode");
            return;
        }
        String key = key(msg.getSupplierCode(), msg.getPartCode(),
                msg.getWarehouseCode() != null ? msg.getWarehouseCode() : "");
        msg.setUpdatedAt(msg.getUpdatedAt() != null ? msg.getUpdatedAt() : String.valueOf(System.currentTimeMillis()));
        store.put(key, msg);
        log.info("库存已更新: {} -> quantity={}", key, msg.getQuantity());
    }

    public List<SupplierInventoryMessage> listAll() {
        return new ArrayList<>(store.values());
    }

    public SupplierInventoryMessage get(String supplierCode, String partCode, String warehouseCode) {
        return store.get(key(supplierCode, partCode, warehouseCode != null ? warehouseCode : ""));
    }

    private static String key(String supplierCode, String partCode, String warehouseCode) {
        return supplierCode + "|" + partCode + "|" + warehouseCode;
    }
}
