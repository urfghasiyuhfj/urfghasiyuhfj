package com.ppm.util;

import com.ppm.entity.SupplierPpmDetail;

import java.math.BigDecimal;

/**
 * 供应商PPM明细工具类
 * 提供从 SupplierPpmDetail 获取各基地数据的统一方法
 */
public final class SupplierPpmDetailUtils {

    private SupplierPpmDetailUtils() {
        // 工具类不允许实例化
    }

    /**
     * 获取指定基地的可疑物料数量
     */
    public static int getBaseDefectCount(SupplierPpmDetail d, String baseCode) {
        if (d == null || baseCode == null) return 0;
        BigDecimal count = switch (baseCode) {
            case "河西" -> d.getHexiSuspiciousCount();
            case "宝骏" -> d.getBaojunSuspiciousCount();
            case "青岛" -> d.getQingdaoSuspiciousCount();
            case "重庆" -> d.getChongqingSuspiciousCount();
            default -> BigDecimal.ZERO;
        };
        return count != null ? count.intValue() : 0;
    }

    /**
     * 获取指定基地的供货量
     */
    public static int getBaseSupplyQty(SupplierPpmDetail d, String baseCode) {
        if (d == null || baseCode == null) return 0;
        BigDecimal qty = switch (baseCode) {
            case "河西" -> d.getHexiSupplyQty();
            case "宝骏" -> d.getBaojunSupplyQty();
            case "青岛" -> d.getQingdaoSupplyQty();
            case "重庆" -> d.getChongqingSupplyQty();
            default -> BigDecimal.ZERO;
        };
        return qty != null ? qty.intValue() : 0;
    }

    /**
     * 获取指定基地的PPM值
     */
    public static BigDecimal getBasePpm(SupplierPpmDetail d, String baseCode) {
        if (d == null || baseCode == null) return BigDecimal.ZERO;
        return switch (baseCode) {
            case "河西" -> d.getHexiSupplierPpm();
            case "宝骏" -> d.getBaojunSupplierPpm();
            case "青岛" -> d.getQingdaoSupplierPpm();
            case "重庆" -> d.getChongqingSupplierPpm();
            default -> BigDecimal.ZERO;
        };
    }

    /**
     * 获取指定基地的排除后供货量（排除螺栓/螺母/卡扣，用于基地/公司总体PPM计算）
     */
    public static int getBaseSupplyQtyExcluded(SupplierPpmDetail d, String baseCode) {
        if (d == null || baseCode == null) return 0;
        BigDecimal qty = switch (baseCode) {
            case "河西" -> d.getHexiSupplyQtyExcluded();
            case "宝骏" -> d.getBaojunSupplyQtyExcluded();
            case "青岛" -> d.getQingdaoSupplyQtyExcluded();
            case "重庆" -> d.getChongqingSupplyQtyExcluded();
            default -> BigDecimal.ZERO;
        };
        return qty != null ? qty.intValue() : 0;
    }
}
