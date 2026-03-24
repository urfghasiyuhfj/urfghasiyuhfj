-- 允许 month_x_no 为负数（供货量调整、冲销等场景）
ALTER TABLE supply_volume MODIFY COLUMN month_x_no INT NOT NULL COMMENT '供货量（可为负数）';
