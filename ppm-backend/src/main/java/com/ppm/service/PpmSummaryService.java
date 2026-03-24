package com.ppm.service;

import com.ppm.dto.PageResult;
import com.ppm.dto.PpmBaseTrendVo;
import com.ppm.dto.PpmGlobalMonthlyVo;
import com.ppm.dto.PpmSummaryQueryDto;
import com.ppm.dto.PpmSummaryVo;
import com.ppm.dto.PpmSupplierMonthlyTrendVo;
import com.ppm.dto.PpmTrendVo;
import com.ppm.entity.SupplierPpmDetail;
import com.ppm.repository.SupplierPpmDetailRepository;
import com.ppm.util.SupplierPpmDetailUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import jakarta.persistence.criteria.Predicate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * PPM 汇总查询服务：从 supplier_ppm_detail 表聚合数据。
 * supplier_ppm_detail 表按供应商×月份存储，包含四大基地的详细数据。
 */
@Service
@RequiredArgsConstructor
public class PpmSummaryService {

    private final SupplierPpmDetailRepository detailRepository;

    /**
     * 分页查询 PPM 汇总
     * 如果指定了 baseCode，则按基地维度展示；否则按供应商整体维度展示
     */
    public PageResult<PpmSummaryVo> page(PpmSummaryQueryDto dto) {
        PageRequest pr = PageRequest.of(
                Math.max(0, dto.getPage() - 1),
                Math.min(100, Math.max(1, dto.getSize())),
                Sort.by(Sort.Direction.DESC, "supplierTotalPpm"));
        
        Specification<SupplierPpmDetail> spec = (root, q, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (dto.getPpmMonth() != null && !dto.getPpmMonth().isBlank()) {
                ps.add(cb.equal(root.get("ppmMonth"), dto.getPpmMonth()));
            }
            if (dto.getSupplierCode() != null && !dto.getSupplierCode().isBlank()) {
                ps.add(cb.like(root.get("supplierCode"), "%" + dto.getSupplierCode() + "%"));
            }
            if (dto.getSupplierName() != null && !dto.getSupplierName().isBlank()) {
                ps.add(cb.like(root.get("supplierName"), "%" + dto.getSupplierName() + "%"));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };

        Page<SupplierPpmDetail> page = detailRepository.findAll(spec, pr);

        // 按供应商整体维度展示
        List<PpmSummaryVo> list = page.getContent().stream()
                .map(this::toVo)
                .collect(Collectors.toList());
        return PageResult.of(list, page.getTotalElements());
    }

    /**
     * 按月份查询 PPM 列表（用于统计/图表）
     */
    public List<PpmSummaryVo> listByMonth(String ppmMonth) {
        return detailRepository.findByPpmMonthOrderBySupplierTotalPpmDesc(ppmMonth).stream()
                .map(this::toVo)
                .collect(Collectors.toList());
    }

    /**
     * 基地月度 PPM 趋势：按 base_code 从 supplier_ppm_detail 中聚合
     */
    public PpmBaseTrendVo getTrendByBase(String baseCode, Integer limitMonths) {
        if (baseCode == null || baseCode.isBlank()) {
            return PpmBaseTrendVo.builder()
                    .months(List.of())
                    .defectCounts(List.of())
                    .supplyQtys(List.of())
                    .ppmValues(List.of())
                    .build();
        }
        
        List<String> allMonths = detailRepository.findDistinctPpmMonths();
        int limit = limitMonths != null && limitMonths > 0 ? Math.min(limitMonths, 24) : 12;
        int from = Math.max(0, allMonths.size() - limit);
        List<String> months = allMonths.subList(from, allMonths.size());
        
        if (months.isEmpty()) {
            return PpmBaseTrendVo.builder()
                    .months(List.of())
                    .defectCounts(List.of())
                    .supplyQtys(List.of())
                    .ppmValues(List.of())
                    .build();
        }
        
        List<SupplierPpmDetail> rows = detailRepository.findByPpmMonthInOrderByPpmMonthAsc(months);
        Map<String, List<SupplierPpmDetail>> byMonth = rows.stream()
                .collect(Collectors.groupingBy(SupplierPpmDetail::getPpmMonth));
        
        List<Integer> defectCounts = new ArrayList<>();
        List<Integer> supplyQtys = new ArrayList<>();
        List<BigDecimal> ppmValues = new ArrayList<>();
        
        for (String m : months) {
            List<SupplierPpmDetail> list = byMonth.getOrDefault(m, List.of());
            int defects = list.stream()
                    .mapToInt(d -> SupplierPpmDetailUtils.getBaseDefectCount(d, baseCode))
                    .sum();
            // 基地总体PPM使用排除后的供货量（排除螺栓/螺母/卡扣）
            int supply = list.stream()
                    .mapToInt(d -> SupplierPpmDetailUtils.getBaseSupplyQtyExcluded(d, baseCode))
                    .sum();
            defectCounts.add(defects);
            supplyQtys.add(supply);
            if (supply == 0) {
                ppmValues.add(BigDecimal.ZERO);
            } else {
                ppmValues.add(BigDecimal.valueOf(defects)
                        .multiply(BigDecimal.valueOf(1_000_000))
                        .divide(BigDecimal.valueOf(supply), 2, RoundingMode.HALF_UP));
            }
        }
        
        return PpmBaseTrendVo.builder()
                .months(months)
                .defectCounts(defectCounts)
                .supplyQtys(supplyQtys)
                .ppmValues(ppmValues)
                .build();
    }

    /**
     * 全局每月总 PPM：从 supplier_ppm_detail 中按月份聚合
     */
    public PpmGlobalMonthlyVo getGlobalMonthly(Integer limitMonths) {
        List<String> allMonths = detailRepository.findDistinctPpmMonths();
        int limit = limitMonths != null && limitMonths > 0 ? Math.min(limitMonths, 24) : 12;
        List<String> months = allMonths.stream().limit(limit).sorted().toList();
        
        if (months.isEmpty()) {
            return PpmGlobalMonthlyVo.builder().items(List.of()).build();
        }

        List<SupplierPpmDetail> rows = detailRepository.findByPpmMonthInOrderByPpmMonthAsc(months);
        Map<String, List<SupplierPpmDetail>> byMonth = rows.stream()
                .collect(Collectors.groupingBy(SupplierPpmDetail::getPpmMonth));
        
        List<PpmGlobalMonthlyVo.Item> items = new ArrayList<>();
        for (String m : months) {
            List<SupplierPpmDetail> list = byMonth.getOrDefault(m, List.of());
            int defects = list.stream()
                    .mapToInt(d -> d.getMonthDefectCount() != null ? d.getMonthDefectCount().intValue() : 0)
                    .sum();
            // 公司总体PPM使用排除后的供货量（排除螺栓/螺母/卡扣）
            int supply = list.stream()
                    .mapToInt(d -> d.getMonthSupplyQtyExcluded() != null ? d.getMonthSupplyQtyExcluded().intValue() : 0)
                    .sum();

            BigDecimal ppm = supply == 0 ? BigDecimal.ZERO
                    : BigDecimal.valueOf(defects)
                    .multiply(BigDecimal.valueOf(1_000_000))
                    .divide(BigDecimal.valueOf(supply), 2, RoundingMode.HALF_UP);
            items.add(new PpmGlobalMonthlyVo.Item(m, defects, supply, ppm));
        }
        return PpmGlobalMonthlyVo.builder().items(items).build();
    }

    /**
     * 供应商月度 PPM TOP15：取最近 N 月内按 PPM 取 TOP15 供应商，再回填这些供应商在各月的 PPM。
     */
    public PpmSupplierMonthlyTrendVo getSupplierMonthlyTrend(Integer limitMonths) {
        List<String> allMonths = detailRepository.findDistinctPpmMonths();
        int limit = limitMonths != null && limitMonths > 0 ? Math.min(limitMonths, 24) : 12;
        List<String> months = allMonths.stream().limit(limit).sorted().toList();
        
        if (months.isEmpty()) {
            return PpmSupplierMonthlyTrendVo.builder().months(List.of()).series(List.of()).build();
        }
        
        List<SupplierPpmDetail> rows = detailRepository.findByPpmMonthInOrderByPpmMonthAscSupplierTotalPpmDesc(months);
        Map<String, Map<String, SupplierPpmDetail>> monthSupplier = new LinkedHashMap<>();
        for (SupplierPpmDetail r : rows) {
            monthSupplier
                    .computeIfAbsent(r.getPpmMonth(), k -> new LinkedHashMap<>())
                    .put(r.getSupplierCode(), r);
        }
        
        String lastMonth = months.get(months.size() - 1);
        Map<String, SupplierPpmDetail> lastMonthMap = monthSupplier.getOrDefault(lastMonth, Map.of());
        List<SupplierPpmDetail> top15 = lastMonthMap.values().stream()
                .sorted((a, b) -> {
                    BigDecimal ppmA = a.getSupplierTotalPpm() != null ? a.getSupplierTotalPpm() : BigDecimal.ZERO;
                    BigDecimal ppmB = b.getSupplierTotalPpm() != null ? b.getSupplierTotalPpm() : BigDecimal.ZERO;
                    return ppmB.compareTo(ppmA);
                })
                .limit(15)
                .toList();
        
        List<PpmSupplierMonthlyTrendVo.SupplierSeries> series = new ArrayList<>();
        for (SupplierPpmDetail s : top15) {
            List<BigDecimal> ppmValues = new ArrayList<>();
            for (String m : months) {
                SupplierPpmDetail cell = monthSupplier.getOrDefault(m, Map.of()).get(s.getSupplierCode());
                ppmValues.add(cell != null && cell.getSupplierTotalPpm() != null 
                        ? cell.getSupplierTotalPpm() : BigDecimal.ZERO);
            }
            series.add(new PpmSupplierMonthlyTrendVo.SupplierSeries(
                    s.getSupplierCode(),
                    s.getSupplierName(),
                    ppmValues));
        }
        return PpmSupplierMonthlyTrendVo.builder().months(months).series(series).build();
    }

    /**
     * 获取所有可用的 PPM 月份列表（倒序）
     */
    public List<String> getAvailableMonths() {
        List<String> months = detailRepository.findDistinctPpmMonths();
        return months.stream().sorted((a, b) -> b.compareTo(a)).toList();
    }

    /**
     * PPM 趋势数据：按月份聚合，支持按基地分系列。限制最近 N 个月。
     * 公司总体PPM = 总不合格数 / 总供货量 × 1,000,000
     * 基地PPM = 基地总不合格数 / 基地总供货量 × 1,000,000
     */
    public PpmTrendVo getTrend(Integer limitMonths) {
        List<String> allMonths = detailRepository.findDistinctPpmMonths();
        int limit = limitMonths != null && limitMonths > 0 ? Math.min(limitMonths, 24) : 12;
        List<String> months = allMonths.stream().limit(limit).sorted().toList();

        if (months.isEmpty()) {
            return PpmTrendVo.builder().months(List.of()).byBase(List.of()).avgPpmByMonth(List.of()).build();
        }

        List<SupplierPpmDetail> rows = detailRepository.findByPpmMonthInOrderByPpmMonthAsc(months);
        Map<String, List<SupplierPpmDetail>> byMonth = rows.stream()
                .collect(Collectors.groupingBy(SupplierPpmDetail::getPpmMonth));

        // 按基地聚合
        Map<String, Map<String, Integer>> baseMonthDefects = new LinkedHashMap<>();
        Map<String, Map<String, Integer>> baseMonthSupply = new LinkedHashMap<>();
        List<BigDecimal> companyPpmByMonth = new ArrayList<>();

        for (String m : months) {
            List<SupplierPpmDetail> list = byMonth.getOrDefault(m, List.of());

            // 计算公司总体：总不合格数 / 总供货量（排除螺栓/螺母/卡扣）
            int totalDefects = list.stream()
                    .mapToInt(d -> d.getMonthDefectCount() != null ? d.getMonthDefectCount().intValue() : 0)
                    .sum();
            int totalSupply = list.stream()
                    .mapToInt(d -> d.getMonthSupplyQtyExcluded() != null ? d.getMonthSupplyQtyExcluded().intValue() : 0)
                    .sum();

            BigDecimal companyPpm = totalSupply > 0
                    ? BigDecimal.valueOf(totalDefects)
                            .multiply(BigDecimal.valueOf(1_000_000))
                            .divide(BigDecimal.valueOf(totalSupply), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            companyPpmByMonth.add(companyPpm);

            // 按基地分组统计缺陷数和供货量（基地总体使用排除后的供货量）
            for (String baseName : List.of("河西", "宝骏", "青岛", "重庆")) {
                int baseDefects = list.stream()
                        .mapToInt(d -> SupplierPpmDetailUtils.getBaseDefectCount(d, baseName))
                        .sum();
                int baseSupply = list.stream()
                        .mapToInt(d -> SupplierPpmDetailUtils.getBaseSupplyQtyExcluded(d, baseName))
                        .sum();

                baseMonthDefects
                        .computeIfAbsent(baseName, k -> new LinkedHashMap<>())
                        .put(m, baseDefects);
                baseMonthSupply
                        .computeIfAbsent(baseName, k -> new LinkedHashMap<>())
                        .put(m, baseSupply);
            }
        }

        List<PpmTrendVo.BaseSeries> byBase = new ArrayList<>();
        for (String baseName : List.of("河西", "宝骏", "青岛", "重庆")) {
            List<BigDecimal> vals = new ArrayList<>();
            for (String m : months) {
                Integer defects = baseMonthDefects.getOrDefault(baseName, Map.of()).getOrDefault(m, 0);
                Integer supply = baseMonthSupply.getOrDefault(baseName, Map.of()).getOrDefault(m, 0);
                BigDecimal basePpm = supply > 0
                        ? BigDecimal.valueOf(defects)
                                .multiply(BigDecimal.valueOf(1_000_000))
                                .divide(BigDecimal.valueOf(supply), 2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;
                vals.add(basePpm);
            }
            byBase.add(PpmTrendVo.BaseSeries.builder().baseName(baseName).ppmValues(vals).build());
        }

        return PpmTrendVo.builder()
                .months(months)
                .byBase(byBase)
                .avgPpmByMonth(companyPpmByMonth)
                .build();
    }

    // ============ 辅助方法 ============

    private PpmSummaryVo toVo(SupplierPpmDetail d) {
        return PpmSummaryVo.builder()
                .id(d.getId())
                .ppmMonth(d.getPpmMonth())
                .baseCode(null)  // 供应商整体维度，无特定基地
                .baseName(null)
                .supplierCode(d.getSupplierCode())
                .supplierName(d.getSupplierName())
                .defectCount(d.getMonthDefectCount() != null ? d.getMonthDefectCount().intValue() : 0)
                .supplyQty(d.getMonthSupplyQty() != null ? d.getMonthSupplyQty().intValue() : 0)
                .ppm(d.getSupplierTotalPpm())
                .createdAt(d.getCreatedAt())
                .build();
    }

    private PpmSummaryVo toVoByBase(SupplierPpmDetail d, String baseCode) {
        return PpmSummaryVo.builder()
                .id(d.getId())
                .ppmMonth(d.getPpmMonth())
                .baseCode(baseCode)
                .baseName(baseCode)
                .supplierCode(d.getSupplierCode())
                .supplierName(d.getSupplierName())
                .defectCount(SupplierPpmDetailUtils.getBaseDefectCount(d, baseCode))
                .supplyQty(SupplierPpmDetailUtils.getBaseSupplyQty(d, baseCode))
                .ppm(SupplierPpmDetailUtils.getBasePpm(d, baseCode))
                .createdAt(d.getCreatedAt())
                .build();
    }
}
