package com.ppm.controller;

import com.ppm.BaseIntegrationTest;
import com.ppm.dto.PpmCalculateResult;
import com.ppm.entity.BaseInfo;
import com.ppm.entity.SupplierPpmDetail;
import com.ppm.entity.SuspiciousMaterial;
import com.ppm.entity.SupplyVolume;
import com.ppm.repository.BaseInfoRepository;
import com.ppm.repository.SupplierPpmDetailRepository;
import com.ppm.repository.SuspiciousMaterialRepository;
import com.ppm.repository.SupplyVolumeRepository;
import com.ppm.service.PpmCalculateService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * PPM Controller 集成测试
 * 测试完整的 API 端到端流程
 */
@AutoConfigureMockMvc
class PpmControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BaseInfoRepository baseInfoRepository;

    @Autowired
    private SuspiciousMaterialRepository suspiciousMaterialRepository;

    @Autowired
    private SupplyVolumeRepository supplyVolumeRepository;

    @Autowired
    private SupplierPpmDetailRepository supplierPpmDetailRepository;

    @Autowired
    private PpmCalculateService ppmCalculateService;

    @BeforeEach
    void setUp() {
        // 清理数据
        supplierPpmDetailRepository.deleteAll();
        suspiciousMaterialRepository.deleteAll();
        supplyVolumeRepository.deleteAll();
        baseInfoRepository.deleteAll();

        // 初始化基地数据
        BaseInfo hexi = new BaseInfo();
        hexi.setBaseCode("河西");
        hexi.setBaseName("河西");
        baseInfoRepository.save(hexi);

        BaseInfo baojun = new BaseInfo();
        baojun.setBaseCode("宝骏");
        baojun.setBaseName("宝骏");
        baseInfoRepository.save(baojun);
    }

    // ==================== API 测试 ====================

    @Test
    @DisplayName("GET /ppm/available-months - 返回空列表当无数据时")
    void availableMonths_noData_returnsEmptyList() throws Exception {
        mockMvc.perform(get("/ppm/available-months"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("POST /ppm/calculate - 计算PPM成功")
    void calculate_withValidMonth_returnsResult() throws Exception {
        // Given - 准备测试数据
        prepareTestData("202510");

        // When & Then
        mockMvc.perform(post("/ppm/calculate")
                        .param("ppmMonth", "202510"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ppmMonth").value("202510"))
                .andExpect(jsonPath("$.data.savedCount").value(greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("GET /ppm/summary - 分页查询PPM汇总")
    void summary_withData_returnsPagedResult() throws Exception {
        // Given
        prepareTestData("202510");
        ppmCalculateService.calculate("202510");

        // When & Then
        mockMvc.perform(get("/ppm/summary")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isArray());
    }

    @Test
    @DisplayName("GET /ppm/trend - 获取PPM趋势数据")
    void trend_returnsTrendData() throws Exception {
        // Given
        prepareTestData("202510");
        ppmCalculateService.calculate("202510");

        // When & Then
        mockMvc.perform(get("/ppm/trend")
                        .param("limitMonths", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.months").isArray());
    }

    @Test
    @DisplayName("GET /ppm/global-monthly - 获取全局月度PPM")
    void globalMonthly_returnsGlobalData() throws Exception {
        // Given
        prepareTestData("202510");
        ppmCalculateService.calculate("202510");

        // When & Then
        mockMvc.perform(get("/ppm/global-monthly")
                        .param("limitMonths", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isArray());
    }

    // ==================== 服务层集成测试 ====================

    @Test
    @DisplayName("PPM计算 - 完整流程验证")
    void ppmCalculate_fullFlow_correctResult() {
        // Given
        prepareTestData("202510");

        // When
        PpmCalculateResult result = ppmCalculateService.calculate("202510");

        // Then
        assertNotNull(result);
        assertEquals("202510", result.getPpmMonth());
        assertTrue(result.getSavedCount() >= 1);

        // 验证数据库中的记录
        var details = supplierPpmDetailRepository.findByPpmMonth("202510");
        assertFalse(details.isEmpty());

        SupplierPpmDetail detail = details.get(0);
        assertEquals("SUP001", detail.getSupplierCode());
        assertEquals("供应商A", detail.getSupplierName());

        // 验证PPM计算: 10缺陷 / 10000供货量 * 1,000,000 = 1000
        assertEquals(0, detail.getSupplierTotalPpm().compareTo(BigDecimal.valueOf(1000)));
    }

    @Test
    @DisplayName("PPM计算 - 多月份数据隔离")
    void ppmCalculate_multipleMonths_isolated() {
        // Given - 准备两个月份的数据
        prepareTestData("202510");
        ppmCalculateService.calculate("202510");

        prepareTestData("202511");
        ppmCalculateService.calculate("202511");

        // When
        var octDetails = supplierPpmDetailRepository.findByPpmMonth("202510");
        var novDetails = supplierPpmDetailRepository.findByPpmMonth("202511");

        // Then
        assertFalse(octDetails.isEmpty());
        assertFalse(novDetails.isEmpty());
        assertEquals(1, octDetails.size());
        assertEquals(1, novDetails.size());
    }

    // ==================== 辅助方法 ====================

    private void prepareTestData(String ppmMonth) {
        int year = Integer.parseInt(ppmMonth.substring(0, 4));
        int month = Integer.parseInt(ppmMonth.substring(4, 6));

        // 创建可疑物料
        SuspiciousMaterial susp = new SuspiciousMaterial();
        susp.setPlant("河西");
        susp.setPartCode("PART001");
        susp.setSupplierCode("SUP001");
        susp.setSupplierName("供应商A");
        susp.setOrderDate(LocalDate.of(year, month, 15));
        susp.setDefectCount(10);
        susp.setQuantity(10);
        susp.setSupplierResp("Y");
        suspiciousMaterialRepository.save(susp);

        // 创建供货量
        SupplyVolume supply = new SupplyVolume();
        supply.setFiscalYear((short) year);
        supply.setBaseCode("河西");
        supply.setPlantId("1000");
        supply.setSupplierCode("SUP001");
        supply.setSupplierName("供应商A");
        supply.setPartCode("PART001");
        supply.setPartName("测试零件");
        supply.setMonth10No(month == 10 ? 10000 : 0);
        supply.setMonth11No(month == 11 ? 10000 : 0);
        supply.setCreateDate(java.time.LocalDateTime.now());
        supply.setDataSource("TEST");
        supply.setDataVer(LocalDate.now());
        supply.setEtlCreateDate(java.time.LocalDateTime.now());
        supplyVolumeRepository.save(supply);
    }
}
