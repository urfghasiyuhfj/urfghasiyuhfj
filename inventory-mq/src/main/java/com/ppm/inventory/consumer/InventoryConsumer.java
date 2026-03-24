package com.ppm.inventory.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ppm.inventory.model.SupplierInventoryMessage;
import com.ppm.inventory.service.InventoryStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 消费供应商库存消息，落库或转发到业务系统。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryConsumer {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final InventoryStore inventoryStore;

    @RabbitListener(queues = "${inventory.mq.queue:inventory.supplier.queue}")
    public void onMessage(byte[] body) {
        try {
            String json = new String(body, java.nio.charset.StandardCharsets.UTF_8);
            SupplierInventoryMessage msg = objectMapper.readValue(json, SupplierInventoryMessage.class);
            log.debug("收到库存消息: supplierCode={}, partCode={}, quantity={}",
                    msg.getSupplierCode(), msg.getPartCode(), msg.getQuantity());

            inventoryStore.upsert(msg);
        } catch (Exception e) {
            log.error("处理库存消息失败", e);
            throw new RuntimeException(e);
        }
    }
}
