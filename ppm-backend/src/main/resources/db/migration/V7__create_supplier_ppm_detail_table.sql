-- 创建供应商PPM汇总表 V7：存储按基地区分的供应商PPM数据
DROP TABLE IF EXISTS supplier_ppm_detail;

CREATE TABLE supplier_ppm_detail (
    -- 主键
    id                  BIGINT       AUTO_INCREMENT PRIMARY KEY,

    -- 基础信息
    ppm_month           VARCHAR(8)   NOT NULL COMMENT 'PPM月份 yyyyMM',
    supplier_code       VARCHAR(32)  NOT NULL COMMENT '供应商编码',
    supplier_name       VARCHAR(128) NULL COMMENT '供应商名称',

    -- 供应商整体数据
    month_defect_count  DECIMAL(18,2) NULL COMMENT '月不合格数',
    month_supply_qty    DECIMAL(18,2) NULL COMMENT '月供货量',
    supplier_total_ppm  DECIMAL(18,2) NULL COMMENT '供应商总PPM',

    -- 河西基地数据
    hexi_suspicious_count DECIMAL(18,2) NULL COMMENT '河西可疑物料数',
    hexi_supply_qty       DECIMAL(18,2) NULL COMMENT '河西供货量',
    hexi_supplier_ppm     DECIMAL(18,2) NULL COMMENT '河西供应商PPM',

    -- 宝骏基地数据
    baojun_suspicious_count DECIMAL(18,2) NULL COMMENT '宝骏可疑物料数',
    baojun_supply_qty       DECIMAL(18,2) NULL COMMENT '宝骏供货量',
    baojun_supplier_ppm     DECIMAL(18,2) NULL COMMENT '宝骏供应商PPM',

    -- 青岛基地数据
    qingdao_suspicious_count DECIMAL(18,2) NULL COMMENT '青岛可疑物料数',
    qingdao_supply_qty       DECIMAL(18,2) NULL COMMENT '青岛供货量',
    qingdao_supplier_ppm     DECIMAL(18,2) NULL COMMENT '青岛供应商PPM',

    -- 重庆基地数据
    chongqing_suspicious_count DECIMAL(18,2) NULL COMMENT '重庆可疑物料数',
    chongqing_supply_qty       DECIMAL(18,2) NULL COMMENT '重庆供货量',
    chongqing_supplier_ppm     DECIMAL(18,2) NULL COMMENT '重庆供应商PPM',

    -- 时间戳
    created_at          DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- 唯一约束和索引
    UNIQUE KEY uk_ppm_supplier (ppm_month, supplier_code),
    KEY idx_ppm_month (ppm_month),
    KEY idx_supplier_code (supplier_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='供应商PPM明细汇总表';
