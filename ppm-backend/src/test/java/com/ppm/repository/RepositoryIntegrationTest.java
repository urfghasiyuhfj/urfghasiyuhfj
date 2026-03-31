package com.ppm.repository;

import com.ppm.BaseIntegrationTest;
import com.ppm.entity.BaseInfo;
import com.ppm.entity.SupplierPpmDetail;
import com.ppm.entity.SuspiciousMaterial;
import com.ppm.entity.SupplyVolume;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Repository 集成测试
 * 测试数据库操作的正确性
 */
@Disabled("需要Docker环境运行Testcontainers")
class RepositoryIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private BaseInfoRepository baseInfoRepository;

    @Autowired
    private SuspiciousMaterialRepository suspiciousMaterialRepository;

    @Autowired
    private SupplyVolumeRepository supplyVolumeRepository;

    @Autowired
    private SupplierPpmDetailRepository supplierPpmDetailRepository;

    @BeforeEach
    void setUp() {
        suspiciousMaterialRepository.deleteAll();
        supplyVolumeRepository.deleteAll();
        supplierPpmDetailRepository.deleteAll();
        baseInfoRepository.deleteAll();
    }

    // ==================== BaseInfo Repository 测试 ====================

    @Test
    @DisplayName("BaseInfo - 保存和查询")
    void baseInfo_saveAndFind_success() {
        BaseInfo base = new BaseInfo();
        base.setBaseCode("河西");
        base.setBaseName("河西基地");
        
        BaseInfo saved = baseInfoRepository.save(base);
        
        assertNotNull(saved.getId());
        assertEquals("河西", saved.getBaseCode());
        
        BaseInfo found = baseInfoRepository.findById(saved.getId()).orElse(null);
        assertNotNull(found);
        assertEquals("河西", found.getBaseCode());
    }

    @Test
    @DisplayName("BaseInfo - 唯一约束校验")
    void baseInfo_uniqueConstraint_enforced() {
        BaseInfo base1 = new BaseInfo();
        base1.setBaseCode("河西");
        baseInfoRepository.save(base1);

        BaseInfo base2 = new BaseInfo();
        base2.setBaseCode("河西");

        assertThrows(Exception.class, () -> baseInfoRepository.save(base2));
    }

    // ==================== SuspiciousMaterial Repository 测试 ====================

    @Test
    @DisplayName("SuspiciousMaterial - 保存和查询")
    void suspiciousMaterial_saveAndFind_success() {
        SuspiciousMaterial susp = createSuspiciousMaterial("SUP001", "河西", LocalDate.of(2025, 10, 15));
        
        SuspiciousMaterial saved = suspiciousMaterialRepository.save(susp);
        
        assertNotNull(saved.getId());
        assertEquals("SUP001", saved.getSupplierCode());
    }

    @Test
    @DisplayName("SuspiciousMaterial - 按日期范围查询")
    void suspiciousMaterial_findByDateRange_success() {
        suspiciousMaterialRepository.save(createSuspiciousMaterial("SUP001", "河西", LocalDate.of(2025, 10, 5)));
        suspiciousMaterialRepository.save(createSuspiciousMaterial("SUP002", "河西", LocalDate.of(2025, 10, 15)));
        suspiciousMaterialRepository.save(createSuspiciousMaterial("SUP003", "河西", LocalDate.of(2025, 11, 5)));

        var results = suspiciousMaterialRepository.findAll(
                (root, q, cb) -> cb.and(
                        cb.greaterThanOrEqualTo(root.get("orderDate"), LocalDate.of(2025, 10, 1)),
                        cb.lessThanOrEqualTo(root.get("orderDate"), LocalDate.of(2025, 10, 31))
                )
        );

        assertEquals(2, results.size());
    }

    // ==================== SupplyVolume Repository 测试 ====================

    @Test
    @DisplayName("SupplyVolume - 保存和查询")
    void supplyVolume_saveAndFind_success() {
        SupplyVolume supply = createSupplyVolume("SUP001", "河西", (short) 2025);
        
        SupplyVolume saved = supplyVolumeRepository.save(supply);
        
        assertNotNull(saved.getId());
        assertEquals("SUP001", saved.getSupplierCode());
        assertEquals(10000, saved.getMonth10No());
    }

    @Test
    @DisplayName("SupplyVolume - 按财年和供应商查询")
    void supplyVolume_findByFiscalYearAndSupplier_success() {
        supplyVolumeRepository.save(createSupplyVolume("SUP001", "河西", (short) 2025));
        supplyVolumeRepository.save(createSupplyVolume("SUP002", "河西", (short) 2025));
        supplyVolumeRepository.save(createSupplyVolume("SUP001", "河西", (short) 2024));

        var results = supplyVolumeRepository.findAll(
                (root, q, cb) -> cb.and(
                        cb.equal(root.get("fiscalYear"), (short) 2025),
                        cb.equal(root.get("supplierCode"), "SUP001")
                )
        );

        assertEquals(1, results.size());
    }

    // ==================== SupplierPpmDetail Repository 测试 ====================

    @Test
    @DisplayName("SupplierPpmDetail - 保存和查询")
    void supplierPpmDetail_saveAndFind_success() {
        SupplierPpmDetail detail = createSupplierPpmDetail("202510", "SUP001");
        
        SupplierPpmDetail saved = supplierPpmDetailRepository.save(detail);
        
        assertNotNull(saved.getId());
        assertEquals("202510", saved.getPpmMonth());
        assertEquals("SUP001", saved.getSupplierCode());
    }

    @Test
    @DisplayName("SupplierPpmDetail - 按月份查询并按PPM降序")
    void supplierPpmDetail_findByMonthOrderByPpmDesc_success() {
        supplierPpmDetailRepository.save(createSupplierPpmDetail("202510", "SUP001", BigDecimal.valueOf(1000)));
        supplierPpmDetailRepository.save(createSupplierPpmDetail("202510", "SUP002", BigDecimal.valueOf(2000)));
        supplierPpmDetailRepository.save(createSupplierPpmDetail("202510", "SUP003", BigDecimal.valueOf(500)));

        List<SupplierPpmDetail> results = supplierPpmDetailRepository
                .findByPpmMonthOrderBySupplierTotalPpmDesc("202510");

        assertEquals(3, results.size());
        assertEquals("SUP002", results.get(0).getSupplierCode()); // PPM 2000
        assertEquals("SUP001", results.get(1).getSupplierCode()); // PPM 1000
        assertEquals("SUP003", results.get(2).getSupplierCode()); // PPM 500
    }

    @Test
    @DisplayName("SupplierPpmDetail - 删除指定月份数据")
    void supplierPpmDetail_deleteByMonth_success() {
        supplierPpmDetailRepository.save(createSupplierPpmDetail("202510", "SUP001"));
        supplierPpmDetailRepository.save(createSupplierPpmDetail("202511", "SUP001"));

        supplierPpmDetailRepository.deleteByPpmMonth("202510");

        var octDetails = supplierPpmDetailRepository.findByPpmMonth("202510");
        var novDetails = supplierPpmDetailRepository.findByPpmMonth("202511");

        assertTrue(octDetails.isEmpty());
        assertFalse(novDetails.isEmpty());
    }

    // ==================== 辅助方法 ====================

    private SuspiciousMaterial createSuspiciousMaterial(String supplierCode, String plant, LocalDate orderDate) {
        SuspiciousMaterial susp = new SuspiciousMaterial();
        susp.setPlant(plant);
        susp.setPartCode("PART001");
        susp.setSupplierCode(supplierCode);
        susp.setSupplierName("供应商" + supplierCode);
        susp.setOrderDate(orderDate);
        susp.setQuantity(10);
        susp.setDefectCount(10);
        susp.setSupplierResp("Y");
        return susp;
    }

    private SupplyVolume createSupplyVolume(String supplierCode, String baseCode, Short fiscalYear) {
        SupplyVolume supply = new SupplyVolume();
        supply.setFiscalYear(fiscalYear);
        supply.setBaseCode(baseCode);
        supply.setPlantId("1000");
        supply.setSupplierCode(supplierCode);
        supply.setSupplierName("供应商" + supplierCode);
        supply.setPartCode("PART001");
        supply.setPartName("测试零件");
        supply.setMonth10No(10000);
        supply.setCreateDate(java.time.LocalDateTime.now());
        supply.setDataSource("TEST");
        supply.setDataVer(LocalDate.now());
        supply.setEtlCreateDate(java.time.LocalDateTime.now());
        return supply;
    }

    private SupplierPpmDetail createSupplierPpmDetail(String ppmMonth, String supplierCode) {
        return createSupplierPpmDetail(ppmMonth, supplierCode, BigDecimal.valueOf(1000));
    }

    private SupplierPpmDetail createSupplierPpmDetail(String ppmMonth, String supplierCode, BigDecimal ppm) {
        SupplierPpmDetail detail = new SupplierPpmDetail();
        detail.setPpmMonth(ppmMonth);
        detail.setSupplierCode(supplierCode);
        detail.setSupplierName("供应商" + supplierCode);
        detail.setMonthDefectCount(BigDecimal.valueOf(10));
        detail.setMonthSupplyQty(BigDecimal.valueOf(10000));
        detail.setSupplierTotalPpm(ppm);
        return detail;
    }
}
