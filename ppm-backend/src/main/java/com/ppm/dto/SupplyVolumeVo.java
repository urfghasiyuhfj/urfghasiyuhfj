package com.ppm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 供货量查询结果VO，用于前端综合查询展示
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SupplyVolumeVo {

    /** 财年 */
    private Integer fiscalYear;

    /** 基地编码 */
    private String baseCode;

    /** 工厂编码 */
    private String plantId;

    /** 供应商代码 */
    private String supplierCode;

    /** 供应商名称 */
    private String supplierName;

    /** 零件代码 */
    private String partCode;

    /** 零件名称 */
    private String partName;

    /** 供货量（展示所有月份的总和或指定月份的供货量） */
    private Integer supplyQty;

    /** 1月供货量 */
    private Integer month1No;

    /** 2月供货量 */
    private Integer month2No;

    /** 3月供货量 */
    private Integer month3No;

    /** 4月供货量 */
    private Integer month4No;

    /** 5月供货量 */
    private Integer month5No;

    /** 6月供货量 */
    private Integer month6No;

    /** 7月供货量 */
    private Integer month7No;

    /** 8月供货量 */
    private Integer month8No;

    /** 9月供货量 */
    private Integer month9No;

    /** 10月供货量 */
    private Integer month10No;

    /** 11月供货量 */
    private Integer month11No;

    /** 12月供货量 */
    private Integer month12No;

    /**
     * 从SupplyVolume实体转换为VO
     */
    public static SupplyVolumeVo fromEntity(com.ppm.entity.SupplyVolume entity) {
        if (entity == null) return null;
        
        SupplyVolumeVo vo = new SupplyVolumeVo();
        vo.setFiscalYear(entity.getFiscalYear() != null ? entity.getFiscalYear().intValue() : null);
        vo.setBaseCode(entity.getBaseCode());
        vo.setPlantId(entity.getPlantId());
        vo.setSupplierCode(entity.getSupplierCode());
        vo.setSupplierName(entity.getSupplierName());
        vo.setPartCode(entity.getPartCode());
        vo.setPartName(entity.getPartName());
        vo.setMonth1No(entity.getMonth1No());
        vo.setMonth2No(entity.getMonth2No());
        vo.setMonth3No(entity.getMonth3No());
        vo.setMonth4No(entity.getMonth4No());
        vo.setMonth5No(entity.getMonth5No());
        vo.setMonth6No(entity.getMonth6No());
        vo.setMonth7No(entity.getMonth7No());
        vo.setMonth8No(entity.getMonth8No());
        vo.setMonth9No(entity.getMonth9No());
        vo.setMonth10No(entity.getMonth10No());
        vo.setMonth11No(entity.getMonth11No());
        vo.setMonth12No(entity.getMonth12No());
        
        // 计算总供货量
        Integer total = 0;
        if (entity.getMonth1No() != null) total += entity.getMonth1No();
        if (entity.getMonth2No() != null) total += entity.getMonth2No();
        if (entity.getMonth3No() != null) total += entity.getMonth3No();
        if (entity.getMonth4No() != null) total += entity.getMonth4No();
        if (entity.getMonth5No() != null) total += entity.getMonth5No();
        if (entity.getMonth6No() != null) total += entity.getMonth6No();
        if (entity.getMonth7No() != null) total += entity.getMonth7No();
        if (entity.getMonth8No() != null) total += entity.getMonth8No();
        if (entity.getMonth9No() != null) total += entity.getMonth9No();
        if (entity.getMonth10No() != null) total += entity.getMonth10No();
        if (entity.getMonth11No() != null) total += entity.getMonth11No();
        if (entity.getMonth12No() != null) total += entity.getMonth12No();
        vo.setSupplyQty(total);
        
        return vo;
    }
}
