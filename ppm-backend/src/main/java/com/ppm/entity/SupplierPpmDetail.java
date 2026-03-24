package com.ppm.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "supplier_ppm_detail",
        uniqueConstraints = @UniqueConstraint(columnNames = {"ppm_month", "supplier_code"}),
        indexes = {
                @Index(name = "idx_ppm_month", columnList = "ppm_month"),
                @Index(name = "idx_supplier_code", columnList = "supplier_code")
        })
public class SupplierPpmDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ppm_month", nullable = false, length = 8)
    private String ppmMonth;

    @Column(name = "supplier_code", nullable = false, length = 32)
    private String supplierCode;

    @Column(name = "supplier_name", length = 128)
    private String supplierName;

    // 供应商整体数据
    @Column(name = "month_defect_count", precision = 18, scale = 2)
    private BigDecimal monthDefectCount;

    @Column(name = "month_supply_qty", precision = 18, scale = 2)
    private BigDecimal monthSupplyQty;

    @Column(name = "supplier_total_ppm", precision = 18, scale = 2)
    private BigDecimal supplierTotalPpm;

    // 河西基地数据
    @Column(name = "hexi_suspicious_count", precision = 18, scale = 2)
    private BigDecimal hexiSuspiciousCount;

    @Column(name = "hexi_supply_qty", precision = 18, scale = 2)
    private BigDecimal hexiSupplyQty;

    @Column(name = "hexi_supplier_ppm", precision = 18, scale = 2)
    private BigDecimal hexiSupplierPpm;

    // 宝骏基地数据
    @Column(name = "baojun_suspicious_count", precision = 18, scale = 2)
    private BigDecimal baojunSuspiciousCount;

    @Column(name = "baojun_supply_qty", precision = 18, scale = 2)
    private BigDecimal baojunSupplyQty;

    @Column(name = "baojun_supplier_ppm", precision = 18, scale = 2)
    private BigDecimal baojunSupplierPpm;

    // 青岛基地数据
    @Column(name = "qingdao_suspicious_count", precision = 18, scale = 2)
    private BigDecimal qingdaoSuspiciousCount;

    @Column(name = "qingdao_supply_qty", precision = 18, scale = 2)
    private BigDecimal qingdaoSupplyQty;

    @Column(name = "qingdao_supplier_ppm", precision = 18, scale = 2)
    private BigDecimal qingdaoSupplierPpm;

    // 重庆基地数据
    @Column(name = "chongqing_suspicious_count", precision = 18, scale = 2)
    private BigDecimal chongqingSuspiciousCount;

    @Column(name = "chongqing_supply_qty", precision = 18, scale = 2)
    private BigDecimal chongqingSupplyQty;

    @Column(name = "chongqing_supplier_ppm", precision = 18, scale = 2)
    private BigDecimal chongqingSupplierPpm;

    // === 排除螺栓/螺母/卡扣后的供货量（用于基地/公司总体PPM计算）===
    @Column(name = "month_supply_qty_excluded", precision = 18, scale = 2)
    private BigDecimal monthSupplyQtyExcluded;

    @Column(name = "hexi_supply_qty_excluded", precision = 18, scale = 2)
    private BigDecimal hexiSupplyQtyExcluded;

    @Column(name = "baojun_supply_qty_excluded", precision = 18, scale = 2)
    private BigDecimal baojunSupplyQtyExcluded;

    @Column(name = "qingdao_supply_qty_excluded", precision = 18, scale = 2)
    private BigDecimal qingdaoSupplyQtyExcluded;

    @Column(name = "chongqing_supply_qty_excluded", precision = 18, scale = 2)
    private BigDecimal chongqingSupplyQtyExcluded;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
