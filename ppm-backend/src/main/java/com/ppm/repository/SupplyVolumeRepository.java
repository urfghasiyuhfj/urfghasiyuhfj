package com.ppm.repository;

import com.ppm.entity.SupplyVolume;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SupplyVolumeRepository extends JpaRepository<SupplyVolume, Long>,
        JpaSpecificationExecutor<SupplyVolume> {

    Page<SupplyVolume> findByFiscalYearAndPlantId(Short fiscalYear, String plantId, Pageable pageable);

    Page<SupplyVolume> findBySupplierCode(String supplierCode, Pageable pageable);

    /**
     * 根据 fiscalYear, plantId, supplierCode, partCode 查询已存在的供货量记录
     */
    @Query("SELECT s FROM SupplyVolume s WHERE s.fiscalYear = :fiscalYear AND s.plantId = :plantId " +
           "AND s.supplierCode = :supplierCode AND s.partCode = :partCode")
    List<SupplyVolume> findByFiscalYearAndPlantIdAndSupplierCodeAndPartCode(
            @Param("fiscalYear") Short fiscalYear,
            @Param("plantId") String plantId,
            @Param("supplierCode") String supplierCode,
            @Param("partCode") String partCode);

    /**
     * 根据 fiscalYear, supplierCode, partCode 查询已存在的供货量记录
     */
    @Query("SELECT s FROM SupplyVolume s WHERE s.fiscalYear = :fiscalYear " +
           "AND s.supplierCode = :supplierCode AND s.partCode = :partCode")
    List<SupplyVolume> findByFiscalYearAndSupplierCodeAndPartCode(
            @Param("fiscalYear") Short fiscalYear,
            @Param("supplierCode") String supplierCode,
            @Param("partCode") String partCode);
}
