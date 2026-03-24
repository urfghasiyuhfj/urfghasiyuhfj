package com.ppm.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "supplier_ppm_summary",
        uniqueConstraints = @UniqueConstraint(columnNames = {"ppm_month", "base_code", "supplier_code"}),
        indexes = {
                @Index(name = "idx_ppm_month", columnList = "ppm_month"),
                @Index(name = "idx_ppm_base", columnList = "base_code"),
                @Index(name = "idx_ppm_supplier", columnList = "supplier_code")
        })
public class SupplierPpmSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ppm_month", nullable = false, length = 8)
    private String ppmMonth;

    @Column(name = "base_id")
    private Long baseId;

    @Column(name = "base_code", nullable = false, length = 32)
    private String baseCode;

    @Column(name = "base_name", nullable = false, length = 64)
    private String baseName;

    @Column(name = "supplier_code", nullable = false, length = 32)
    private String supplierCode;

    @Column(name = "supplier_name", nullable = false, length = 128)
    private String supplierName;

    @Column(name = "defect_count", nullable = false)
    private Integer defectCount;

    @Column(name = "supply_qty", nullable = false)
    private Integer supplyQty;

    @Column(name = "ppm", nullable = false, precision = 12, scale = 2)
    private BigDecimal ppm;

    @Column(name = "created_at", updatable = false)
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
