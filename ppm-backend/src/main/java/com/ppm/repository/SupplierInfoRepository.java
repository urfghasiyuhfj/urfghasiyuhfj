package com.ppm.repository;

import com.ppm.entity.SupplierInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SupplierInfoRepository extends JpaRepository<SupplierInfo, Long> {

    Optional<SupplierInfo> findBySupplierCode(String supplierCode);
}
