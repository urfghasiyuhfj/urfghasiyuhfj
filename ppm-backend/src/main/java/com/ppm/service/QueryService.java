package com.ppm.service;

import com.ppm.dto.PageResult;
import com.ppm.dto.QuerySupplyDto;
import com.ppm.dto.QuerySuspiciousDto;
import com.ppm.dto.SupplyVolumeVo;
import com.ppm.entity.SupplyVolume;
import com.ppm.entity.SuspiciousMaterial;
import com.ppm.repository.SupplyVolumeRepository;
import com.ppm.repository.SuspiciousMaterialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QueryService {

    private final SuspiciousMaterialRepository suspiciousMaterialRepository;
    private final SupplyVolumeRepository supplyVolumeRepository;

    public PageResult<SuspiciousMaterial> pageSuspicious(QuerySuspiciousDto dto) {
        var pr = PageRequest.of(
                Math.max(0, (dto.getPage() == null ? 1 : dto.getPage()) - 1),
                Math.min(100, Math.max(1, dto.getSize() == null ? 20 : dto.getSize())),
                Sort.by(Sort.Direction.DESC, "recordDate", "id"));
        Specification<SuspiciousMaterial> spec = (root, q, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (dto.getPlant() != null && !dto.getPlant().isBlank()) {
                ps.add(cb.like(root.get("plant"), "%" + dto.getPlant().trim() + "%"));
            }
            if (dto.getSupplierCode() != null && !dto.getSupplierCode().isBlank()) {
                ps.add(cb.like(root.get("supplierCode"), "%" + dto.getSupplierCode().trim() + "%"));
            }
            if (dto.getSupplierName() != null && !dto.getSupplierName().isBlank()) {
                ps.add(cb.like(root.get("supplierName"), "%" + dto.getSupplierName().trim() + "%"));
            }
            if (dto.getPartCode() != null && !dto.getPartCode().isBlank()) {
                ps.add(cb.like(root.get("partCode"), "%" + dto.getPartCode().trim() + "%"));
            }
            if (dto.getPartName() != null && !dto.getPartName().isBlank()) {
                ps.add(cb.like(root.get("partName"), "%" + dto.getPartName().trim() + "%"));
            }
            if (dto.getRecordDateFrom() != null) {
                ps.add(cb.greaterThanOrEqualTo(root.get("recordDate"), dto.getRecordDateFrom()));
            }
            if (dto.getRecordDateTo() != null) {
                ps.add(cb.lessThanOrEqualTo(root.get("recordDate"), dto.getRecordDateTo()));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        Page<SuspiciousMaterial> page = suspiciousMaterialRepository.findAll(spec, pr);
        return PageResult.of(page.getContent(), page.getTotalElements());
    }

    public PageResult<SupplyVolumeVo> pageSupply(QuerySupplyDto dto) {
        // 构建排序
        Sort sort = buildSort(dto.getSortField(), dto.getSortOrder());
        if (sort.isUnsorted()) {
            sort = Sort.by(Sort.Direction.DESC, "fiscalYear", "baseCode", "plantId", "id");
        }

        var pr = PageRequest.of(
                Math.max(0, (dto.getPage() == null ? 1 : dto.getPage()) - 1),
                Math.min(100, Math.max(1, dto.getSize() == null ? 20 : dto.getSize())),
                sort);
        Specification<SupplyVolume> spec = (root, q, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (dto.getFiscalYear() != null) {
                ps.add(cb.equal(root.get("fiscalYear"), dto.getFiscalYear().shortValue()));
            }
            // 注意：ppmMonth字段已移除，供货量现在按月份字段存储（month_1_no ~ month_12_no）
            // 如需按月份筛选，请在查询后根据getMonthXNo(month)方法过滤
            if (dto.getBaseCode() != null && !dto.getBaseCode().isBlank()) {
                ps.add(cb.equal(root.get("baseCode"), dto.getBaseCode().trim()));
            }
            if (dto.getPlantId() != null && !dto.getPlantId().isBlank()) {
                ps.add(cb.like(root.get("plantId"), "%" + dto.getPlantId().trim() + "%"));
            }
            if (dto.getSupplierCode() != null && !dto.getSupplierCode().isBlank()) {
                ps.add(cb.like(root.get("supplierCode"), "%" + dto.getSupplierCode().trim() + "%"));
            }
            // 新增：供应商名称筛选
            if (dto.getSupplierName() != null && !dto.getSupplierName().isBlank()) {
                ps.add(cb.like(root.get("supplierName"), "%" + dto.getSupplierName().trim() + "%"));
            }
            // 新增：零件号筛选
            if (dto.getPartCode() != null && !dto.getPartCode().isBlank()) {
                ps.add(cb.like(root.get("partCode"), "%" + dto.getPartCode().trim() + "%"));
            }
            // 新增：零件名称筛选
            if (dto.getPartName() != null && !dto.getPartName().isBlank()) {
                ps.add(cb.like(root.get("partName"), "%" + dto.getPartName().trim() + "%"));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        Page<SupplyVolume> page = supplyVolumeRepository.findAll(spec, pr);
        // 转换为VO
        List<SupplyVolumeVo> voList = page.getContent().stream()
                .map(SupplyVolumeVo::fromEntity)
                .toList();
        return PageResult.of(voList, page.getTotalElements());
    }

    /**
     * 构建排序
     */
    private Sort buildSort(String sortField, String sortOrder) {
        if (sortField == null || sortField.isBlank()) {
            return Sort.unsorted();
        }
        Sort.Direction direction = "asc".equalsIgnoreCase(sortOrder)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        return Sort.by(direction, sortField);
    }
}
