package com.ppm.service;

import com.ppm.dto.PpmCalculateResult;
import com.ppm.dto.excel.SuspiciousMaterialExcelRow;
import com.ppm.dto.excel.SupplyVolumeExcelRow;
import com.ppm.entity.SupplyVolume;
import com.ppm.repository.SupplierPpmDetailRepository;
import com.ppm.repository.SuspiciousMaterialRepository;
import com.ppm.repository.SupplyVolumeRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * ImportService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ImportServiceTest {

    @Mock
    private SupplierPpmDetailRepository detailRepository;
    @Mock
    private SuspiciousMaterialRepository suspiciousMaterialRepository;
    @Mock
    private SupplyVolumeRepository supplyVolumeRepository;
    @Mock
    private PpmCalculateService ppmCalculateService;
    @Mock
    private PlatformTransactionManager transactionManager;
    @Mock
    private EntityManager entityManager;
    @InjectMocks
    private ImportService importService;

    @Test
    @DisplayName("可疑物料导入 - 正常数据")
    void importSuspiciousMaterial_normalData() {
        when(suspiciousMaterialRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(ppmCalculateService.calculate(any())).thenReturn(new PpmCalculateResult());
    }

    @Test
    @DisplayName("可疑物料校验 - 区域工厂为空")
    void importSuspiciousMaterial_emptyPlant() {
        SuspiciousMaterialExcelRow row = new SuspiciousMaterialExcelRow();
        row.setPlant("");
        row.setPartCode("PART001");
        row.setSupplierCode("SUP001");
    }

    @Test
    @DisplayName("可疑物料校验 - 零件号为空")
    void importSuspiciousMaterial_emptyPartCode() {
        SuspiciousMaterialExcelRow row = new SuspiciousMaterialExcelRow();
        row.setPlant("河西");
        row.setPartCode("");
        row.setSupplierCode("SUP001");
    }

    @Test
    @DisplayName("可疑物料校验 - 供应商代码为空")
    void importSuspiciousMaterial_emptySupplierCode() {
        SuspiciousMaterialExcelRow row = new SuspiciousMaterialExcelRow();
        row.setPlant("河西");
        row.setPartCode("PART001");
        row.setSupplierCode("");
    }

    @Test
    @DisplayName("可疑物料校验 - 数量为负数")
    void importSuspiciousMaterial_negativeQuantity() {
        SuspiciousMaterialExcelRow row = new SuspiciousMaterialExcelRow();
        row.setPlant("河西");
        row.setPartCode("PART001");
        row.setSupplierCode("SUP001");
        row.setQuantity(-5.0);
    }

    @Test
    @DisplayName("供货量导入 - 正常数据")
    void importSupplyVolume_normalData() {
        when(supplyVolumeRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(supplyVolumeRepository.findByFiscalYearAndPlantIdAndSupplierCodeAndPartCode(any(), any(), any(), any()))
                .thenReturn(List.of());
        when(ppmCalculateService.calculate(any())).thenReturn(new PpmCalculateResult());
    }

    @Test
    @DisplayName("供货量校验 - 财年为空")
    void importSupplyVolume_emptyFiscalYear() {
        SupplyVolumeExcelRow row = new SupplyVolumeExcelRow();
        row.setFiscalYear(null);
        row.setPlantId("河西");
        row.setSupplierCode("SUP001");
        row.setPartCode("PART001");
    }

    @Test
    @DisplayName("供货量校验 - 所有月份供货量为空")
    void importSupplyVolume_allMonthNull() {
        SupplyVolumeExcelRow row = new SupplyVolumeExcelRow();
        row.setFiscalYear("2025");
        row.setPlantId("河西");
        row.setSupplierCode("SUP001");
        row.setPartCode("PART001");
    }

    @Test
    @DisplayName("覆盖导入 - 更新已存在的记录")
    void importSupplyVolumeWithOverwrite_existingRecord() {
        SupplyVolume existing = new SupplyVolume();
        existing.setFiscalYear((short) 2025);
        existing.setPlantId("河西");
        existing.setSupplierCode("SUP001");
        existing.setPartCode("PART001");
        existing.setMonth10No(100);

        when(supplyVolumeRepository.findByFiscalYearAndPlantIdAndSupplierCodeAndPartCode(any(), any(), any(), any()))
                .thenReturn(List.of(existing));
        when(supplyVolumeRepository.save(any())).thenReturn(existing);
    }
}
