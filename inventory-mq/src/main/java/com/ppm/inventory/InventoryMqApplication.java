package com.ppm.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 供应商库存数据接口 - 消息队列示例。
 * 独立于 PPM 主系统运行，端口 8081，仅依赖 RabbitMQ。
 */
@SpringBootApplication
public class InventoryMqApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventoryMqApplication.class, args);
    }
}
