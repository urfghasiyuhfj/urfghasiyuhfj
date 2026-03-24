package com.ppm.inventory.controller;

import com.ppm.inventory.model.SupplierInventoryMessage;
import com.ppm.inventory.service.InventoryProducer;
import com.ppm.inventory.service.InventoryStore;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 示例接口：
 * - POST 模拟供应商推送库存（发到 MQ，由消费者落库）
 * - GET 查询当前已落库的库存列表（仅示例，内存存储）
 */
@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryProducer producer;
    private final InventoryStore store;

    /** 模拟供应商推送一条/批量库存数据（写入 MQ） */
    @PostMapping("/push")
    public String push(@RequestBody SupplierInventoryMessage message) {
        if (message.getUpdatedAt() == null || message.getUpdatedAt().isBlank()) {
            message.setUpdatedAt(String.valueOf(System.currentTimeMillis()));
        }
        if (message.getBizId() == null || message.getBizId().isBlank()) {
            message.setBizId(message.getSupplierCode() + "_" + message.getPartCode() + "_" + System.currentTimeMillis());
        }
        producer.send(message);
        return "ok";
    }

    /** 批量推送（逐条发到 MQ） */
    @PostMapping("/push-batch")
    public String pushBatch(@RequestBody List<SupplierInventoryMessage> messages) {
        for (SupplierInventoryMessage msg : messages) {
            if (msg.getUpdatedAt() == null || msg.getUpdatedAt().isBlank()) {
                msg.setUpdatedAt(String.valueOf(System.currentTimeMillis()));
            }
            if (msg.getBizId() == null || msg.getBizId().isBlank()) {
                msg.setBizId(msg.getSupplierCode() + "_" + msg.getPartCode() + "_" + System.currentTimeMillis());
            }
            producer.send(msg);
        }
        return "ok, count=" + messages.size();
    }

    /** 查询当前已消费的库存列表（示例：内存） */
    @GetMapping("/list")
    public List<SupplierInventoryMessage> list() {
        return store.listAll();
    }
}
