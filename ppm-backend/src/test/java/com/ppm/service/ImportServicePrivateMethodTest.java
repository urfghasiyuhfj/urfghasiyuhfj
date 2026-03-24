package com.ppm.service;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ImportService 私有方法单元测试
 * 使用反射测试工具方法
 */
@ExtendWith(MockitoExtension.class)
class ImportServicePrivateMethodTest {

    private ImportService importService;

    @BeforeEach
    void setUp() {
        importService = new ImportService(
                null, null, null, null, null
        );
    }

    // ==================== normalizeNumericLike 测试 ====================

    @Test
    @DisplayName("normalizeNumericLike - 去除.0后缀")
    void normalizeNumericLike_removesDotZeroSuffix() throws Exception {
        Method method = ImportService.class.getDeclaredMethod("normalizeNumericLike", String.class);
        method.setAccessible(true);

        assertEquals("123", method.invoke(null, "123.0"));
        assertEquals("456", method.invoke(null, "456.0"));
    }

    @Test
    @DisplayName("normalizeNumericLike - 去除前后空格")
    void normalizeNumericLike_trimsWhitespace() throws Exception {
        Method method = ImportService.class.getDeclaredMethod("normalizeNumericLike", String.class);
        method.setAccessible(true);

        assertEquals("abc", method.invoke(null, "  abc  "));
        assertEquals("123", method.invoke(null, "  123  "));
    }

    @Test
    @DisplayName("normalizeNumericLike - 空字符串返回null")
    void normalizeNumericLike_emptyString_returnsNull() throws Exception {
        Method method = ImportService.class.getDeclaredMethod("normalizeNumericLike", String.class);
        method.setAccessible(true);

        assertNull(method.invoke(null, ""));
        assertNull(method.invoke(null, "   "));
        assertNull(method.invoke(null, (String) null));
    }

    @Test
    @DisplayName("normalizeNumericLike - 保留非.0后缀的小数")
    void normalizeNumericLike_keepsOtherDecimals() throws Exception {
        Method method = ImportService.class.getDeclaredMethod("normalizeNumericLike", String.class);
        method.setAccessible(true);

        assertEquals("123.5", method.invoke(null, "123.5"));
        assertEquals("456.99", method.invoke(null, "456.99"));
    }

    // ==================== trimToNull 测试 ====================

    @Test
    @DisplayName("trimToNull - 空字符串返回null")
    void trimToNull_emptyString_returnsNull() throws Exception {
        Method method = ImportService.class.getDeclaredMethod("trimToNull", String.class);
        method.setAccessible(true);

        assertNull(method.invoke(null, ""));
        assertNull(method.invoke(null, "   "));
        assertNull(method.invoke(null, (String) null));
    }

    @Test
    @DisplayName("trimToNull - 非空字符串去除空格")
    void trimToNull_nonEmptyString_trims() throws Exception {
        Method method = ImportService.class.getDeclaredMethod("trimToNull", String.class);
        method.setAccessible(true);

        assertEquals("abc", method.invoke(null, "  abc  "));
        assertEquals("123", method.invoke(null, "123"));
    }

    // ==================== trunc 测试 ====================

    @Test
    @DisplayName("trunc - 超长字符串被截断")
    void trunc_longString_truncated() throws Exception {
        Method method = ImportService.class.getDeclaredMethod("trunc", String.class, int.class);
        method.setAccessible(true);

        String longStr = "a".repeat(100);
        String result = (String) method.invoke(null, longStr, 32);
        assertEquals(32, result.length());
    }

    @Test
    @DisplayName("trunc - 短字符串保持不变")
    void trunc_shortString_unchanged() throws Exception {
        Method method = ImportService.class.getDeclaredMethod("trunc", String.class, int.class);
        method.setAccessible(true);

        String shortStr = "abc";
        String result = (String) method.invoke(null, shortStr, 32);
        assertEquals("abc", result);
    }

    @Test
    @DisplayName("trunc - null返回null")
    void trunc_null_returnsNull() throws Exception {
        Method method = ImportService.class.getDeclaredMethod("trunc", String.class, int.class);
        method.setAccessible(true);

        assertNull(method.invoke(null, (String) null, 32));
    }

    // ==================== firstChar 测试 ====================

    @Test
    @DisplayName("firstChar - 返回首字符")
    void firstChar_returnsFirstCharacter() throws Exception {
        Method method = ImportService.class.getDeclaredMethod("firstChar", String.class);
        method.setAccessible(true);

        assertEquals("Y", method.invoke(null, "Yes"));
        assertEquals("N", method.invoke(null, "No"));
        assertEquals("A", method.invoke(null, "ABC"));
    }

    @Test
    @DisplayName("firstChar - null返回null")
    void firstChar_null_returnsNull() throws Exception {
        Method method = ImportService.class.getDeclaredMethod("firstChar", String.class);
        method.setAccessible(true);

        assertNull(method.invoke(null, (String) null));
    }

    // ==================== inferBaseCodeFromPlantId 测试 ====================

    @Test
    @DisplayName("inferBaseCodeFromPlantId - 重庆基地映射")
    void inferBaseCodeFromPlantId_chongqing_correctMapping() throws Exception {
        Method method = ImportService.class.getDeclaredMethod("inferBaseCodeFromPlantId", String.class);
        method.setAccessible(true);

        assertEquals("重庆", method.invoke(null, "8200"));
        assertEquals("重庆", method.invoke(null, "8300"));
        assertEquals("重庆", method.invoke(null, "重庆"));
    }

    @Test
    @DisplayName("inferBaseCodeFromPlantId - 宝骏基地映射")
    void inferBaseCodeFromPlantId_baojun_correctMapping() throws Exception {
        Method method = ImportService.class.getDeclaredMethod("inferBaseCodeFromPlantId", String.class);
        method.setAccessible(true);

        assertEquals("宝骏", method.invoke(null, "8000"));
        assertEquals("宝骏", method.invoke(null, "8100"));
        assertEquals("宝骏", method.invoke(null, "宝骏"));
    }

    @Test
    @DisplayName("inferBaseCodeFromPlantId - 河西基地映射")
    void inferBaseCodeFromPlantId_hexi_correctMapping() throws Exception {
        Method method = ImportService.class.getDeclaredMethod("inferBaseCodeFromPlantId", String.class);
        method.setAccessible(true);

        assertEquals("河西", method.invoke(null, "1000"));
        assertEquals("河西", method.invoke(null, "4000"));
        assertEquals("河西", method.invoke(null, "河西"));
    }

    @Test
    @DisplayName("inferBaseCodeFromPlantId - 青岛基地映射")
    void inferBaseCodeFromPlantId_qingdao_correctMapping() throws Exception {
        Method method = ImportService.class.getDeclaredMethod("inferBaseCodeFromPlantId", String.class);
        method.setAccessible(true);

        assertEquals("青岛", method.invoke(null, "3000"));
        assertEquals("青岛", method.invoke(null, "5000"));
        assertEquals("青岛", method.invoke(null, "青岛"));
    }

    @Test
    @DisplayName("inferBaseCodeFromPlantId - 未知ID返回null")
    void inferBaseCodeFromPlantId_unknown_returnsNull() throws Exception {
        Method method = ImportService.class.getDeclaredMethod("inferBaseCodeFromPlantId", String.class);
        method.setAccessible(true);

        assertNull(method.invoke(null, "9999"));
        assertNull(method.invoke(null, (String) null));
    }

    // ==================== inferBaseCodeFromFilename 测试 ====================

    @Test
    @DisplayName("inferBaseCodeFromFilename - 从文件名推断基地")
    void inferBaseCodeFromFilename_correctInference() throws Exception {
        Method method = ImportService.class.getDeclaredMethod("inferBaseCodeFromFilename", String.class);
        method.setAccessible(true);

        assertEquals("河西", method.invoke(null, "河西供货量2510.xlsx"));
        assertEquals("宝骏", method.invoke(null, "宝骏供货量202510.xlsx"));
        assertEquals("青岛", method.invoke(null, "青岛供货量.xlsx"));
        assertEquals("重庆", method.invoke(null, "重庆供货量数据.xlsx"));
        assertNull(method.invoke(null, "未知文件.xlsx"));
        assertNull(method.invoke(null, (String) null));
    }

    // ==================== inferFiscalYearFromFilename 测试 ====================

    @Test
    @DisplayName("inferFiscalYearFromFilename - 从文件名推断财年")
    void inferFiscalYearFromFilename_correctInference() throws Exception {
        Method method = ImportService.class.getDeclaredMethod("inferFiscalYearFromFilename", String.class);
        method.setAccessible(true);

        assertEquals(2025, method.invoke(null, "供货量202510.xlsx"));
        assertEquals(2025, method.invoke(null, "供货量2510.xlsx"));
        assertEquals(2024, method.invoke(null, "供货量2401.xlsx"));
        assertEquals(2023, method.invoke(null, "供货量2312.xlsx"));
        assertNull(method.invoke(null, "供货量.xlsx"));
        assertNull(method.invoke(null, (String) null));
    }

    // ==================== inferPpmMonthFromFilename 测试 ====================

    @Test
    @DisplayName("inferPpmMonthFromFilename - 从文件名推断PPM月份")
    void inferPpmMonthFromFilename_correctInference() throws Exception {
        Method method = ImportService.class.getDeclaredMethod("inferPpmMonthFromFilename", String.class);
        method.setAccessible(true);

        assertEquals("202510", method.invoke(null, "可疑物料统计2510.xlsx"));
        assertEquals("202510", method.invoke(null, "可疑物料统计202510.xlsx"));
        assertEquals("202401", method.invoke(null, "数据2401.xlsx"));
        assertNull(method.invoke(null, "可疑物料统计.xlsx"));
        assertNull(method.invoke(null, (String) null));
    }

    // ==================== isExcludedPartName 测试 ====================

    @Test
    @DisplayName("isExcludedPartName - 螺栓被排除")
    void isExcludedPartName_bolt_excluded() throws Exception {
        Method method = ImportService.class.getDeclaredMethod("isExcludedPartName", String.class);
        method.setAccessible(true);

        assertTrue((Boolean) method.invoke(null, "高强度螺栓"));
        assertTrue((Boolean) method.invoke(null, "螺栓M8"));
        assertTrue((Boolean) method.invoke(null, "六角螺栓"));
    }

    @Test
    @DisplayName("isExcludedPartName - 螺母被排除")
    void isExcludedPartName_nut_excluded() throws Exception {
        Method method = ImportService.class.getDeclaredMethod("isExcludedPartName", String.class);
        method.setAccessible(true);

        assertTrue((Boolean) method.invoke(null, "六角螺母"));
        assertTrue((Boolean) method.invoke(null, "螺母M10"));
    }

    @Test
    @DisplayName("isExcludedPartName - 卡扣被排除")
    void isExcludedPartName_clip_excluded() throws Exception {
        Method method = ImportService.class.getDeclaredMethod("isExcludedPartName", String.class);
        method.setAccessible(true);

        assertTrue((Boolean) method.invoke(null, "塑料卡扣"));
        assertTrue((Boolean) method.invoke(null, "线束卡扣"));
    }

    @Test
    @DisplayName("isExcludedPartName - 普通零件不排除")
    void isExcludedPartName_normalPart_notExcluded() throws Exception {
        Method method = ImportService.class.getDeclaredMethod("isExcludedPartName", String.class);
        method.setAccessible(true);

        assertFalse((Boolean) method.invoke(null, "发动机支架"));
        assertFalse((Boolean) method.invoke(null, "变速箱壳体"));
        assertFalse((Boolean) method.invoke(null, "减震器"));
        assertFalse((Boolean) method.invoke(null, ""));
        assertFalse((Boolean) method.invoke(null, (String) null));
    }

    // ==================== isSupplierResponsible 测试 ====================

    @Test
    @DisplayName("isSupplierResponsible - Y返回true")
    void isSupplierResponsible_y_returnsTrue() throws Exception {
        Method method = ImportService.class.getDeclaredMethod("isSupplierResponsible", String.class);
        method.setAccessible(true);

        assertTrue((Boolean) method.invoke(null, "Y"));
        assertTrue((Boolean) method.invoke(null, "y"));
        assertTrue((Boolean) method.invoke(null, " Y "));
    }

    @Test
    @DisplayName("isSupplierResponsible - 非Y返回false")
    void isSupplierResponsible_notY_returnsFalse() throws Exception {
        Method method = ImportService.class.getDeclaredMethod("isSupplierResponsible", String.class);
        method.setAccessible(true);

        assertFalse((Boolean) method.invoke(null, "N"));
        assertFalse((Boolean) method.invoke(null, "n"));
        assertFalse((Boolean) method.invoke(null, ""));
        assertFalse((Boolean) method.invoke(null, (String) null));
    }

    // ==================== mapPlantToBase 测试 ====================

    @Test
    @DisplayName("mapPlantToBase - 正确映射工厂到基地")
    void mapPlantToBase_correctMapping() throws Exception {
        Method method = PpmCalculateService.class.getDeclaredMethod("mapPlantToBase", String.class);
        method.setAccessible(true);

        assertEquals("河西", method.invoke(null, "河西工厂"));
        assertEquals("宝骏", method.invoke(null, "宝骏基地"));
        assertEquals("青岛", method.invoke(null, "青岛车间"));
        assertEquals("重庆", method.invoke(null, "重庆分厂"));
        assertNull(method.invoke(null, "未知工厂"));
        assertNull(method.invoke(null, (String) null));
    }
}
