package com.ppm.service;

import com.ppm.dto.PpmCalculateResult;
import com.ppm.entity.*;
import com.ppm.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * PpmCalculateService 单元测试
 * 测试覆盖：PPM计算核心逻辑、边界条件、数据聚合
 */
@ExtendWith(MockitoExtension.class)
class PpmCalculateServiceTest {

    @Mock
    private BaseInfoRepository baseInfoRepository;

    @Mock
    private SuspiciousMaterialRepository suspiciousMaterialRepository;

    @Mock
    private SupplyVolumeRepository supplyVolumeRepository;

    @Mock
    private SupplierPpmDetailRepository detailRepository;

    @InjectMocks
    private PpmCalculateService ppmCalculateService;

    // ==================== 参数校验测试 ====================

    @Test
    @DisplayName("ppmMonth为null时应抛出IllegalArgumentException")
    void calculate_nullPpmMonth_throwsException() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> ppmCalculateService.calculate(null)
        );
        assertTrue(ex.getMessage().contains("ppm_month"));
    }

    @Test
    @DisplayName("ppmMonth格式不正确时应抛出IllegalArgumentException")
    void calculate_invalidPpmMonthFormat_throwsException() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> ppmCalculateService.calculate("2025-10")
        );
        assertTrue(ex.getMessage().contains("格式应为 yyyyMM"));
    }

    @Test
    @DisplayName("ppmMonth长度不足时应抛出IllegalArgumentException")
    void calculate_shortPpmMonth_throwsException() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> ppmCalculateService.calculate("20")
        );
        assertTrue(ex.getMessage().contains("ppm_month"));
    }

    // ==================== 正常计算测试 ====================

    @Test
    @DisplayName("正常PPM计算 - 有可疑物料和供货量")
    void calculate_normalCase_success() {
        // Given
        String ppmMonth = "202510";
        when(baseInfoRepository.count()).thenReturn(4L);

        // Mock 可疑物料数据
        SuspiciousMaterial suspMaterial = createSuspiciousMaterial(
                "SUP001", "供应商A", "河西", LocalDate.of(2025, 10, 15), 10, "Y"
        );
        when(suspiciousMaterialRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(suspMaterial));

        // Mock 供货量数据
        SupplyVolume supplyVolume = createSupplyVolume(
                "SUP001", "供应商A", "河西", (short) 2025, 10000, 10
        );
        when(supplyVolumeRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(supplyVolume));

        when(detailRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        PpmCalculateResult result = ppmCalculateService.calculate(ppmMonth);

        // Then
        assertNotNull(result);
        assertEquals("202510", result.getPpmMonth());
        assertEquals(1, result.getSavedCount());

        // 验证PPM计算: (10 / 10000) * 1,000,000 = 1000
        ArgumentCaptor<List<SupplierPpmDetail>> captor = ArgumentCaptor.forClass(List.class);
        verify(detailRepository).saveAll(captor.capture());
        SupplierPpmDetail saved = captor.getValue().get(0);

        assertEquals("SUP001", saved.getSupplierCode());
        assertEquals("供应商A", saved.getSupplierName());
        assertEquals(0, saved.getSupplierTotalPpm().compareTo(BigDecimal.valueOf(1000)));
        assertEquals(0, saved.getHexiSupplierPpm().compareTo(BigDecimal.valueOf(1000)));
    }

    @Test
    @DisplayName("PPM计算 - 多基地数据聚合")
    void calculate_multipleBases_aggregatesCorrectly() {
        // Given
        String ppmMonth = "202510";
        when(baseInfoRepository.count()).thenReturn(4L);

        // Mock 多基地可疑物料
        SuspiciousMaterial hexiDefect = createSuspiciousMaterial(
                "SUP001", "供应商A", "河西", LocalDate.of(2025, 10, 1), 5, "Y"
        );
        SuspiciousMaterial baojunDefect = createSuspiciousMaterial(
                "SUP001", "供应商A", "宝骏", LocalDate.of(2025, 10, 5), 3, "Y"
        );
        when(suspiciousMaterialRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(hexiDefect, baojunDefect));

        // Mock 多基地供货量
        SupplyVolume hexiSupply = createSupplyVolume(
                "SUP001", "供应商A", "河西", (short) 2025, 5000, 10
        );
        SupplyVolume baojunSupply = createSupplyVolume(
                "SUP001", "供应商A", "宝骏", (short) 2025, 3000, 10
        );
        when(supplyVolumeRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(hexiSupply, baojunSupply));

        when(detailRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        PpmCalculateResult result = ppmCalculateService.calculate(ppmMonth);

        // Then
        ArgumentCaptor<List<SupplierPpmDetail>> captor = ArgumentCaptor.forClass(List.class);
        verify(detailRepository).saveAll(captor.capture());
        SupplierPpmDetail saved = captor.getValue().get(0);

        // 验证各基地PPM
        // 河西: 5缺陷 / 5000供货量 * 1M = 1000
        assertEquals(0, saved.getHexiSupplierPpm().compareTo(BigDecimal.valueOf(1000)));
        // 宝骏: 3缺陷 / 3000供货量 * 1M = 1000
        assertEquals(0, saved.getBaojunSupplierPpm().compareTo(BigDecimal.valueOf(1000)));
        // 总体: 8缺陷 / 8000供货量 * 1M = 1000
        assertEquals(0, saved.getSupplierTotalPpm().compareTo(BigDecimal.valueOf(1000)));
    }

    // ==================== 边界条件测试 ====================

    @Test
    @DisplayName("供货量为0时PPM应为0")
    void calculate_zeroSupplyQty_ppmIsZero() {
        // Given
        String ppmMonth = "202510";
        when(baseInfoRepository.count()).thenReturn(4L);

        SuspiciousMaterial suspMaterial = createSuspiciousMaterial(
                "SUP001", "供应商A", "河西", LocalDate.of(2025, 10, 15), 10, "Y"
        );
        when(suspiciousMaterialRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(suspMaterial));

        // 供货量为0
        SupplyVolume supplyVolume = createSupplyVolume(
                "SUP001", "供应商A", "河西", (short) 2025, 0, 10
        );
        when(supplyVolumeRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(supplyVolume));

        when(detailRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        PpmCalculateResult result = ppmCalculateService.calculate(ppmMonth);

        // Then
        ArgumentCaptor<List<SupplierPpmDetail>> captor = ArgumentCaptor.forClass(List.class);
        verify(detailRepository).saveAll(captor.capture());
        SupplierPpmDetail saved = captor.getValue().get(0);

        assertEquals(0, saved.getSupplierTotalPpm().compareTo(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("无可疑物料数据时仍能正常计算")
    void calculate_noSuspiciousMaterial_stillCalculates() {
        // Given
        String ppmMonth = "202510";
        when(baseInfoRepository.count()).thenReturn(4L);
        when(suspiciousMaterialRepository.findAll(any(Specification.class)))
                .thenReturn(List.of());

        SupplyVolume supplyVolume = createSupplyVolume(
                "SUP001", "供应商A", "河西", (short) 2025, 10000, 10
        );
        when(supplyVolumeRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(supplyVolume));

        when(detailRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        PpmCalculateResult result = ppmCalculateService.calculate(ppmMonth);

        // Then
        assertEquals(1, result.getSavedCount());
        ArgumentCaptor<List<SupplierPpmDetail>> captor = ArgumentCaptor.forClass(List.class);
        verify(detailRepository).saveAll(captor.capture());
        SupplierPpmDetail saved = captor.getValue().get(0);

        // 无缺陷，PPM应为0
        assertEquals(0, saved.getMonthDefectCount().compareTo(BigDecimal.ZERO));
        assertEquals(0, saved.getSupplierTotalPpm().compareTo(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("无供货量数据时记录数为0")
    void calculate_noSupplyVolume_returnsZeroCount() {
        // Given
        String ppmMonth = "202510";
        when(baseInfoRepository.count()).thenReturn(4L);
        when(suspiciousMaterialRepository.findAll(any(Specification.class)))
                .thenReturn(List.of());
        when(supplyVolumeRepository.findAll(any(Specification.class)))
                .thenReturn(List.of());
        when(supplyVolumeRepository.count()).thenReturn(0L);

        // When
        PpmCalculateResult result = ppmCalculateService.calculate(ppmMonth);

        // Then
        assertEquals(0, result.getSavedCount());
        verify(detailRepository, never()).saveAll(any());
    }

    // ==================== 供应商责任判断测试 ====================

    @Test
    @DisplayName("仅统计供应商责任为Y的可疑物料")
    void calculate_onlySupplierResponsible_counted() {
        // Given
        String ppmMonth = "202510";
        when(baseInfoRepository.count()).thenReturn(4L);

        // 供应商责任为Y
        SuspiciousMaterial responsible = createSuspiciousMaterial(
                "SUP001", "供应商A", "河西", LocalDate.of(2025, 10, 1), 10, "Y"
        );
        // 供应商责任为N
        SuspiciousMaterial notResponsible = createSuspiciousMaterial(
                "SUP002", "供应商B", "河西", LocalDate.of(2025, 10, 1), 20, "N"
        );
        // 供应商责任为null
        SuspiciousMaterial nullResp = createSuspiciousMaterial(
                "SUP003", "供应商C", "河西", LocalDate.of(2025, 10, 1), 30, null
        );

        when(suspiciousMaterialRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(responsible, notResponsible, nullResp));

        SupplyVolume supply = createSupplyVolume(
                "SUP001", "供应商A", "河西", (short) 2025, 10000, 10
        );
        when(supplyVolumeRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(supply));

        when(detailRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        ppmCalculateService.calculate(ppmMonth);

        // Then
        ArgumentCaptor<List<SupplierPpmDetail>> captor = ArgumentCaptor.forClass(List.class);
        verify(detailRepository).saveAll(captor.capture());

        // 只有供应商A被统计
        assertEquals(1, captor.getValue().size());
        assertEquals("SUP001", captor.getValue().get(0).getSupplierCode());
    }

    // ==================== 零件排除测试 ====================

    @Test
    @DisplayName("螺栓/螺母/卡扣零件不计入排除后供货量")
    void calculate_excludedParts_notCountedInExcludedQty() {
        // Given
        String ppmMonth = "202510";
        when(baseInfoRepository.count()).thenReturn(4L);

        when(suspiciousMaterialRepository.findAll(any(Specification.class)))
                .thenReturn(List.of());

        // 普通零件供货量
        SupplyVolume normalSupply = createSupplyVolume(
                "SUP001", "供应商A", "河西", (short) 2025, 5000, 10
        );
        normalSupply.setPartName("发动机支架");

        // 螺栓供货量
        SupplyVolume boltSupply = createSupplyVolume(
                "SUP001", "供应商A", "河西", (short) 2025, 3000, 10
        );
        boltSupply.setPartName("高强度螺栓");

        when(supplyVolumeRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(normalSupply, boltSupply));

        when(detailRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        ppmCalculateService.calculate(ppmMonth);

        // Then
        ArgumentCaptor<List<SupplierPpmDetail>> captor = ArgumentCaptor.forClass(List.class);
        verify(detailRepository).saveAll(captor.capture());
        SupplierPpmDetail saved = captor.getValue().get(0);

        // 全量供货量 = 5000 + 3000 = 8000
        assertEquals(0, saved.getMonthSupplyQty().compareTo(BigDecimal.valueOf(8000)));
        // 排除后供货量 = 5000（螺栓被排除）
        assertEquals(0, saved.getMonthSupplyQtyExcluded().compareTo(BigDecimal.valueOf(5000)));
    }

    // ==================== PPM计算精度测试 ====================

    @Test
    @DisplayName("PPM计算结果保留2位小数")
    void calculate_ppmRounding_twoDecimalPlaces() {
        // Given
        String ppmMonth = "202510";
        when(baseInfoRepository.count()).thenReturn(4L);

        SuspiciousMaterial susp = createSuspiciousMaterial(
                "SUP001", "供应商A", "河西", LocalDate.of(2025, 10, 1), 3, "Y"
        );
        when(suspiciousMaterialRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(susp));

        // 供货量10000，缺陷3，PPM = 3/10000 * 1000000 = 300
        SupplyVolume supply = createSupplyVolume(
                "SUP001", "供应商A", "河西", (short) 2025, 10000, 10
        );
        when(supplyVolumeRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(supply));

        when(detailRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        ppmCalculateService.calculate(ppmMonth);

        // Then
        ArgumentCaptor<List<SupplierPpmDetail>> captor = ArgumentCaptor.forClass(List.class);
        verify(detailRepository).saveAll(captor.capture());
        SupplierPpmDetail saved = captor.getValue().get(0);

        assertEquals(0, saved.getSupplierTotalPpm().compareTo(BigDecimal.valueOf(300)));
        assertEquals(2, saved.getSupplierTotalPpm().scale());
    }

    @Test
    @DisplayName("PPM计算 - 除不尽时四舍五入")
    void calculate_ppmRounding_roundsUp() {
        // Given
        String ppmMonth = "202510";
        when(baseInfoRepository.count()).thenReturn(4L);

        SuspiciousMaterial susp = createSuspiciousMaterial(
                "SUP001", "供应商A", "河西", LocalDate.of(2025, 10, 1), 1, "Y"
        );
        when(suspiciousMaterialRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(susp));

        // 供货量3，缺陷1，PPM = 1/3 * 1000000 = 333333.333... ≈ 333333.33
        SupplyVolume supply = createSupplyVolume(
                "SUP001", "供应商A", "河西", (short) 2025, 3, 10
        );
        when(supplyVolumeRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(supply));

        when(detailRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        ppmCalculateService.calculate(ppmMonth);

        // Then
        ArgumentCaptor<List<SupplierPpmDetail>> captor = ArgumentCaptor.forClass(List.class);
        verify(detailRepository).saveAll(captor.capture());
        SupplierPpmDetail saved = captor.getValue().get(0);

        assertEquals(0, saved.getSupplierTotalPpm().compareTo(new BigDecimal("333333.33")));
    }

    // ==================== 辅助方法 ====================

    private SuspiciousMaterial createSuspiciousMaterial(
            String supplierCode, String supplierName, String plant,
            LocalDate orderDate, int defectCount, String supplierResp) {
        SuspiciousMaterial m = new SuspiciousMaterial();
        m.setSupplierCode(supplierCode);
        m.setSupplierName(supplierName);
        m.setPlant(plant);
        m.setOrderDate(orderDate);
        m.setDefectCount(defectCount);
        m.setSupplierResp(supplierResp);
        m.setQuantity(defectCount);
        return m;
    }

    private SupplyVolume createSupplyVolume(
            String supplierCode, String supplierName, String baseCode,
            Short fiscalYear, int monthQty, int targetMonth) {
        SupplyVolume v = new SupplyVolume();
        v.setSupplierCode(supplierCode);
        v.setSupplierName(supplierName);
        v.setBaseCode(baseCode);
        v.setFiscalYear(fiscalYear);
        v.setPartName("普通零件");

        // 设置目标月份的供货量
        switch (targetMonth) {
            case 1 -> v.setMonth1No(monthQty);
            case 2 -> v.setMonth2No(monthQty);
            case 3 -> v.setMonth3No(monthQty);
            case 4 -> v.setMonth4No(monthQty);
            case 5 -> v.setMonth5No(monthQty);
            case 6 -> v.setMonth6No(monthQty);
            case 7 -> v.setMonth7No(monthQty);
            case 8 -> v.setMonth8No(monthQty);
            case 9 -> v.setMonth9No(monthQty);
            case 10 -> v.setMonth10No(monthQty);
            case 11 -> v.setMonth11No(monthQty);
            case 12 -> v.setMonth12No(monthQty);
        }
        return v;
    }
}
