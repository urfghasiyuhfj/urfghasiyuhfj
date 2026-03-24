package com.ppm.service;

import com.alibaba.excel.EasyExcel;
import com.ppm.dto.PpmSummaryQueryDto;
import com.ppm.dto.QuerySupplyDto;
import com.ppm.dto.QuerySuspiciousDto;
import com.ppm.dto.excel.PpmSummaryExportRow;
import com.ppm.dto.excel.SuspiciousExportRow;
import com.ppm.dto.excel.SupplyExportRow;
import com.ppm.entity.SupplierPpmDetail;
import com.ppm.entity.SupplyVolume;
import com.ppm.entity.SuspiciousMaterial;
import com.ppm.repository.SupplierPpmDetailRepository;
import com.ppm.repository.SupplyVolumeRepository;
import com.ppm.repository.SuspiciousMaterialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Excel 导出服务：PPM 汇总、可疑物料、供货量。
 * 从 supplier_ppm_detail 表导出数据。
 */
@Service
@RequiredArgsConstructor
public class ExportService {

    private final SupplierPpmDetailRepository detailRepository;
    private final SuspiciousMaterialRepository suspiciousMaterialRepository;
    private final SupplyVolumeRepository supplyVolumeRepository;

    private static final int EXPORT_MAX = 10000;

    public void exportPpmSummary(PpmSummaryQueryDto dto, HttpServletResponse response) throws IOException {
        Specification<SupplierPpmDetail> spec = buildPpmSpec(dto);
        List<SupplierPpmDetail> list = detailRepository.findAll(spec,
                PageRequest.of(0, EXPORT_MAX, Sort.by(Sort.Direction.DESC, "supplierTotalPpm"))).getContent();

        // 按供应商整体维度导出
        List<PpmSummaryExportRow> rows = list.stream()
                .map(this::toPpmExportRow)
                .toList();
        writeExcel(response, "PPM汇总", PpmSummaryExportRow.class, rows);
    }

    public void exportSuspicious(QuerySuspiciousDto dto, HttpServletResponse response) throws IOException {
        Specification<SuspiciousMaterial> spec = buildSuspSpec(dto);
        List<SuspiciousMaterial> list = suspiciousMaterialRepository.findAll(spec,
                PageRequest.of(0, EXPORT_MAX, Sort.by(Sort.Direction.DESC, "recordDate", "id"))).getContent();
        List<SuspiciousExportRow> rows = list.stream().map(this::toSuspExportRow).toList();
        writeExcel(response, "可疑物料", SuspiciousExportRow.class, rows);
    }

    public void exportSupply(QuerySupplyDto dto, HttpServletResponse response) throws IOException {
        Specification<SupplyVolume> spec = buildSupplySpec(dto);
        List<SupplyVolume> list = supplyVolumeRepository.findAll(spec,
                PageRequest.of(0, EXPORT_MAX, Sort.by(Sort.Direction.DESC, "fiscalYear", "plantId", "id"))).getContent();
        List<SupplyExportRow> rows = list.stream().map(this::toSupplyExportRow).toList();
        writeExcel(response, "供货量", SupplyExportRow.class, rows);
    }

    private Specification<SupplierPpmDetail> buildPpmSpec(PpmSummaryQueryDto dto) {
        return (root, q, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (dto.getPpmMonth() != null && !dto.getPpmMonth().isBlank()) {
                ps.add(cb.equal(root.get("ppmMonth"), dto.getPpmMonth()));
            }
            if (dto.getSupplierCode() != null && !dto.getSupplierCode().isBlank()) {
                ps.add(cb.like(root.get("supplierCode"), "%" + dto.getSupplierCode().trim() + "%"));
            }
            if (dto.getSupplierName() != null && !dto.getSupplierName().isBlank()) {
                ps.add(cb.like(root.get("supplierName"), "%" + dto.getSupplierName().trim() + "%"));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
    }

    private Specification<SuspiciousMaterial> buildSuspSpec(QuerySuspiciousDto dto) {
        return (root, q, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (dto.getPlant() != null && !dto.getPlant().isBlank()) {
                ps.add(cb.like(root.get("plant"), "%" + dto.getPlant().trim() + "%"));
            }
            if (dto.getSupplierCode() != null && !dto.getSupplierCode().isBlank()) {
                ps.add(cb.like(root.get("supplierCode"), "%" + dto.getSupplierCode().trim() + "%"));
            }
            if (dto.getSupplierName() != null && !dto.getSupplierName().isBlank()) {
                ps.add(cb.like(root.get("supplierName"), "%" + dto.getSupplierName().trim() + "%"));
            }
            if (dto.getPartCode() != null && !dto.getPartCode().isBlank()) {
                ps.add(cb.like(root.get("partCode"), "%" + dto.getPartCode().trim() + "%"));
            }
            if (dto.getPartName() != null && !dto.getPartName().isBlank()) {
                ps.add(cb.like(root.get("partName"), "%" + dto.getPartName().trim() + "%"));
            }
            if (dto.getRecordDateFrom() != null) {
                ps.add(cb.greaterThanOrEqualTo(root.get("recordDate"), dto.getRecordDateFrom()));
            }
            if (dto.getRecordDateTo() != null) {
                ps.add(cb.lessThanOrEqualTo(root.get("recordDate"), dto.getRecordDateTo()));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
    }

    private Specification<SupplyVolume> buildSupplySpec(QuerySupplyDto dto) {
        return (root, q, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (dto.getFiscalYear() != null) {
                ps.add(cb.equal(root.get("fiscalYear"), dto.getFiscalYear().shortValue()));
            }
            if (dto.getBaseCode() != null && !dto.getBaseCode().isBlank()) {
                ps.add(cb.equal(root.get("baseCode"), dto.getBaseCode().trim()));
            }
            if (dto.getPlantId() != null && !dto.getPlantId().isBlank()) {
                ps.add(cb.like(root.get("plantId"), "%" + dto.getPlantId().trim() + "%"));
            }
            if (dto.getSupplierCode() != null && !dto.getSupplierCode().isBlank()) {
                ps.add(cb.like(root.get("supplierCode"), "%" + dto.getSupplierCode().trim() + "%"));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
    }

    /**
     * 按供应商整体维度导出
     */
    private PpmSummaryExportRow toPpmExportRow(SupplierPpmDetail e) {
        PpmSummaryExportRow r = new PpmSummaryExportRow();
        r.setPpmMonth(e.getPpmMonth());
        r.setBaseCode(null);
        r.setBaseName(null);
        r.setSupplierCode(e.getSupplierCode());
        r.setSupplierName(e.getSupplierName());
        r.setDefectCount(e.getMonthDefectCount() != null ? e.getMonthDefectCount().intValue() : 0);
        r.setSupplyQty(e.getMonthSupplyQty() != null ? e.getMonthSupplyQty().intValue() : 0);
        r.setPpm(e.getSupplierTotalPpm());
        return r;
    }

    /**
     * 按基地维度导出
     */
    private PpmSummaryExportRow toPpmExportRowByBase(SupplierPpmDetail e, String baseCode) {
        PpmSummaryExportRow r = new PpmSummaryExportRow();
        r.setPpmMonth(e.getPpmMonth());
        r.setBaseCode(baseCode);
        r.setBaseName(baseCode);
        r.setSupplierCode(e.getSupplierCode());
        r.setSupplierName(e.getSupplierName());
        r.setDefectCount(getBaseDefectCount(e, baseCode));
        r.setSupplyQty(getBaseSupplyQty(e, baseCode));
        r.setPpm(getBasePpm(e, baseCode));
        return r;
    }

    private int getBaseDefectCount(SupplierPpmDetail d, String baseCode) {
        if (d == null || baseCode == null) return 0;
        BigDecimal count = switch (baseCode) {
            case "河西" -> d.getHexiSuspiciousCount();
            case "宝骏" -> d.getBaojunSuspiciousCount();
            case "青岛" -> d.getQingdaoSuspiciousCount();
            case "重庆" -> d.getChongqingSuspiciousCount();
            default -> BigDecimal.ZERO;
        };
        return count != null ? count.intValue() : 0;
    }

    private int getBaseSupplyQty(SupplierPpmDetail d, String baseCode) {
        if (d == null || baseCode == null) return 0;
        BigDecimal qty = switch (baseCode) {
            case "河西" -> d.getHexiSupplyQty();
            case "宝骏" -> d.getBaojunSupplyQty();
            case "青岛" -> d.getQingdaoSupplyQty();
            case "重庆" -> d.getChongqingSupplyQty();
            default -> BigDecimal.ZERO;
        };
        return qty != null ? qty.intValue() : 0;
    }

    private BigDecimal getBasePpm(SupplierPpmDetail d, String baseCode) {
        if (d == null || baseCode == null) return BigDecimal.ZERO;
        return switch (baseCode) {
            case "河西" -> d.getHexiSupplierPpm();
            case "宝骏" -> d.getBaojunSupplierPpm();
            case "青岛" -> d.getQingdaoSupplierPpm();
            case "重庆" -> d.getChongqingSupplierPpm();
            default -> BigDecimal.ZERO;
        };
    }

    private SuspiciousExportRow toSuspExportRow(SuspiciousMaterial e) {
        SuspiciousExportRow r = new SuspiciousExportRow();
        r.setPlant(e.getPlant());
        r.setRecordDate(e.getRecordDate() != null ? java.sql.Date.valueOf(e.getRecordDate()) : null);
        r.setPartCode(e.getPartCode());
        r.setPartName(e.getPartName());
        r.setSupplierCode(e.getSupplierCode());
        r.setSupplierName(e.getSupplierName());
        r.setQuantity(e.getQuantity());
        r.setFaultType(e.getFaultType());
        r.setOrderDate(e.getOrderDate() != null ? java.sql.Date.valueOf(e.getOrderDate()) : null);
        r.setSupplierResp(e.getSupplierResp());
        return r;
    }

    private SupplyExportRow toSupplyExportRow(SupplyVolume e) {
        SupplyExportRow r = new SupplyExportRow();
        r.setFiscalYear(e.getFiscalYear());
        r.setBaseCode(e.getBaseCode());
        r.setPlantId(e.getPlantId());
        r.setSupplierCode(e.getSupplierCode());
        r.setSupplierName(e.getSupplierName());
        r.setPartCode(e.getPartCode());
        r.setPartName(e.getPartName());
        r.setMonth1No(e.getMonth1No());
        r.setMonth2No(e.getMonth2No());
        r.setMonth3No(e.getMonth3No());
        r.setMonth4No(e.getMonth4No());
        r.setMonth5No(e.getMonth5No());
        r.setMonth6No(e.getMonth6No());
        r.setMonth7No(e.getMonth7No());
        r.setMonth8No(e.getMonth8No());
        r.setMonth9No(e.getMonth9No());
        r.setMonth10No(e.getMonth10No());
        r.setMonth11No(e.getMonth11No());
        r.setMonth12No(e.getMonth12No());
        return r;
    }

    private <T> void writeExcel(HttpServletResponse response, String sheetName, Class<T> clazz, List<T> rows)
            throws IOException {
        String fileName = URLEncoder.encode(sheetName + "_" + System.currentTimeMillis(), StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20") + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName);
        EasyExcel.write(response.getOutputStream(), clazz).sheet(sheetName).doWrite(rows);
    }
}
