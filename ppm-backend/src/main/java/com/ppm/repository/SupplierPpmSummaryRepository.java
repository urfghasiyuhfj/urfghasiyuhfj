package com.ppm.repository;

import com.ppm.entity.SupplierPpmSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SupplierPpmSummaryRepository extends JpaRepository<SupplierPpmSummary, Long>,
        JpaSpecificationExecutor<SupplierPpmSummary> {

    Page<SupplierPpmSummary> findByPpmMonth(String ppmMonth, Pageable pageable);

    List<SupplierPpmSummary> findByPpmMonthOrderByPpmDesc(String ppmMonth);

    void deleteByPpmMonth(String ppmMonth);

    @Query("SELECT DISTINCT s.ppmMonth FROM SupplierPpmSummary s ORDER BY s.ppmMonth DESC")
    List<String> findDistinctPpmMonths();

    List<SupplierPpmSummary> findByBaseCodeOrderByPpmMonthAsc(String baseCode);

    List<SupplierPpmSummary> findByPpmMonthInOrderByPpmMonthAsc(List<String> ppmMonths);
}
