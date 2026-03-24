package com.ppm.service;

import com.ppm.dto.PpmCalculateResult;
import com.ppm.entity.*;
import com.ppm.repository.BaseInfoRepository;
import com.ppm.repository.SupplierPpmDetailRepository;
import com.ppm.repository.SupplyVolumeRepository;
import com.ppm.repository.SuspiciousMaterialRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

/**
 * PPM 计算服务：从可疑物料、供货量聚合计算并写入 supplier_ppm_detail。
 * 公式：PPM = (不合格数 / 供货量) × 1_000_000
 * 规则（与飞书河西基地口径一致）：
 * - 不合格数：仅统计「是否供应商责任=Y」的可疑物料数量；
 * 
 * supplier_ppm_detail 表按供应商×月份存储，包含四大基地的详细数据。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PpmCalculateService {

    /**
     * plant_id（供货量表）-> base_code 映射，与 base_info 对应。
     * 需与 ImportService.inferPlantIdFromFilename 保持一致，覆盖 Excel 导入与直接录入的 plant_id。
     * 同时支持直接使用基地名称作为 plant_id（河西/宝骏/青岛/重庆）。
     */
    private static final Map<String, String> PLANT_ID_TO_BASE = Map.ofEntries(
            Map.entry("1000", "河西"),
            //Map.entry("6430", "河西"),  // ImportService 从「河西」文件名推断
            Map.entry("河西", "河西"),
            Map.entry("4000", "河西"),
            Map.entry("8000", "宝骏"),
            Map.entry("8100", "宝骏"),
            Map.entry("宝骏", "宝骏"),
            Map.entry("6000", "宝骏"),
            Map.entry("3000", "青岛"),
            Map.entry("5000", "青岛"),
            //Map.entry("6400", "青岛"),  // ImportService 从「青岛」文件名推断
            Map.entry("青岛", "青岛"),
            Map.entry("8200", "重庆"),
            Map.entry("8300", "重庆"),
            Map.entry("重庆", "重庆")
    );

    private final BaseInfoRepository baseInfoRepository;
    private final SuspiciousMaterialRepository suspiciousMaterialRepository;
    private final SupplyVolumeRepository supplyVolumeRepository;
    private final SupplierPpmDetailRepository detailRepository;

    /**
     * 按月份、供应商维度计算 PPM 并写入 supplier_ppm_detail。
     * ppm_month 格式：yyyyMM（如 202510）。缺陷按当月聚合，供货量按年聚合。
     * supplier_ppm_detail 表按供应商×月份存储，包含四大基地的详细数据。
     *
     * @return 计算结果
     */
    // 重要：使用新事务执行自动计算，避免计算异常把"导入事务"标记为 rollback-only
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public PpmCalculateResult calculate(String ppmMonth) {
        if (ppmMonth == null || ppmMonth.length() < 4) {
            throw new IllegalArgumentException("ppm_month 格式应为 yyyyMM，如 202510");
        }
        String norm = ppmMonth != null ? ppmMonth.trim().replace(".0", "") : "";
        if (!norm.matches("\\d{4}(\\d{2})?")) {
            throw new IllegalArgumentException("ppm_month 格式应为 yyyyMM，如 202510，当前: " + ppmMonth);
        }
        int year = Integer.parseInt(norm.substring(0, 4));
        int month = norm.length() >= 6 ? Integer.parseInt(norm.substring(4, 6)) : 12;
        if (month < 1 || month > 12) month = 12;
        String ppmMonthStored = String.format("%04d%02d", year, month);

        // 确保 base_info 至少包含系统内默认的几个基地，避免因表为空导致无法计算 PPM
        ensureBaseInfoInitialized();

        // 计算并保存供应商PPM明细到 supplier_ppm_detail 表
        int savedCount = calculateSupplierPpmDetail(ppmMonthStored, year, month);

        log.info("PPM 计算完成: ppmMonth={}, 写入 supplier_ppm_detail {} 条", ppmMonthStored, savedCount);
        return new PpmCalculateResult(ppmMonthStored, savedCount, false, List.of());
    }

    /**
     * 计算供应商PPM明细汇总（包含各基地的详细数据）
     * 根据fiscal_year和month_x_no字段来确定供货量所在的月份
     * 
     * @return 保存的记录数
     */
    private int calculateSupplierPpmDetail(String ppmMonthStored, int year, int month) {
        // 查询可疑物料数据 - 使用当月
        LocalDate defectStart = LocalDate.of(year, month, 1);
        LocalDate defectEnd = LocalDate.of(year, month, 1).plusMonths(1).minusDays(1);

        List<SuspiciousMaterial> suspList = suspiciousMaterialRepository.findAll(
                (Specification<SuspiciousMaterial>) (root, q, cb) -> cb.and(
                        cb.greaterThanOrEqualTo(root.get("orderDate"), defectStart),
                        cb.lessThanOrEqualTo(root.get("orderDate"), defectEnd)
                ));

        // 查询供货量数据 - 使用当年所有月份的数据
        List<SupplyVolume> supplyList = supplyVolumeRepository.findAll(
                (Specification<SupplyVolume>) (root, q, cb) ->
                        cb.equal(root.get("fiscalYear"), Short.valueOf((short) year))
        );

        if (supplyList.isEmpty()) {
            long totalSupply = supplyVolumeRepository.count();
            log.warn("PPM 计算: ppmMonth={} 未查询到当年供货量数据（过滤条件 fiscal_year={}）。若库中确有数据请检查导入数据（当前共 {} 条供货量记录）",
                    ppmMonthStored, year, totalSupply);
        }

        // 按供应商聚合数据
        Map<String, SupplierPpmData> supplierDataMap = new HashMap<>();

        // 处理可疑物料数据 - 按供应商聚合
        for (SuspiciousMaterial m : suspList) {
            if (!isSupplierResponsible(m.getSupplierResp())) continue;
            String sc = m.getSupplierCode();
            if (sc == null) continue;

            String base = mapPlantToBase(m.getPlant());
            int defects = m.getDefectCount() != null ? m.getDefectCount()
                    : (m.getQuantity() != null ? m.getQuantity() : 0);

            SupplierPpmData data = supplierDataMap.computeIfAbsent(sc, k -> new SupplierPpmData());
            data.supplierCode = sc;
            data.supplierName = m.getSupplierName();
            data.monthDefectCount += defects;

            // 按基地统计可疑物料
            if ("河西".equals(base)) {
                data.hexiSuspiciousCount += defects;
            } else if ("宝骏".equals(base)) {
                data.baojunSuspiciousCount += defects;
            } else if ("青岛".equals(base)) {
                data.qingdaoSuspiciousCount += defects;
            } else if ("重庆".equals(base)) {
                data.chongqingSuspiciousCount += defects;
            }
        }

        // 处理供货量数据 - 根据fiscal_year和month_x_no确定月份
        for (SupplyVolume v : supplyList) {
            String sc = v.getSupplierCode();
            if (sc == null) continue;

            String base = v.getBaseCode() != null && !v.getBaseCode().isBlank()
                    ? v.getBaseCode().trim() : null;
            if (base == null) {
                String pid = v.getPlantId() != null ? v.getPlantId().trim() : "";
                base = PLANT_ID_TO_BASE.get(pid);
                if (base == null && (pid.equals("河西") || pid.equals("宝骏") || pid.equals("青岛") || pid.equals("重庆"))) {
                    base = pid;
                }
            }
            if (base == null) continue;

            // 获取财年
            Short fiscalYear = v.getFiscalYear();
            if (fiscalYear == null) continue;

            // 遍历12个月份，根据fiscal_year和month_x_no判断是否为计算的月份
            // 如果fiscal_year等于要计算的年份，并且该月有供货量，则统计
            if (!fiscalYear.equals(Short.valueOf((short) year))) {
                continue;
            }

            SupplierPpmData data = supplierDataMap.computeIfAbsent(sc, k -> new SupplierPpmData());
            data.supplierCode = sc;
            if (data.supplierName == null && v.getSupplierName() != null) {
                data.supplierName = v.getSupplierName();
            }

            // 遍历1-12月的供货量
            int[] monthQty = {
                v.getMonth1No() != null ? v.getMonth1No() : 0,
                v.getMonth2No() != null ? v.getMonth2No() : 0,
                v.getMonth3No() != null ? v.getMonth3No() : 0,
                v.getMonth4No() != null ? v.getMonth4No() : 0,
                v.getMonth5No() != null ? v.getMonth5No() : 0,
                v.getMonth6No() != null ? v.getMonth6No() : 0,
                v.getMonth7No() != null ? v.getMonth7No() : 0,
                v.getMonth8No() != null ? v.getMonth8No() : 0,
                v.getMonth9No() != null ? v.getMonth9No() : 0,
                v.getMonth10No() != null ? v.getMonth10No() : 0,
                v.getMonth11No() != null ? v.getMonth11No() : 0,
                v.getMonth12No() != null ? v.getMonth12No() : 0
            };

            // 如果当前遍历到的月份等于要计算的月份，则统计该供货量
            for (int m = 1; m <= 12; m++) {
                if (m != month) continue; // 只统计要计算的月份
                int qty = monthQty[m - 1];
                if (qty == 0) continue;

                // 全量供货量（用于单个供应商PPM计算）
                data.monthSupplyQty += qty;

                // 排除螺栓/螺母/卡扣后的供货量（用于基地/公司总体PPM计算）
                boolean isExcluded = isExcludedPartName(v.getPartName());
                if (!isExcluded) {
                    data.monthSupplyQtyExcluded += qty;
                }

                // 按基地统计供货量（双轨计算）
                if ("河西".equals(base)) {
                    data.hexiSupplyQty += qty;
                    if (!isExcluded) {
                        data.hexiSupplyQtyExcluded += qty;
                    }
                } else if ("宝骏".equals(base)) {
                    data.baojunSupplyQty += qty;
                    if (!isExcluded) {
                        data.baojunSupplyQtyExcluded += qty;
                    }
                } else if ("青岛".equals(base)) {
                    data.qingdaoSupplyQty += qty;
                    if (!isExcluded) {
                        data.qingdaoSupplyQtyExcluded += qty;
                    }
                } else if ("重庆".equals(base)) {
                    data.chongqingSupplyQty += qty;
                    if (!isExcluded) {
                        data.chongqingSupplyQtyExcluded += qty;
                    }
                }
            }
        }

        // 删除旧数据
        detailRepository.deleteByPpmMonth(ppmMonthStored);
        detailRepository.flush();

        // 计算并保存
        List<SupplierPpmDetail> toSave = new ArrayList<>();
        for (SupplierPpmData data : supplierDataMap.values()) {
            SupplierPpmDetail detail = new SupplierPpmDetail();
            detail.setPpmMonth(ppmMonthStored);
            detail.setSupplierCode(data.supplierCode);
            detail.setSupplierName(data.supplierName);

            // 供应商整体PPM
            detail.setMonthDefectCount(BigDecimal.valueOf(data.monthDefectCount));
            detail.setMonthSupplyQty(BigDecimal.valueOf(data.monthSupplyQty));
            if (data.monthSupplyQty > 0) {
                detail.setSupplierTotalPpm(BigDecimal.valueOf(data.monthDefectCount)
                        .multiply(BigDecimal.valueOf(1_000_000))
                        .divide(BigDecimal.valueOf(data.monthSupplyQty), 2, RoundingMode.HALF_UP));
            } else {
                detail.setSupplierTotalPpm(BigDecimal.ZERO);
            }

            // 河西基地PPM
            detail.setHexiSuspiciousCount(BigDecimal.valueOf(data.hexiSuspiciousCount));
            detail.setHexiSupplyQty(BigDecimal.valueOf(data.hexiSupplyQty));
            if (data.hexiSupplyQty > 0) {
                detail.setHexiSupplierPpm(BigDecimal.valueOf(data.hexiSuspiciousCount)
                        .multiply(BigDecimal.valueOf(1_000_000))
                        .divide(BigDecimal.valueOf(data.hexiSupplyQty), 2, RoundingMode.HALF_UP));
            } else {
                detail.setHexiSupplierPpm(BigDecimal.ZERO);
            }

            // 宝骏基地PPM
            detail.setBaojunSuspiciousCount(BigDecimal.valueOf(data.baojunSuspiciousCount));
            detail.setBaojunSupplyQty(BigDecimal.valueOf(data.baojunSupplyQty));
            if (data.baojunSupplyQty > 0) {
                detail.setBaojunSupplierPpm(BigDecimal.valueOf(data.baojunSuspiciousCount)
                        .multiply(BigDecimal.valueOf(1_000_000))
                        .divide(BigDecimal.valueOf(data.baojunSupplyQty), 2, RoundingMode.HALF_UP));
            } else {
                detail.setBaojunSupplierPpm(BigDecimal.ZERO);
            }

            // 青岛基地PPM
            detail.setQingdaoSuspiciousCount(BigDecimal.valueOf(data.qingdaoSuspiciousCount));
            detail.setQingdaoSupplyQty(BigDecimal.valueOf(data.qingdaoSupplyQty));
            if (data.qingdaoSupplyQty > 0) {
                detail.setQingdaoSupplierPpm(BigDecimal.valueOf(data.qingdaoSuspiciousCount)
                        .multiply(BigDecimal.valueOf(1_000_000))
                        .divide(BigDecimal.valueOf(data.qingdaoSupplyQty), 2, RoundingMode.HALF_UP));
            } else {
                detail.setQingdaoSupplierPpm(BigDecimal.ZERO);
            }

            // 重庆基地PPM
            detail.setChongqingSuspiciousCount(BigDecimal.valueOf(data.chongqingSuspiciousCount));
            detail.setChongqingSupplyQty(BigDecimal.valueOf(data.chongqingSupplyQty));
            if (data.chongqingSupplyQty > 0) {
                detail.setChongqingSupplierPpm(BigDecimal.valueOf(data.chongqingSuspiciousCount)
                        .multiply(BigDecimal.valueOf(1_000_000))
                        .divide(BigDecimal.valueOf(data.chongqingSupplyQty), 2, RoundingMode.HALF_UP));
            } else {
                detail.setChongqingSupplierPpm(BigDecimal.ZERO);
            }

            // 写入排除后的供货量（用于基地/公司总体PPM计算）
            detail.setMonthSupplyQtyExcluded(BigDecimal.valueOf(data.monthSupplyQtyExcluded));
            detail.setHexiSupplyQtyExcluded(BigDecimal.valueOf(data.hexiSupplyQtyExcluded));
            detail.setBaojunSupplyQtyExcluded(BigDecimal.valueOf(data.baojunSupplyQtyExcluded));
            detail.setQingdaoSupplyQtyExcluded(BigDecimal.valueOf(data.qingdaoSupplyQtyExcluded));
            detail.setChongqingSupplyQtyExcluded(BigDecimal.valueOf(data.chongqingSupplyQtyExcluded));

            toSave.add(detail);
        }

        if (!toSave.isEmpty()) {
            detailRepository.saveAll(toSave);
            log.info("供应商PPM明细汇总计算完成: ppmMonth={}, 写入 {} 条", ppmMonthStored, toSave.size());
        }
        
        return toSave.size();
    }

    /**
     * 供应商PPM数据聚合类
     * 双轨计算：同时记录全量供货量和排除螺栓/螺母/卡扣后的供货量
     */
    private static class SupplierPpmData {
        String supplierCode;
        String supplierName;
        int monthDefectCount = 0;
        int monthSupplyQty = 0;
        int monthSupplyQtyExcluded = 0;  // 排除螺栓/螺母/卡扣后的供货量

        int hexiSuspiciousCount = 0;
        int hexiSupplyQty = 0;
        int hexiSupplyQtyExcluded = 0;  // 排除后的河西供货量

        int baojunSuspiciousCount = 0;
        int baojunSupplyQty = 0;
        int baojunSupplyQtyExcluded = 0;  // 排除后的宝骏供货量

        int qingdaoSuspiciousCount = 0;
        int qingdaoSupplyQty = 0;
        int qingdaoSupplyQtyExcluded = 0;  // 排除后的青岛供货量

        int chongqingSuspiciousCount = 0;
        int chongqingSupplyQty = 0;
        int chongqingSupplyQtyExcluded = 0;  // 排除后的重庆供货量
    }

    /** 判断是否排除零件（螺栓/螺母/卡扣） */
    private static boolean isExcludedPartName(String partName) {
        if (partName == null || partName.isEmpty()) return false;
        String p = partName.trim();
        boolean excluded = p.contains("螺栓") || p.contains("螺母") || p.contains("卡扣");
        if (excluded) {
            log.debug("排除零件: {}", partName);
        }
        return excluded;
    }

    /** 仅统计「是否供应商责任=Y」的不合格数（与飞书口径一致） */
    private static boolean isSupplierResponsible(String supplierResp) {
        if (supplierResp == null) return false;
        return "Y".equalsIgnoreCase(supplierResp.trim());
    }



    /**
     * 若 base_info 为空，则自动插入系统内常用的几个基地，避免首次部署时未初始化导致无法计算 PPM。
     * - base_code/base_name：河西、宝骏、青岛、重庆
     */
    private void ensureBaseInfoInitialized() {
        if (baseInfoRepository.count() > 0) {
            return;
        }
        List<BaseInfo> defaults = new ArrayList<>();
        defaults.add(newBase("河西", "河西"));
        defaults.add(newBase("宝骏", "宝骏"));
        defaults.add(newBase("青岛", "青岛"));
        defaults.add(newBase("重庆", "重庆"));
        baseInfoRepository.saveAll(defaults);
        log.info("base_info 为空，已自动初始化默认基地数据: 河西/宝骏/青岛/重庆");
    }

    private static BaseInfo newBase(String code, String name) {
        BaseInfo b = new BaseInfo();
        b.setBaseCode(code);
        b.setBaseName(name);
        return b;
    }

    private static String mapPlantToBase(String plant) {
        if (plant == null) return null;
        String p = plant.trim();
        if (p.contains("河西")) return "河西";
        if (p.contains("宝骏")) return "宝骏";
        if (p.contains("青岛")) return "青岛";
        if (p.contains("重庆")) return "重庆";
        return null;
    }
}
