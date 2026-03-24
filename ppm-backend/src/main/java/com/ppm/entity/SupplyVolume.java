package com.ppm.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "supply_volume", indexes = {
        @Index(name = "idx_supply_plant", columnList = "plant_id"),
        @Index(name = "idx_supply_supplier", columnList = "supplier_code"),
        @Index(name = "idx_supply_year_plant", columnList = "fiscal_year, plant_id"),
        @Index(name = "idx_supply_base_code", columnList = "base_code"),
        @Index(name = "idx_supply_part_code", columnList = "part_code"),
        @Index(name = "idx_supply_supplier_part", columnList = "supplier_code, part_code")
})
public class SupplyVolume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "create_date",nullable = false)
    private LocalDateTime createDate;

    @Column(name = "data_source",nullable = false)
    private String dataSource;

    @Column(name = "date_ver",nullable = false)
    private LocalDate dataVer;

    @Column(name = "etl_create_date",nullable = false)
    private LocalDateTime etlCreateDate;

    @Column(name = "fiscal_year", nullable = false)
    private Short fiscalYear;

    @Column(name = "month_1_no", nullable = false)
    private Integer month1No;

    @Column(name = "month_2_no", nullable = false)
    private Integer month2No;

    @Column(name = "month_3_no", nullable = false)
    private Integer month3No;

    @Column(name = "month_4_no", nullable = false)
    private Integer month4No;

    @Column(name = "month_5_no", nullable = false)
    private Integer month5No;

    @Column(name = "month_6_no", nullable = false)
    private Integer month6No;

    @Column(name = "month_7_no", nullable = false)
    private Integer month7No;

    @Column(name = "month_8_no", nullable = false)
    private Integer month8No;

    @Column(name = "month_9_no", nullable = false)
    private Integer month9No;

    @Column(name = "month_10_no", nullable = false)
    private Integer month10No;

    @Column(name = "month_11_no", nullable = false)
    private Integer month11No;

    @Column(name = "month_12_no", nullable = false)
    private Integer month12No;

    @Column(name = "part_name", length = 128)
    private String partName;

    @Column(name = "part_code", nullable = false, length = 64)
    private String partCode;

    @Column(name = "plant_id", nullable = false, length = 32)
    private String plantId;

    @Column(name = "supplier_code", nullable = false, length = 32)
    private String supplierCode;

    @Column(name = "supplier_name", length = 128)
    private String supplierName;

    @Column(name = "base_code", length = 32)
    private String baseCode;

    @Column(name = "total_no")
    private Integer totalNo;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "if_docking")
    private String ifDocking;

    /**
     * 根据月份获取对应的供货量
     * @param month 月份（1-12）
     * @return 对应月份的供货量，如果月份无效返回0
     */
    public int getMonthXNo(int month) {
        if (month < 1 || month > 12) {
            return 0;
        }
        return switch (month) {
            case 1 -> month1No != null ? month1No : 0;
            case 2 -> month2No != null ? month2No : 0;
            case 3 -> month3No != null ? month3No : 0;
            case 4 -> month4No != null ? month4No : 0;
            case 5 -> month5No != null ? month5No : 0;
            case 6 -> month6No != null ? month6No : 0;
            case 7 -> month7No != null ? month7No : 0;
            case 8 -> month8No != null ? month8No : 0;
            case 9 -> month9No != null ? month9No : 0;
            case 10 -> month10No != null ? month10No : 0;
            case 11 -> month11No != null ? month11No : 0;
            case 12 -> month12No != null ? month12No : 0;
            default -> 0;
        };
    }

    @PrePersist
    void prePersist() {
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
