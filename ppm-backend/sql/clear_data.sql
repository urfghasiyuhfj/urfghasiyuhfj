-- ================================================================
-- PPM 数据分析系统 - 清除业务数据脚本
-- 数据库: ppm_db
-- 说明: 清除所有业务数据，保留基地主数据(base_info)
-- ================================================================

USE ppm_db;

-- 禁用外键检查（如果有外键约束）
SET FOREIGN_KEY_CHECKS = 0;

-- 清除业务数据表
TRUNCATE TABLE supply_volume;
TRUNCATE TABLE suspicious_material;
TRUNCATE TABLE supplier_ppm_detail;
TRUNCATE TABLE supplier_ppm_summary;
TRUNCATE TABLE supplier_info;

-- 重新启用外键检查
SET FOREIGN_KEY_CHECKS = 1;

-- 验证清除结果
SELECT '数据清除完成！' AS message;
SELECT 
    'base_info' AS table_name, COUNT(*) AS record_count FROM base_info
UNION ALL
SELECT 
    'supplier_info', COUNT(*) FROM supplier_info
UNION ALL
SELECT 
    'supply_volume', COUNT(*) FROM supply_volume
UNION ALL
SELECT 
    'suspicious_material', COUNT(*) FROM suspicious_material
UNION ALL
SELECT 
    'supplier_ppm_detail', COUNT(*) FROM supplier_ppm_detail;
