# PPM 计算逻辑及涉及模块完整性检查报告

**检查日期**：2025-01-29  

---

## 一、PPM 计算逻辑概览

### 1.1 核心公式

```
PPM = (不合格数 / 供货量) × 1,000,000
```

计算维度：**基地 × 供应商**

### 1.2 数据来源与时间范围

| 数据源 | 表 | 时间维度 | 字段 |
|--------|------|----------|------|
| 不合格数 | `suspicious_material` | 按月 | `record_date` 在当月内 |
| 供货量 | `supply_volume` | 按年 | `fiscal_year` 等于指定年 |

---

## 二、涉及模块清单

### 2.1 后端模块

| 模块 | 路径 | 职责 |
|------|------|------|
| **PpmCalculateService** | `service/PpmCalculateService.java` | 核心计算逻辑：聚合缺陷与供货量，计算 PPM，写入结果 |
| **ImportService** | `service/ImportService.java` | 导入可疑物料/供货量后自动触发 PPM 计算 |
| **PpmController** | `controller/PpmController.java` | 暴露 `POST /ppm/calculate` 手动触发计算 |
| **PpmSummaryService** | `service/PpmSummaryService.java` | 查询、分页、趋势统计 |
| **BaseInfoRepository** | `repository/BaseInfoRepository.java` | 提供有效基地列表 |
| **SuspiciousMaterialRepository** | `repository/SuspiciousMaterialRepository.java` | 按 record_date 查询可疑物料 |
| **SupplyVolumeRepository** | `repository/SupplyVolumeRepository.java` | 按 fiscal_year 查询供货量 |
| **SupplierPpmSummaryRepository** | `repository/SupplierPpmSummaryRepository.java` | 存储结果，支持 deleteByPpmMonth、findByPpmMonthOrderByPpmDesc |

### 2.2 实体与 DTO

| 类型 | 类 | 说明 |
|------|------|------|
| 实体 | `SuspiciousMaterial` | 可疑物料，含 plant、recordDate、supplierCode、defectCount、quantity |
| 实体 | `SupplyVolume` | 供货量，含 plantId、fiscalYear、supplierCode、monthXNo |
| 实体 | `BaseInfo` | 基地主数据，base_code 与 base_name |
| 实体 | `SupplierPpmSummary` | PPM 汇总结果 |
| DTO | `PpmCalculateResult` | 计算结果：ppmMonth、savedCount、hasDifferences、differences |
| DTO | `PpmDiffItem` | 差异明细 |

### 2.3 前端

| 模块 | 路径 | 说明 |
|------|------|------|
| Ppm.vue | `views/Ppm.vue` | 选择月份、触发计算、展示结果与差异提醒 |
| ppm Api | `api/ppm.js` | `calculate(ppmMonth)` 调用 `POST /api/ppm/calculate` |

---

## 三、数据流与触发链路

### 3.1 触发方式

1. **手动触发**：PPM 管理页选择月份 → 点击「计算」→ `POST /ppm/calculate?ppmMonth=yyyyMM`
2. **自动触发**：导入可疑物料或供货量 Excel → `ImportService` 内部调用 `ppmCalculateService.calculate(ppmMonth)`

### 3.2 计算流程

```
1. 校验 ppmMonth（yyyyMM）
2. 加载 base_info，构建 baseCodes / baseByCode
3. 查询 suspicious_material（record_date 在当月）
4. 查询 supply_volume（fiscal_year 等于年）
5. 按 base|supplier 聚合 defectCount 和 supplyQty
6. 对 supplyQty > 0 的维度计算 PPM
7. 与已有结果对比，生成差异
8. 删除该月旧数据，批量插入新数据
9. 返回 PpmCalculateResult
```

---

## 四、已修复问题

### 4.1 plant_id 映射不一致（已修复）

**问题**：`ImportService.inferPlantIdFromFilename` 从文件名推断的 plant_id 与 `PpmCalculateService.PLANT_ID_TO_BASE` 不一致。

| 基地 | ImportService 推断 | PpmCalculateService 原映射 | 结果 |
|------|-------------------|---------------------------|------|
| 河西 | 6430 | 仅 1000 | 6430 无法映射 |
| 青岛 | 6400 | 仅 3000 | 6400 无法映射 |

**修复**：在 `PLANT_ID_TO_BASE` 中补充 `6430 -> 河西`、`6400 -> 青岛`。

### 4.2 ppmMonth 参数校验（已增强）

**问题**：非法格式（如 `2025xx`、`202510.0`）可能引发 `NumberFormatException`。

**修复**：
- 支持去除 `.0` 后缀
- 正则校验 `\d{4}(\d{2})?`
- 统一存储为 `yyyyMM`（6 位）

---

## 五、潜在设计说明

### 5.1 时间维度差异

- 缺陷：**按月**（record_date 在当月）
- 供货量：**按年**（fiscal_year）

即用「当月缺陷数 ÷ 当年供货量」计算 PPM。若业务要求「当月缺陷 ÷ 当月供货」，需在 `supply_volume` 或类似表中增加月份维度并调整聚合逻辑。

### 5.2 record_date 为 null

`record_date` 为空的可疑物料不参与当月计算，可能被忽略。若需纳入，应定义明确规则（如归入某默认月份）。

### 5.3 供货量为 0 或负数

`supplyQty <= 0` 的组合会被跳过，不写入 `supplier_ppm_summary`。

### 5.4 base_info 为空

`base_info` 为空时直接返回空结果，不执行计算。

---

## 六、模块完整性结论

| 检查项 | 状态 |
|--------|------|
| 计算服务逻辑 | ✅ 完整 |
| 缺陷/供货量聚合 | ✅ 正确 |
| 基地映射 | ✅ 已与导入逻辑对齐 |
| 参数校验 | ✅ 已增强 |
| 手动计算接口 | ✅ 正常 |
| 导入后自动计算 | ✅ 正常 |
| 差异检测与返回 | ✅ 正常 |
| 前端调用与展示 | ✅ 正常 |

**结论**：PPM 计算逻辑及关联模块已完整，plant_id 映射与参数校验问题已修复。
