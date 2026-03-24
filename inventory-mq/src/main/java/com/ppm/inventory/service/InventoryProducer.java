package com.ppm.inventory.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ppm.inventory.model.SupplierInventoryMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 向 MQ 发送供应商库存消息（供供应商系统或网关调用）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryProducer {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${inventory.mq.exchange:inventory.exchange}")
    private String exchangeName;

    @Value("${inventory.mq.routing-key:inventory.supplier}")
    private String routingKey;

    public void send(SupplierInventoryMessage message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            rabbitTemplate.convertAndSend(exchangeName, routingKey, json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            log.info("已发送库存消息: supplierCode={}, partCode={}", message.getSupplierCode(), message.getPartCode());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("序列化消息失败", e);
        }
    }
}
