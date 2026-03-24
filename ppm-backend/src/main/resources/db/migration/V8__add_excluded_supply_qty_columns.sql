-- 为 supplier_ppm_detail 表添加排除螺栓/螺母/卡扣后的供货量字段
-- 用于基地/公司总体PPM计算（排除特定零件）

ALTER TABLE supplier_ppm_detail
    ADD COLUMN month_supply_qty_excluded DECIMAL(18, 2) NULL COMMENT '排除螺栓/螺母/卡扣后的月度供货量（用于公司总体PPM计算）',
    ADD COLUMN hexi_supply_qty_excluded DECIMAL(18, 2) NULL COMMENT '排除螺栓/螺母/卡扣后的河西基地供货量',
    ADD COLUMN baojun_supply_qty_excluded DECIMAL(18, 2) NULL COMMENT '排除螺栓/螺母/卡扣后的宝骏基地供货量',
    ADD COLUMN qingdao_supply_qty_excluded DECIMAL(18, 2) NULL COMMENT '排除螺栓/螺母/卡扣后的青岛基地供货量',
    ADD COLUMN chongqing_supply_qty_excluded DECIMAL(18, 2) NULL COMMENT '排除螺栓/螺母/卡扣后的重庆基地供货量';
