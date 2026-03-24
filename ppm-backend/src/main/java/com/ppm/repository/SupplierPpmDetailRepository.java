package com.ppm.repository;

import com.ppm.entity.SupplierPpmDetail;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SupplierPpmDetailRepository extends JpaRepository<SupplierPpmDetail, Long>,
        JpaSpecificationExecutor<SupplierPpmDetail> {

    Optional<SupplierPpmDetail> findByPpmMonthAndSupplierCode(String ppmMonth, String supplierCode);

    List<SupplierPpmDetail> findByPpmMonth(String ppmMonth);

    List<SupplierPpmDetail> findByPpmMonthOrderBySupplierTotalPpmDesc(String ppmMonth);

    Page<SupplierPpmDetail> findByPpmMonth(String ppmMonth, Pageable pageable);

    void deleteByPpmMonth(String ppmMonth);

    List<SupplierPpmDetail> findBySupplierCode(String supplierCode);

    @Query("SELECT DISTINCT s.ppmMonth FROM SupplierPpmDetail s ORDER BY s.ppmMonth DESC")
    List<String> findDistinctPpmMonths();

    List<SupplierPpmDetail> findByPpmMonthInOrderByPpmMonthAsc(List<String> ppmMonths);

    @Query("SELECT s FROM SupplierPpmDetail s WHERE s.ppmMonth IN :ppmMonths ORDER BY s.ppmMonth ASC, s.supplierTotalPpm DESC")
    List<SupplierPpmDetail> findByPpmMonthInOrderByPpmMonthAscSupplierTotalPpmDesc(List<String> ppmMonths);
}
