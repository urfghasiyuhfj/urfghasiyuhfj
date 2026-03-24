package com.ppm.service;

import com.ppm.dto.SuspiciousMaterialStatsVo;
import com.ppm.entity.SuspiciousMaterial;
import com.ppm.repository.SuspiciousMaterialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SuspiciousStatsService {

    private final SuspiciousMaterialRepository suspiciousMaterialRepository;

    public SuspiciousMaterialStatsVo getStats(LocalDate recordDateFrom, LocalDate recordDateTo) {
        Specification<SuspiciousMaterial> spec = (root, q, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (recordDateFrom != null) {
                ps.add(cb.greaterThanOrEqualTo(root.get("recordDate"), recordDateFrom));
            }
            if (recordDateTo != null) {
                ps.add(cb.lessThanOrEqualTo(root.get("recordDate"), recordDateTo));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        List<SuspiciousMaterial> list = suspiciousMaterialRepository.findAll(spec);

        Map<String, Long> plantCount = new LinkedHashMap<>();
        Map<String, Long> faultTypeCount = new LinkedHashMap<>();
        Map<String, Long> failureDescCount = new LinkedHashMap<>();
        Map<String, Long> supplierCount = new LinkedHashMap<>();
        Map<String, Long> monthCount = new LinkedHashMap<>();

        for (SuspiciousMaterial m : list) {
            String plant = m.getPlant() != null ? m.getPlant().trim() : "未分类";
            plantCount.merge(plant, 1L, Long::sum);

            String faultType = m.getFaultType() != null && !m.getFaultType().isBlank()
                    ? m.getFaultType().trim() : "未分类";
            faultTypeCount.merge(faultType, 1L, Long::sum);

            String failureDesc = m.getFailureDesc() != null && !m.getFailureDesc().isBlank()
                    ? m.getFailureDesc().trim() : "未分类";
            failureDescCount.merge(failureDesc, 1L, Long::sum);

            String supplierKey = (m.getSupplierName() != null ? m.getSupplierName().trim() : "")
                    + "(" + (m.getSupplierCode() != null ? m.getSupplierCode() : "") + ")";
            if (supplierKey.equals("()")) supplierKey = "未知";
            supplierCount.merge(supplierKey, 1L, Long::sum);

            if (m.getRecordDate() != null) {
                String monthKey = String.format("%d%02d", m.getRecordDate().getYear(), m.getRecordDate().getMonthValue());
                monthCount.merge(monthKey, 1L, Long::sum);
            }
        }

        List<SuspiciousMaterialStatsVo.NameCount> byPlant = toNameCountList(plantCount);
        List<SuspiciousMaterialStatsVo.NameCount> byFaultType = toNameCountList(faultTypeCount);
        List<SuspiciousMaterialStatsVo.NameCount> byFailureDesc = toNameCountList(failureDescCount);

        List<SuspiciousMaterialStatsVo.NameCount> bySupplier = supplierCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(15)
                .map(e -> new SuspiciousMaterialStatsVo.NameCount(e.getKey(), e.getValue()))
                .collect(Collectors.toList());

        List<SuspiciousMaterialStatsVo.NameCount> byMonth = monthCount.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new SuspiciousMaterialStatsVo.NameCount(e.getKey(), e.getValue()))
                .collect(Collectors.toList());

        return new SuspiciousMaterialStatsVo(byPlant, byFaultType, byFailureDesc, bySupplier, byMonth, list.size());
    }

    private static List<SuspiciousMaterialStatsVo.NameCount> toNameCountList(Map<String, Long> map) {
        return map.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> new SuspiciousMaterialStatsVo.NameCount(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }
}
