package com.ppm.repository;

import com.ppm.entity.SuspiciousMaterial;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;

public interface SuspiciousMaterialRepository extends JpaRepository<SuspiciousMaterial, Long>,
        JpaSpecificationExecutor<SuspiciousMaterial> {

    Page<SuspiciousMaterial> findByPlant(String plant, Pageable pageable);

    Page<SuspiciousMaterial> findBySupplierCode(String supplierCode, Pageable pageable);

    Page<SuspiciousMaterial> findByRecordDateBetween(LocalDate start, LocalDate end, Pageable pageable);
}
