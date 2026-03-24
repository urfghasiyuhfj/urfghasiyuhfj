# 系统中 PPM 计算过程说明

本文档说明系统中 PPM 的完整计算流程，包括**参数来源**（数据表、函数、计算方式）和**计算公式**。

---

## 一、计算入口与触发时机

| 触发方式 | 位置 | 说明 |
|---------|------|------|
| 导入供货量后自动计算 | `ImportService.triggerPpmRecalcFromSupply(List<String> ppmMonths)` | 供货量落库后，对导入涉及的每个月份调用一次计算 |
| 导入可疑物料后自动计算 | `ImportService.triggerPpmRecalcFromSuspicious(List<SuspiciousMaterialExcelRow> rows)` | 按可疑物料的 `record_date` 解析出月份集合，对每个月份调用一次计算 |
| 手动触发 | `PpmController` → `PpmCalculateService.calculate(String ppmMonth)` | 前端/接口传入 `ppm_month`（如 202507）触发当月计算 |

**核心计算函数**：`PpmCalculateService.calculate(String ppmMonth)`  
- 文件：`ppm-backend/src/main/java/com/ppm/service/PpmCalculateService.java`

---

## 二、输入参数

| 参数 | 类型 | 来源 | 说明 |
|------|------|------|------|
| **ppm_month** | String | 调用方传入 | 格式 `yyyyMM`，如 `202507` 表示 2025 年 7 月。用于确定：① 缺陷统计的日期范围；② 供货量筛选的月份；③ 结果写入的月份维度。 |

---

## 三、参与计算的数据表与读取方式

### 1. 基地主数据（有效基地范围）

| 项目 | 说明 |
|------|------|
| **数据表** | `base_info` |
| **读取方式** | `BaseInfoRepository.findAll()`（若表为空，会先执行 `ensureBaseInfoInitialized()` 插入默认：河西、宝骏、青岛、重庆） |
| **用途** | 得到有效基地集合 `baseCodes` 和 `base_code → BaseInfo` 映射 `baseByCode`；只有属于这些基地的缺陷与供货量才参与聚合，且结果中的 `base_id`、`base_name` 来自此表。 |

### 2. 不合格数（缺陷）数据

| 项目 | 说明 |
|------|------|
| **数据表** | `suspicious_material`（可疑物料表） |
| **读取方式** | `SuspiciousMaterialRepository.findAll(Specification)`，条件为：<br>• `record_date >= 当月1日` 且 `record_date <= 当月最后一日`（由 `ppm_month` 解析出 `defectStart`、`defectEnd`） |
| **用到的字段** | `plant`、`supplier_code`、`supplier_name`、`supplier_resp`、`defect_count`、`quantity` |

**不合格数的取值与过滤规则**：

- **只统计「是否供应商责任 = Y」的记录**  
  - 判断：`isSupplierResponsible(m.getSupplierResp())` → 仅当 `supplier_resp` 为 `"Y"`（忽略大小写）时计入。
- **基地**：由 `mapPlantToBase(m.getPlant())` 从 `plant` 解析（plant 字符串包含「河西/宝骏/青岛/重庆」之一则对应该基地）；不属于 `base_info` 中基地的记录不参与。
- **单条不合格数**：  
  - 若 `defect_count != null` → 取 `defect_count`；  
  - 否则取 `quantity`（默认 0）。  
  - 代码：`int defects = m.getDefectCount() != null ? m.getDefectCount() : (m.getQuantity() != null ? m.getQuantity() : 0);`

### 3. 供货量数据

| 项目 | 说明 |
|------|------|
| **数据表** | `supply_volume`（供货量表） |
| **读取方式** | `SupplyVolumeRepository.findAll(Specification)`，条件为：<br>• `fiscal_year = ppm_month 的年份`<br>• `ppm_month = 传入的 ppm_month`（如 202507） |
| **用到的字段** | `base_code`、`plant_id`、`supplier_code`、`supplier_name`、`part_name`、`month_x_no` |

**供货量的取值与过滤规则**：

- **排除零件**：`isExcludedPartName(v.getPartName())` 为 true 的行不参与——即 `part_name` 包含「螺栓」「螺母」「卡扣」之一的记录排除。
- **基地**：优先用 `supply_volume.base_code`；若为空则用 `plant_id` 查 `PLANT_ID_TO_BASE` 映射（如 8200→重庆、8000→宝骏、6430→河西、6400→青岛等），或若 `plant_id` 本身在 `baseCodes` 中则直接当基地；不属于有效基地的行不参与。
- **单条供货量**：取 `month_x_no`（为 null 时按 0）。

---

## 四、聚合维度与键

- **聚合键**：`base_code + "|" + supplier_code`（即「基地 + 供应商」唯一确定一个汇总单元）。
- **中间结构**：内存中 `Map<String, DefectSupply> agg`，键为上述聚合键，`DefectSupply` 中累计：
  - `defectCount`：该基地该供应商当月不合格数之和；
  - `supplyQty`：该基地该供应商当月供货量之和（已排除螺栓/螺母/卡扣）；
  - `supplierName`：取最后一条非空的供应商名称。

**聚合过程简述**：

1. 遍历可疑物料：按 `(基地, 供应商)` 累加不合格数。
2. 遍历供货量：按 `(基地, 供应商)` 累加供货量（`month_x_no`）。

---

## 五、计算公式

对每个「基地 + 供应商」单元（且 **供货量 > 0**）计算：

```
PPM = (不合格数 / 供货量) × 1 000 000
```

- **不合格数**：上述聚合得到的 `DefectSupply.defectCount`（来自 `suspicious_material`，仅 supplier_resp='Y'，按 record_date 在当月）。
- **供货量**：上述聚合得到的 `DefectSupply.supplyQty`（来自 `supply_volume`，按 fiscal_year + ppm_month 且排除螺栓/螺母/卡扣）。

**代码实现**（四舍五入保留 2 位小数）：

```java
BigDecimal ppm = BigDecimal.valueOf(ds.defectCount)
        .multiply(BigDecimal.valueOf(1_000_000))
        .divide(BigDecimal.valueOf(ds.supplyQty), 2, RoundingMode.HALF_UP);
```

- **供货量 = 0** 的单元不写入结果（不计算 PPM，也不插入 `supplier_ppm_summary`）。

---

## 六、结果写入

| 项目 | 说明 |
|------|------|
| **目标表** | `supplier_ppm_summary` |
| **唯一约束** | `(ppm_month, base_code, supplier_code)` |
| **写入前操作** | 先 `SupplierPpmSummaryRepository.deleteByPpmMonth(ppmMonthStored)` 删除当月全部历史汇总，再 `flush()`，再批量插入新结果，避免唯一键冲突。 |
| **每条记录字段来源** | 见下表。 |

| 字段 | 来源 |
|------|------|
| ppm_month | 入参 `ppm_month` 规范化后的 `ppmMonthStored` |
| base_id | `base_info` 中该 `base_code` 的 id，若无则为 null |
| base_code | 聚合键中的基地代码 |
| base_name | `base_info.base_name`，若无则用 `base_code` |
| supplier_code | 聚合键中的供应商代码 |
| supplier_name | 聚合过程中的 `DefectSupply.supplierName`，空则用 `supplier_code` |
| defect_count | 聚合得到的该基地该供应商不合格数 |
| supply_qty | 聚合得到的该基地该供应商供货量 |
| ppm | 按上面公式计算，保留 2 位小数 |

---

## 七、流程简图（参数与公式）

```
输入: ppm_month (yyyyMM)
         │
         ▼
┌─────────────────────────────────────────────────────────────────┐
│ 1. base_info 表 → 有效基地集合 baseCodes、baseByCode             │
│    （空则先初始化河西/宝骏/青岛/重庆）                            │
└─────────────────────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────────────┐
│ 2. suspicious_material 表                                        │
│    条件: record_date ∈ [当月1日, 当月最后一日]                    │
│    过滤: supplier_resp = 'Y'；plant 解析出的基地 ∈ baseCodes     │
│    取值: 每条 defect_count ?? quantity → 按 (基地, 供应商) 累加  │
└─────────────────────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────────────┐
│ 3. supply_volume 表                                              │
│    条件: fiscal_year = 年, ppm_month = 传入月份                  │
│    过滤: part_name 不包含 螺栓/螺母/卡扣；基地 ∈ baseCodes        │
│    取值: 每条 month_x_no → 按 (基地, 供应商) 累加                 │
└─────────────────────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────────────┐
│ 4. 按 (基地, 供应商) 汇总                                        │
│    defectCount = Σ 不合格数,  supplyQty = Σ 供货量               │
│    仅对 supplyQty > 0 的单元计算 PPM                             │
└─────────────────────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────────────┐
│ 5. 公式                                                          │
│    PPM = (defectCount / supplyQty) × 1_000_000  保留2位小数      │
└─────────────────────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────────────┐
│ 6. 先删当月 supplier_ppm_summary，再写入新结果                    │
└─────────────────────────────────────────────────────────────────┘
```

---

## 八、小结表（参数来源与公式）

| 计算项 | 数据表 | 字段/条件 | 函数/说明 |
|--------|--------|-----------|-----------|
| 有效基地 | base_info | base_code, base_name, id | BaseInfoRepository.findAll()；空则 ensureBaseInfoInitialized() |
| 不合格数 | suspicious_material | record_date, plant, supplier_code, supplier_resp, defect_count, quantity | 按 record_date 在当月；supplier_resp='Y'；plant→基地∈baseCodes；defect_count ?? quantity 按 (基地, 供应商) 累加 |
| 供货量 | supply_volume | fiscal_year, ppm_month, base_code, plant_id, part_name, month_x_no | fiscal_year=年、ppm_month=月；排除螺栓/螺母/卡扣；基地∈baseCodes；month_x_no 按 (基地, 供应商) 累加 |
| PPM 公式 | — | defectCount, supplyQty | PPM = (defectCount / supplyQty) × 1_000_000，保留 2 位小数，supplyQty>0 才写入 |
| 结果表 | supplier_ppm_summary | ppm_month, base_code, supplier_code, defect_count, supply_qty, ppm, ... | deleteByPpmMonth 后 saveAll(toSave) |

以上即系统中 PPM 的完整计算过程、参数来源与计算公式。
