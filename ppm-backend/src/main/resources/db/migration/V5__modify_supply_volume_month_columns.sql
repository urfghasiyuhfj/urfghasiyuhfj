-- 修改供货量表：将ppm_month和month_x_no改为12个月的供货量字段
-- 1. 删除旧的ppm_month字段和相关索引
ALTER TABLE supply_volume
    DROP COLUMN IF EXISTS ppm_month,
    DROP COLUMN IF EXISTS month_x_no;

-- 删除相关索引
DROP INDEX IF EXISTS idx_supply_ppm_month ON supply_volume;
DROP INDEX IF EXISTS idx_supply_year_month ON supply_volume;
DROP INDEX IF EXISTS idx_supply_base_month ON supply_volume;

-- 2. 添加12个月的供货量字段
ALTER TABLE supply_volume
    ADD COLUMN month_1_no INT NOT NULL DEFAULT 0 COMMENT '1月供货量' AFTER fiscal_year,
    ADD COLUMN month_2_no INT NOT NULL DEFAULT 0 COMMENT '2月供货量' AFTER month_1_no,
    ADD COLUMN month_3_no INT NOT NULL DEFAULT 0 COMMENT '3月供货量' AFTER month_2_no,
    ADD COLUMN month_4_no INT NOT NULL DEFAULT 0 COMMENT '4月供货量' AFTER month_3_no,
    ADD COLUMN month_5_no INT NOT NULL DEFAULT 0 COMMENT '5月供货量' AFTER month_4_no,
    ADD COLUMN month_6_no INT NOT NULL DEFAULT 0 COMMENT '6月供货量' AFTER month_5_no,
    ADD COLUMN month_7_no INT NOT NULL DEFAULT 0 COMMENT '7月供货量' AFTER month_6_no,
    ADD COLUMN month_8_no INT NOT NULL DEFAULT 0 COMMENT '8月供货量' AFTER month_7_no,
    ADD COLUMN month_9_no INT NOT NULL DEFAULT 0 COMMENT '9月供货量' AFTER month_8_no,
    ADD COLUMN month_10_no INT NOT NULL DEFAULT 0 COMMENT '10月供货量' AFTER month_9_no,
    ADD COLUMN month_11_no INT NOT NULL DEFAULT 0 COMMENT '11月供货量' AFTER month_10_no,
    ADD COLUMN month_12_no INT NOT NULL DEFAULT 0 COMMENT '12月供货量' AFTER month_11_no;
