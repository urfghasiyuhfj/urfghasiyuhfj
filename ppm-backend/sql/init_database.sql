-- ================================================================
-- PPM 数据分析系统 - 建库建表脚本
-- 数据库: ppm_db
-- 字符集: utf8mb4
-- 执行方式: mysql -u root -p < init_database.sql
-- ================================================================

-- 1. 创建数据库
CREATE DATABASE IF NOT EXISTS ppm_db
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE ppm_db;

-- ================================================================
-- 2. 创建数据表
-- ================================================================

-- 2.1 基地主数据（河西/宝骏/青岛/重庆 等）
DROP TABLE IF EXISTS base_info;
CREATE TABLE base_info (
    id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
    base_code   VARCHAR(32)  NOT NULL COMMENT '基地编码',
    base_name   VARCHAR(64)  NOT NULL COMMENT '基地名称',
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_base_code (base_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='基地主数据';

-- 2.2 供应商主数据
DROP TABLE IF EXISTS supplier_info;
CREATE TABLE supplier_info (
    id             BIGINT       AUTO_INCREMENT PRIMARY KEY,
    supplier_code  VARCHAR(32)  NOT NULL COMMENT '供应商编码',
    supplier_name  VARCHAR(128) NOT NULL COMMENT '供应商名称',
    created_at     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_supplier_code (supplier_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='供应商主数据';

-- 2.3 供货量明细（对应「基地供货量」Excel）
DROP TABLE IF EXISTS supply_volume;
CREATE TABLE supply_volume (
    id               BIGINT       AUTO_INCREMENT PRIMARY KEY,
    create_date      DATETIME     NOT NULL COMMENT '创建日期',
    data_source      VARCHAR(64)  NOT NULL COMMENT '数据来源',
    date_ver         DATE         NOT NULL COMMENT '数据版本日期',
    etl_create_date  DATETIME     NOT NULL COMMENT 'ETL创建日期',
    fiscal_year      SMALLINT     NOT NULL COMMENT '财年',
    month_1_no       INT          NOT NULL DEFAULT 0 COMMENT '1月供货量',
    month_2_no       INT          NOT NULL DEFAULT 0 COMMENT '2月供货量',
    month_3_no       INT          NOT NULL DEFAULT 0 COMMENT '3月供货量',
    month_4_no       INT          NOT NULL DEFAULT 0 COMMENT '4月供货量',
    month_5_no       INT          NOT NULL DEFAULT 0 COMMENT '5月供货量',
    month_6_no       INT          NOT NULL DEFAULT 0 COMMENT '6月供货量',
    month_7_no       INT          NOT NULL DEFAULT 0 COMMENT '7月供货量',
    month_8_no       INT          NOT NULL DEFAULT 0 COMMENT '8月供货量',
    month_9_no       INT          NOT NULL DEFAULT 0 COMMENT '9月供货量',
    month_10_no      INT          NOT NULL DEFAULT 0 COMMENT '10月供货量',
    month_11_no      INT          NOT NULL DEFAULT 0 COMMENT '11月供货量',
    month_12_no      INT          NOT NULL DEFAULT 0 COMMENT '12月供货量',
    part_name        VARCHAR(128) NULL     COMMENT '零件名称',
    part_code        VARCHAR(64)  NOT NULL COMMENT '零件号',
    plant_id         VARCHAR(32)  NOT NULL COMMENT '基地/工厂编码',
    supplier_code    VARCHAR(32)  NOT NULL COMMENT '供应商编码',
    supplier_name    VARCHAR(128) NULL     COMMENT '供应商名称',
    base_code        VARCHAR(32)  NULL     COMMENT '基地编码',
    total_no         INT          NULL     COMMENT '总供货量',
    if_docking       VARCHAR(32)  NULL     COMMENT '是否对接',
    created_at       DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_supply_plant (plant_id),
    KEY idx_supply_supplier (supplier_code),
    KEY idx_supply_year_plant (fiscal_year, plant_id),
    KEY idx_supply_base_code (base_code),
    KEY idx_supply_part_code (part_code),
    KEY idx_supply_supplier_part (supplier_code, part_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='供货量明细';

-- 2.4 可疑物料明细（对应「可疑物料统计」Excel）
DROP TABLE IF EXISTS suspicious_material;
CREATE TABLE suspicious_material (
    id               BIGINT       AUTO_INCREMENT PRIMARY KEY,
    plant            VARCHAR(64)  NOT NULL COMMENT '区域工厂',
    record_date      DATE         NULL     COMMENT '录入日期',
    recorder         VARCHAR(32)  NULL     COMMENT '录入人员',
    part_code        VARCHAR(64)  NOT NULL COMMENT '零件号',
    part_name        VARCHAR(128) NULL     COMMENT '零件名称',
    function_module  VARCHAR(64)  NULL     COMMENT '功能模块',
    supplier_code    VARCHAR(32)  NOT NULL COMMENT '供应商编码',
    supplier_name    VARCHAR(128) NULL     COMMENT '供应商名称',
    model_machine    VARCHAR(64)  NULL     COMMENT '车型|机型',
    failure_desc     VARCHAR(256) NULL     COMMENT '失效描述',
    fault_type       VARCHAR(64)  NULL     COMMENT '故障类别',
    quantity         INT          NOT NULL DEFAULT 0 COMMENT '数量',
    order_date       DATE         NULL     COMMENT '开单日期',
    shift_section    VARCHAR(64)  NULL     COMMENT '班次|工段',
    prod_area        VARCHAR(64)  NULL     COMMENT '产生区域',
    supplier_resp    CHAR(1)      NULL     COMMENT '是否供应商责任 Y/N',
    remark           VARCHAR(256) NULL     COMMENT '备注',
    supplier_part    VARCHAR(128) NULL     COMMENT '供应商&零件',
    defect_count     INT          NULL     COMMENT '可疑物料数量',
    supply_qty       INT          NULL     COMMENT '供货量',
    brand            VARCHAR(64)  NULL     COMMENT '品牌',
    created_at       DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_susp_plant (plant),
    KEY idx_susp_supplier (supplier_code),
    KEY idx_susp_record_date (record_date),
    KEY idx_susp_order_date (order_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='可疑物料明细';

-- 2.5 各基地×供应商 PPM 汇总（计算结果写入）
DROP TABLE IF EXISTS supplier_ppm_summary;
CREATE TABLE supplier_ppm_summary (
    id             BIGINT        AUTO_INCREMENT PRIMARY KEY,
    ppm_month      VARCHAR(8)    NOT NULL COMMENT 'PPM月份，如 202510',
    base_id        BIGINT        NULL     COMMENT 'FK base_info',
    base_code      VARCHAR(32)   NOT NULL COMMENT '基地编码',
    base_name      VARCHAR(64)   NOT NULL COMMENT '基地名称',
    supplier_code  VARCHAR(32)   NOT NULL COMMENT '供应商编码',
    supplier_name  VARCHAR(128)  NOT NULL COMMENT '供应商名称',
    defect_count   INT           NOT NULL COMMENT '不合格数',
    supply_qty     INT           NOT NULL COMMENT '供货量',
    ppm            DECIMAL(12,2) NOT NULL COMMENT 'PPM值 = (不合格数/供货量)*1000000',
    created_at     DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_ppm_month_base_supplier (ppm_month, base_code, supplier_code),
    KEY idx_ppm_month (ppm_month),
    KEY idx_ppm_base (base_code),
    KEY idx_ppm_supplier (supplier_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='基地×供应商 PPM 汇总';

-- 2.6 供应商 PPM 明细（多维度PPM数据）
DROP TABLE IF EXISTS supplier_ppm_detail;
CREATE TABLE supplier_ppm_detail (
    id                            BIGINT        AUTO_INCREMENT PRIMARY KEY,
    ppm_month                     VARCHAR(8)    NOT NULL COMMENT 'PPM月份，如 202510',
    supplier_code                 VARCHAR(32)   NOT NULL COMMENT '供应商编码',
    supplier_name                 VARCHAR(128)  NULL     COMMENT '供应商名称',
    -- 供应商整体数据
    month_defect_count            DECIMAL(18,2) NULL     COMMENT '月度不合格数',
    month_supply_qty              DECIMAL(18,2) NULL     COMMENT '月度供货量',
    supplier_total_ppm            DECIMAL(18,2) NULL     COMMENT '供应商总体PPM',
    -- 河西基地数据
    hexi_suspicious_count         DECIMAL(18,2) NULL     COMMENT '河西可疑物料数',
    hexi_supply_qty               DECIMAL(18,2) NULL     COMMENT '河西供货量',
    hexi_supplier_ppm             DECIMAL(18,2) NULL     COMMENT '河西PPM',
    -- 宝骏基地数据
    baojun_suspicious_count       DECIMAL(18,2) NULL     COMMENT '宝骏可疑物料数',
    baojun_supply_qty             DECIMAL(18,2) NULL     COMMENT '宝骏供货量',
    baojun_supplier_ppm           DECIMAL(18,2) NULL     COMMENT '宝骏PPM',
    -- 青岛基地数据
    qingdao_suspicious_count      DECIMAL(18,2) NULL     COMMENT '青岛可疑物料数',
    qingdao_supply_qty            DECIMAL(18,2) NULL     COMMENT '青岛供货量',
    qingdao_supplier_ppm          DECIMAL(18,2) NULL     COMMENT '青岛PPM',
    -- 重庆基地数据
    chongqing_suspicious_count    DECIMAL(18,2) NULL     COMMENT '重庆可疑物料数',
    chongqing_supply_qty          DECIMAL(18,2) NULL     COMMENT '重庆供货量',
    chongqing_supplier_ppm        DECIMAL(18,2) NULL     COMMENT '重庆PPM',
    -- 排除螺栓/螺母/卡扣后的供货量
    month_supply_qty_excluded     DECIMAL(18,2) NULL     COMMENT '排除后月度供货量',
    hexi_supply_qty_excluded      DECIMAL(18,2) NULL     COMMENT '排除后河西供货量',
    baojun_supply_qty_excluded    DECIMAL(18,2) NULL     COMMENT '排除后宝骏供货量',
    qingdao_supply_qty_excluded   DECIMAL(18,2) NULL     COMMENT '排除后青岛供货量',
    chongqing_supply_qty_excluded DECIMAL(18,2) NULL     COMMENT '排除后重庆供货量',
    created_at                    DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at                    DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_ppm_month_supplier (ppm_month, supplier_code),
    KEY idx_ppm_month (ppm_month),
    KEY idx_supplier_code (supplier_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='供应商PPM明细（多维度）';

-- ================================================================
-- 3. 初始化基础数据
-- ================================================================

-- 3.1 初始化基地数据
INSERT INTO base_info (base_code, base_name) VALUES
('河西', '河西'),
('宝骏', '宝骏'),
('青岛', '青岛'),
('重庆', '重庆');

-- ================================================================
-- 4. 验证表结构
-- ================================================================
SELECT '数据库与表创建完成！' AS message;
SHOW TABLES;
