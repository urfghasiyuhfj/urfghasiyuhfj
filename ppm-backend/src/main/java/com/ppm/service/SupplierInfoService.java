package com.ppm.service;

import com.ppm.entity.SupplierInfo;
import com.ppm.repository.SupplierInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 供应商信息服务
 */
@Service
@RequiredArgsConstructor
public class SupplierInfoService {

    private final SupplierInfoRepository supplierInfoRepository;

    public List<SupplierInfo> listAll() {
        return supplierInfoRepository.findAll();
    }

    public Optional<SupplierInfo> getById(Long id) {
        return supplierInfoRepository.findById(id);
    }

    public Optional<SupplierInfo> getByCode(String supplierCode) {
        return supplierInfoRepository.findBySupplierCode(supplierCode);
    }

    @Transactional(rollbackFor = Exception.class)
    public SupplierInfo create(SupplierInfo supplierInfo) {
        if (supplierInfoRepository.findBySupplierCode(supplierInfo.getSupplierCode()).isPresent()) {
            throw new IllegalArgumentException("供应商编码已存在: " + supplierInfo.getSupplierCode());
        }
        return supplierInfoRepository.save(supplierInfo);
    }

    @Transactional(rollbackFor = Exception.class)
    public SupplierInfo update(Long id, SupplierInfo supplierInfo) {
        SupplierInfo existing = supplierInfoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("供应商不存在: " + id));
        
        // 检查编码是否被其他记录使用
        Optional<SupplierInfo> other = supplierInfoRepository.findBySupplierCode(supplierInfo.getSupplierCode());
        if (other.isPresent() && !other.get().getId().equals(id)) {
            throw new IllegalArgumentException("供应商编码已被其他记录使用: " + supplierInfo.getSupplierCode());
        }
        
        existing.setSupplierCode(supplierInfo.getSupplierCode());
        existing.setSupplierName(supplierInfo.getSupplierName());
        return supplierInfoRepository.save(existing);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (!supplierInfoRepository.existsById(id)) {
            throw new IllegalArgumentException("供应商不存在: " + id);
        }
        supplierInfoRepository.deleteById(id);
    }
}
