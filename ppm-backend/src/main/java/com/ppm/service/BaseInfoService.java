package com.ppm.service;

import com.ppm.dto.BaseInfoDto;
import com.ppm.entity.BaseInfo;
import com.ppm.repository.BaseInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BaseInfoService {

    private final BaseInfoRepository baseInfoRepository;

    public List<BaseInfo> listAll() {
        return baseInfoRepository.findAllByOrderByBaseCodeAsc();
    }

    public BaseInfo getById(Long id) {
        return baseInfoRepository.findById(id).orElse(null);
    }

    public BaseInfo getByCode(String baseCode) {
        return baseInfoRepository.findByBaseCode(baseCode).orElse(null);
    }

    @Transactional(rollbackFor = Exception.class)
    public BaseInfo create(BaseInfoDto dto) {
        String code = dto.getBaseCode() != null ? dto.getBaseCode().trim() : "";
        String name = dto.getBaseName() != null ? dto.getBaseName().trim() : "";
        if (baseInfoRepository.existsByBaseCode(code)) {
            throw new IllegalArgumentException("基地编码已存在: " + code);
        }
        BaseInfo entity = new BaseInfo();
        entity.setBaseCode(code);
        entity.setBaseName(name);
        return baseInfoRepository.save(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public BaseInfo update(Long id, BaseInfoDto dto) {
        BaseInfo entity = baseInfoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("基地不存在: " + id));
        String code = dto.getBaseCode() != null ? dto.getBaseCode().trim() : "";
        String name = dto.getBaseName() != null ? dto.getBaseName().trim() : "";
        if (baseInfoRepository.existsByBaseCodeAndIdNot(code, id)) {
            throw new IllegalArgumentException("基地编码已被其他记录使用: " + code);
        }
        entity.setBaseCode(code);
        entity.setBaseName(name);
        return baseInfoRepository.save(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (!baseInfoRepository.existsById(id)) {
            throw new IllegalArgumentException("基地不存在: " + id);
        }
        baseInfoRepository.deleteById(id);
    }
}
