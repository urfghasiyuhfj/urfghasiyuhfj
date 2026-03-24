-- 供货量按月筛选：增加 ppm_month（yyyyMM），与 PPM 计算月份一致
ALTER TABLE supply_volume
    ADD COLUMN ppm_month VARCHAR(8) NULL COMMENT 'PPM月份 yyyyMM，用于按月筛选与计算' AFTER fiscal_year;

CREATE INDEX idx_supply_ppm_month ON supply_volume (ppm_month);
CREATE INDEX idx_supply_year_month ON supply_volume (fiscal_year, ppm_month);
