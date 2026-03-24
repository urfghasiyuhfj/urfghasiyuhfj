package com.ppm.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "suspicious_material", indexes = {
        @Index(name = "idx_susp_plant", columnList = "plant"),
        @Index(name = "idx_susp_supplier", columnList = "supplier_code"),
        @Index(name = "idx_susp_record_date", columnList = "record_date"),
        @Index(name = "idx_susp_order_date", columnList = "order_date")
})
public class SuspiciousMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plant", nullable = false, length = 64)
    private String plant;

    @Column(name = "record_date")
    private LocalDate recordDate;

    @Column(name = "recorder", length = 32)
    private String recorder;

    @Column(name = "part_code", nullable = false, length = 64)
    private String partCode;

    @Column(name = "part_name", length = 128)
    private String partName;

    @Column(name = "function_module", length = 64)
    private String functionModule;

    @Column(name = "supplier_code", nullable = false, length = 32)
    private String supplierCode;

    @Column(name = "supplier_name", length = 128)
    private String supplierName;

    @Column(name = "model_machine", length = 64)
    private String modelMachine;

    @Column(name = "failure_desc", length = 256)
    private String failureDesc;

    @Column(name = "fault_type", length = 64)
    private String faultType;

    @Column(name = "quantity", nullable = false)
    private Integer quantity = 0;

    @Column(name = "order_date")
    private LocalDate orderDate;

    @Column(name = "shift_section", length = 64)
    private String shiftSection;

    @Column(name = "prod_area", length = 64)
    private String prodArea;

    @Column(name = "supplier_resp", columnDefinition = "CHAR(1)")
    private String supplierResp;

    @Column(name = "remark", length = 256)
    private String remark;

    @Column(name = "supplier_part", length = 128)
    private String supplierPart;

    @Column(name = "defect_count")
    private Integer defectCount;

    @Column(name = "supply_qty")
    private Integer supplyQty;

    @Column(name = "brand", length = 64)
    private String brand;

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
