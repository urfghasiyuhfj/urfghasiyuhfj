-- 供货量增加基地字段：从文件名解析（如 PPM可疑物料分析_宝骏供货量2507 中「宝骏」为基地）
ALTER TABLE supply_volume
    ADD COLUMN base_code VARCHAR(32) NULL COMMENT '基地编码/名称，如河西/宝骏/青岛/重庆，从文件名解析' AFTER ppm_month;

CREATE INDEX idx_supply_base_code ON supply_volume (base_code);
CREATE INDEX idx_supply_base_month ON supply_volume (base_code, ppm_month);
