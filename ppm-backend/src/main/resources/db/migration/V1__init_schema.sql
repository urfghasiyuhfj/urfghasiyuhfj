-- 基地主数据（河西/宝骏/青岛/重庆 等）
CREATE TABLE base_info (
    id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
    base_code   VARCHAR(32)  NOT NULL COMMENT '基地编码，如 6430/8000/8200',
    base_name   VARCHAR(64)  NOT NULL COMMENT '基地名称',
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_base_code (base_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='基地主数据';

-- 供应商主数据
CREATE TABLE supplier_info (
    id             BIGINT       AUTO_INCREMENT PRIMARY KEY,
    supplier_code  VARCHAR(32)  NOT NULL COMMENT '供应商编码',
    supplier_name  VARCHAR(128) NOT NULL COMMENT '供应商名称',
    created_at     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_supplier_code (supplier_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='供应商主数据';

-- 供货量明细（对应「基地供货量」Excel，如重庆供货量）
CREATE TABLE supply_volume (
    id               BIGINT       AUTO_INCREMENT PRIMARY KEY,
    fiscal_year      SMALLINT     NOT NULL COMMENT '年份',
    plant_id         VARCHAR(32)  NOT NULL COMMENT '基地/工厂编码',
    supplier_code    VARCHAR(32)  NOT NULL,
    supplier_name    VARCHAR(128) NULL,
    part_code        VARCHAR(64)  NOT NULL,
    part_name        VARCHAR(128) NULL,
    month_x_no       INT          NOT NULL COMMENT '供货量（可为负数）',
    supplier_part    VARCHAR(128) NULL COMMENT '供应商&零件 组合键',
    created_at       DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_supply_plant (plant_id),
    KEY idx_supply_supplier (supplier_code),
    KEY idx_supply_year_plant (fiscal_year, plant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='供货量明细';

-- 可疑物料明细（对应「可疑物料统计」Excel）
CREATE TABLE suspicious_material (
    id               BIGINT       AUTO_INCREMENT PRIMARY KEY,
    plant            VARCHAR(64)  NOT NULL COMMENT '区域工厂',
    record_date      DATE         NULL COMMENT '录入日期',
    recorder         VARCHAR(32)  NULL COMMENT '录入人员',
    part_code        VARCHAR(64)  NOT NULL,
    part_name        VARCHAR(128) NULL,
    function_module  VARCHAR(64)  NULL COMMENT '功能模块',
    supplier_code    VARCHAR(32)  NOT NULL,
    supplier_name    VARCHAR(128) NULL,
    model_machine    VARCHAR(64)  NULL COMMENT '车型|机型',
    failure_desc     VARCHAR(256) NULL COMMENT '失效描述',
    fault_type       VARCHAR(64)  NULL COMMENT '故障类别',
    quantity         INT          NOT NULL DEFAULT 0 COMMENT '数量',
    order_date       DATE         NULL COMMENT '开单日期',
    shift_section    VARCHAR(64)  NULL COMMENT '班次|工段',
    prod_area        VARCHAR(64)  NULL COMMENT '产生区域',
    supplier_resp    CHAR(1)      NULL COMMENT '是否供应商责任 Y/N',
    remark           VARCHAR(256) NULL,
    supplier_part    VARCHAR(128) NULL COMMENT '供应商&零件',
    defect_count     INT          NULL COMMENT '可疑物料数量',
    supply_qty       INT          NULL COMMENT '供货量',
    brand            VARCHAR(64)  NULL COMMENT '品牌',
    created_at       DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_susp_plant (plant),
    KEY idx_susp_supplier (supplier_code),
    KEY idx_susp_record_date (record_date),
    KEY idx_susp_order_date (order_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='可疑物料明细';

-- 各基地×供应商 PPM 汇总（计算结果写入）
CREATE TABLE supplier_ppm_summary (
    id             BIGINT        AUTO_INCREMENT PRIMARY KEY,
    ppm_month      VARCHAR(8)    NOT NULL COMMENT 'PPM月份 如 202510',
    base_id        BIGINT        NULL COMMENT 'FK base_info',
    base_code      VARCHAR(32)   NOT NULL COMMENT '基地编码',
    base_name      VARCHAR(64)   NOT NULL COMMENT '基地名称',
    supplier_code  VARCHAR(32)   NOT NULL,
    supplier_name  VARCHAR(128)  NOT NULL,
    defect_count   INT           NOT NULL COMMENT '不合格数',
    supply_qty     INT           NOT NULL COMMENT '供货量',
    ppm            DECIMAL(12,2) NOT NULL COMMENT 'PPM值',
    created_at     DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_ppm_month_base_supplier (ppm_month, base_code, supplier_code),
    KEY idx_ppm_month (ppm_month),
    KEY idx_ppm_base (base_code),
    KEY idx_ppm_supplier (supplier_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='基地×供应商 PPM 汇总';

-- 初始化基地数据（与 Excel 中河西/宝骏/青岛/重庆 对应）
INSERT INTO base_info (base_code, base_name) VALUES
('河西', '河西'),
('宝骏', '宝骏'),
('青岛', '青岛'),
('重庆', '重庆');

