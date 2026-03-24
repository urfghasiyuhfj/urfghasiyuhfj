-- 重新设计供货量数据表 V6：完善字段以匹配 SupplyVolume 实体类
-- 删除旧表（如果存在）
DROP TABLE IF EXISTS supply_volume;

-- 创建新的供货量明细表
CREATE TABLE supply_volume (
    -- 主键
    id                  BIGINT       AUTO_INCREMENT PRIMARY KEY,

    -- 元数据字段
    create_date         DATETIME     NOT NULL COMMENT '创建日期',
    data_source         VARCHAR(64)  NOT NULL COMMENT '数据来源',
    date_ver            DATE         NOT NULL COMMENT '数据版本日期',
    etl_create_date     DATETIME     NOT NULL COMMENT 'ETL创建日期',

    -- 业务主键
    fiscal_year         SMALLINT     NOT NULL COMMENT '财年',
    plant_id            VARCHAR(32)  NOT NULL COMMENT '工厂/基地编码',
    supplier_code       VARCHAR(32)  NOT NULL COMMENT '供应商编码',
    supplier_name       VARCHAR(128) NULL COMMENT '供应商名称',
    part_code           VARCHAR(64)  NOT NULL COMMENT '零件编码',
    part_name           VARCHAR(128) NULL COMMENT '零件名称',

    -- 基地信息
    base_code           VARCHAR(32)  NULL COMMENT '基地编码，如河西/宝骏/青岛/重庆',

    -- 12个月供货量（可为负数，支持冲销/调整）
    month_1_no          INT          NOT NULL DEFAULT 0 COMMENT '1月供货量',
    month_2_no          INT          NOT NULL DEFAULT 0 COMMENT '2月供货量',
    month_3_no          INT          NOT NULL DEFAULT 0 COMMENT '3月供货量',
    month_4_no          INT          NOT NULL DEFAULT 0 COMMENT '4月供货量',
    month_5_no          INT          NOT NULL DEFAULT 0 COMMENT '5月供货量',
    month_6_no          INT          NOT NULL DEFAULT 0 COMMENT '6月供货量',
    month_7_no          INT          NOT NULL DEFAULT 0 COMMENT '7月供货量',
    month_8_no          INT          NOT NULL DEFAULT 0 COMMENT '8月供货量',
    month_9_no          INT          NOT NULL DEFAULT 0 COMMENT '9月供货量',
    month_10_no         INT          NOT NULL DEFAULT 0 COMMENT '10月供货量',
    month_11_no         INT          NOT NULL DEFAULT 0 COMMENT '11月供货量',
    month_12_no         INT          NOT NULL DEFAULT 0 COMMENT '12月供货量',

    -- 汇总字段
    total_no            INT          NULL COMMENT '年度总供货量',

    -- 对接状态
    if_docking          VARCHAR(8)   NULL COMMENT '是否对接 Y/N',

    -- 时间戳
    created_at          DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- 索引
    KEY idx_supply_plant (plant_id),
    KEY idx_supply_supplier (supplier_code),
    KEY idx_supply_year_plant (fiscal_year, plant_id),
    KEY idx_supply_base_code (base_code),
    KEY idx_supply_part_code (part_code),
    KEY idx_supply_supplier_part (supplier_code, part_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='供货量明细';
