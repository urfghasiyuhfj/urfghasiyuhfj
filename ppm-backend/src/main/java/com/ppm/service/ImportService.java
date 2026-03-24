package com.ppm.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.ppm.dto.excel.SupplierPpmExcelRow;
import com.ppm.dto.excel.SuspiciousMaterialExcelRow;
import com.ppm.dto.excel.SupplyVolumeExcelRow;
import com.ppm.entity.SupplierPpmDetail;
import com.ppm.entity.SuspiciousMaterial;
import com.ppm.entity.SupplyVolume;
import com.ppm.dto.ImportFromFolderResult;
import com.ppm.dto.ImportErrorItem;
import com.ppm.dto.ImportResult;
import com.ppm.dto.PpmCalculateResult;
import com.ppm.dto.SupplyVolumeConflictVO;
import com.ppm.repository.SupplierPpmDetailRepository;
import com.ppm.repository.SuspiciousMaterialRepository;
import com.ppm.repository.SupplyVolumeRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Excel 导入服务：解析供应商 PPM、可疑物料、供货量 Excel，写入 MySQL。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImportService {

    private static final String[] BASE_CODES = {"河西", "宝骏", "青岛", "重庆"};
    private static final int MAX_LEN_8 = 8;
    private static final int MAX_LEN_32 = 32;
    private static final int MAX_LEN_64 = 64;
    private static final int MAX_LEN_128 = 128;
    private static final int MAX_LEN_256 = 256;

    private final SupplierPpmDetailRepository detailRepository;
    private final SuspiciousMaterialRepository suspiciousMaterialRepository;
    private final SupplyVolumeRepository supplyVolumeRepository;
    private final PpmCalculateService ppmCalculateService;
    private final PlatformTransactionManager transactionManager;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(rollbackFor = Exception.class)
    public ImportResult importSupplierPpm(MultipartFile file) {
        return importSupplierPpm(() -> file.getInputStream());
    }

    @Transactional(rollbackFor = Exception.class)
    public ImportResult importSuspiciousMaterial(MultipartFile file) {
        return importSuspiciousMaterial(() -> file.getInputStream());
    }

    public ImportResult importSupplyVolume(MultipartFile file) {
        // 供货量需要从文件名推断月份（如：重庆供货量2510/供货量202510）
        return importSupplyVolume(() -> file.getInputStream(), file.getOriginalFilename());
    }

    @Transactional(rollbackFor = Exception.class)
    public ImportResult importSupplierPpm(InputStreamProvider inProvider) {
        try (InputStream in = inProvider.open()) {
            List<SupplierPpmExcelRow> rows = readExcel(in, SupplierPpmExcelRow.class);
            List<ImportErrorItem> errors = new ArrayList<>();
            int n = persistSupplierPpm(rows, errors);
            return new ImportResult(n, List.of(), errors);
        } catch (Exception e) {
            log.warn("导入供应商 PPM 失败", e);
            throw new RuntimeException("导入供应商 PPM 失败: " + e.getMessage(), e);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public ImportResult importSuspiciousMaterial(InputStreamProvider inProvider) {
        try (InputStream in = inProvider.open()) {
            List<SuspiciousMaterialExcelRow> rows = readExcel(in, SuspiciousMaterialExcelRow.class);
            List<ImportErrorItem> errors = new ArrayList<>();
            List<SuspiciousMaterialExcelRow> validRows = new ArrayList<>();
            int n = persistSuspiciousMaterial(rows, errors, validRows);
            List<PpmCalculateResult> recalc = triggerPpmRecalcFromSuspicious(validRows);
            return new ImportResult(n, recalc, errors);
        } catch (Exception e) {
            log.warn("导入可疑物料统计失败", e);
            throw new RuntimeException("导入可疑物料统计失败: " + e.getMessage(), e);
        }
    }

    public ImportResult importSupplyVolume(InputStreamProvider inProvider) {
        return importSupplyVolume(inProvider, null);
    }

    public ImportResult importSupplyVolume(InputStreamProvider inProvider, String filenameHint) {
        try (InputStream in = inProvider.open()) {
            List<SupplyVolumeExcelRow> rows = readExcel(in, SupplyVolumeExcelRow.class);
            List<ImportErrorItem> errors = new ArrayList<>();
            List<SupplyVolumeExcelRow> validRows = new ArrayList<>();
            List<String> ppmMonthsImported = new ArrayList<>();

            // 跳过冲突检测，直接导入（覆盖模式）
            // 关键：供货量"落库"与"自动计算"解耦。
            // 落库先在独立事务内提交；自动计算失败也不会把导入标记为 rollback-only。
            TransactionTemplate tt = new TransactionTemplate(transactionManager);
            Integer nBoxed = tt.execute(status -> persistSupplyVolume(rows, errors, validRows, filenameHint, ppmMonthsImported));
            int n = nBoxed != null ? nBoxed : 0;

            ImportResult result = new ImportResult();
            List<PpmCalculateResult> recalc = triggerPpmRecalcFromSupply(ppmMonthsImported.stream().distinct().toList());
            result.setRowsImported(n);
            result.setPpmRecalculations(recalc);
            result.setValidationErrors(errors);
            result.setHasConflicts(false);
            return result;
        } catch (Exception e) {
            log.warn("导入供货量失败", e);
            throw new RuntimeException("导入供货量失败: " + e.getMessage(), e);
        }
    }

    /**
     * 检测供货量导入冲突
     * @return 冲突列表，如果为空则表示无冲突
     */
    public List<SupplyVolumeConflictVO> checkSupplyVolumeConflicts(MultipartFile file) {
        try {
            List<SupplyVolumeExcelRow> rows = readExcel(file.getInputStream(), SupplyVolumeExcelRow.class);
            List<ImportErrorItem> errors = new ArrayList<>();
            List<SupplyVolumeExcelRow> validRows = new ArrayList<>();
            return checkSupplyVolumeConflicts(rows, file.getOriginalFilename(), validRows, errors);
        } catch (Exception e) {
            log.warn("检测供货量冲突失败", e);
            throw new RuntimeException("检测供货量冲突失败: " + e.getMessage(), e);
        }
    }

    /**
     * 检测供货量导入冲突（核心方法）
     */
    private List<SupplyVolumeConflictVO> checkSupplyVolumeConflicts(List<SupplyVolumeExcelRow> rows,
                                                                    String filenameHint,
                                                                    List<SupplyVolumeExcelRow> validRows,
                                                                    List<ImportErrorItem> errors) {
        List<SupplyVolumeConflictVO> conflicts = new ArrayList<>();

        if (rows == null || rows.isEmpty()) {
            return conflicts;
        }

        String defaultBaseCode = inferBaseCodeFromFilename(filenameHint);
        String defaultPlantId = inferPlantIdFromFilename(filenameHint);
        Integer defaultYear = inferFiscalYearFromFilename(filenameHint);

        for (int i = 0; i < rows.size(); i++) {
            SupplyVolumeExcelRow r = rows.get(i);
            int excelRow = i + 2;

            Double fy = r.getFiscalYearDouble();
            if (fy == null && defaultYear != null) {
                fy = defaultYear.doubleValue();
            }
            String plantId = normalizeNumericLike(trimToNull(r.getPlantId()));
            if ((plantId == null || plantId.isBlank()) && defaultPlantId != null) {
                plantId = defaultPlantId;
            }
            String supplierCode = normalizeNumericLike(trimToNull(r.getSupplierCode()));
            String partCode = normalizeNumericLike(trimToNull(r.getPartCode()));

            // 基本校验
            if (fy == null || plantId == null || plantId.isBlank() ||
                supplierCode == null || supplierCode.isBlank() ||
                partCode == null || partCode.isBlank()) {
                continue;
            }

            validRows.add(r);

            // 查询数据库中已存在的记录
            Short fiscalYearShort = (short) (int) Math.round(fy.doubleValue());
            List<SupplyVolume> existingRecords = supplyVolumeRepository
                    .findByFiscalYearAndPlantIdAndSupplierCodeAndPartCode(fiscalYearShort, plantId, supplierCode, partCode);

            if (existingRecords.isEmpty()) {
                continue;
            }

            // 检查每个月份是否有冲突
            SupplyVolume existing = existingRecords.get(0);
            Double[] monthValues = {r.getMonth1NoDouble(), r.getMonth2NoDouble(), r.getMonth3NoDouble(), r.getMonth4NoDouble(),
                                    r.getMonth5NoDouble(), r.getMonth6NoDouble(), r.getMonth7NoDouble(), r.getMonth8NoDouble(),
                                    r.getMonth9NoDouble(), r.getMonth10NoDouble(), r.getMonth11NoDouble(), r.getMonth12NoDouble()};

            for (int month = 1; month <= 12; month++) {
                Double newValue = monthValues[month - 1];
                if (newValue == null) {
                    continue;
                }
                int newQty = newValue.intValue();
                int oldQty = existing.getMonthXNo(month);

                if (newQty != oldQty) {
                    // 存在差异，记录冲突
                    SupplyVolumeConflictVO conflict = new SupplyVolumeConflictVO();
                    conflict.setConflictKey(UUID.randomUUID().toString());
                    conflict.setSupplierCode(supplierCode);
                    conflict.setSupplierName(r.getSupplierName());
                    conflict.setPartCode(partCode);
                    conflict.setPartName(r.getPartName());
                    conflict.setBaseCode(existing.getBaseCode());
                    conflict.setPlantId(plantId);
                    conflict.setFiscalYear(fy.intValue());
                    conflict.setMonth(month);
                    conflict.setOldSupplyQty(oldQty);
                    conflict.setNewSupplyQty(newQty);
                    conflict.setDiffQty(newQty - oldQty);
                    conflicts.add(conflict);
                }
            }
        }

        return conflicts;
    }

    /**
     * 供货量覆盖导入（用户确认后执行）- 支持覆盖已存在的数据
     */
    @Transactional(rollbackFor = Exception.class)
    public ImportResult importSupplyVolumeWithOverride(MultipartFile file, boolean overwrite) {
        if (!overwrite) {
            throw new IllegalArgumentException("覆盖标志必须为 true");
        }

        try {
            List<SupplyVolumeExcelRow> rows = readExcel(file.getInputStream(), SupplyVolumeExcelRow.class);
            List<ImportErrorItem> errors = new ArrayList<>();
            List<String> ppmMonthsImported = new ArrayList<>();

            // 覆盖导入：直接使用所有有效的行，不检测冲突
            // 因为覆盖导入的目的就是用新数据覆盖旧数据
            List<SupplyVolumeExcelRow> validRows = new ArrayList<>();
            for (SupplyVolumeExcelRow r : rows) {
                // 基本校验：财年、工厂、供应商、零件必填
                if (r.getFiscalYearDouble() != null &&
                    r.getPlantId() != null && !r.getPlantId().isBlank() &&
                    r.getSupplierCode() != null && !r.getSupplierCode().isBlank() &&
                    r.getPartCode() != null && !r.getPartCode().isBlank()) {
                    validRows.add(r);
                }
            }

            ImportResult result = new ImportResult();
            
            // 执行覆盖导入
            TransactionTemplate tt = new TransactionTemplate(transactionManager);
            Integer nBoxed = tt.execute(status -> persistSupplyVolumeWithOverwrite(
                    rows, errors, validRows, file.getOriginalFilename(), ppmMonthsImported));
            int n = nBoxed != null ? nBoxed : 0;

            List<PpmCalculateResult> recalc = triggerPpmRecalcFromSupply(
                    ppmMonthsImported.stream().distinct().toList());
            result.setRowsImported(n);
            result.setPpmRecalculations(recalc);
            result.setValidationErrors(errors);
            return result;
        } catch (Exception e) {
            log.warn("覆盖导入供货量失败", e);
            throw new RuntimeException("覆盖导入供货量失败: " + e.getMessage(), e);
        }
    }

    /**
     * 持久化供货量数据（支持覆盖模式）
     */
    private int persistSupplyVolumeWithOverwrite(List<SupplyVolumeExcelRow> rows,
                                                  List<ImportErrorItem> errors,
                                                  List<SupplyVolumeExcelRow> validRows,
                                                  String filenameHint,
                                                  List<String> outPpmMonthsImported) {
        if (rows == null || rows.isEmpty()) return 0;

        String defaultBaseCode = inferBaseCodeFromFilename(filenameHint);
        String defaultPlantId = inferPlantIdFromFilename(filenameHint);
        Integer defaultYear = inferFiscalYearFromFilename(filenameHint);
        Set<String> importedMonths = new HashSet<>();

        for (int i = 0; i < rows.size(); i++) {
            SupplyVolumeExcelRow r = rows.get(i);
            int excelRow = i + 2;

            Double fy = r.getFiscalYearDouble();
            if (fy == null && defaultYear != null) fy = defaultYear.doubleValue();
            String plantId = normalizeNumericLike(trimToNull(r.getPlantId()));
            if ((plantId == null || plantId.isBlank()) && defaultPlantId != null) {
                plantId = defaultPlantId;
            }
            String supplierCode = normalizeNumericLike(trimToNull(r.getSupplierCode()));
            String partCode = normalizeNumericLike(trimToNull(r.getPartCode()));

            // 基本校验
            if (fy == null) {
                errors.add(new ImportErrorItem(excelRow, "fiscal_year", "不能为空"));
                continue;
            }
            if (plantId == null || plantId.isBlank()) {
                errors.add(new ImportErrorItem(excelRow, "plant_id", "不能为空"));
                continue;
            }
            if (supplierCode == null || supplierCode.isBlank()) {
                errors.add(new ImportErrorItem(excelRow, "supplier_code", "不能为空"));
                continue;
            }
            if (partCode == null || partCode.isBlank()) {
                errors.add(new ImportErrorItem(excelRow, "part_code", "不能为空"));
                continue;
            }

            // 检查所有月份的供货量是否都是null
            boolean allMonthNull = (r.getMonth1NoDouble() == null) && (r.getMonth2NoDouble() == null) &&
                    (r.getMonth3NoDouble() == null) && (r.getMonth4NoDouble() == null) &&
                    (r.getMonth5NoDouble() == null) && (r.getMonth6NoDouble() == null) &&
                    (r.getMonth7NoDouble() == null) && (r.getMonth8NoDouble() == null) &&
                    (r.getMonth9NoDouble() == null) && (r.getMonth10NoDouble() == null) &&
                    (r.getMonth11NoDouble() == null) && (r.getMonth12NoDouble() == null);
            if (allMonthNull) {
                errors.add(new ImportErrorItem(excelRow, "month_x_no", "供货量不能全部为空"));
                continue;
            }

            validRows.add(r);

            // 记录被导入的月份
            if (outPpmMonthsImported != null && defaultYear != null) {
                if (r.getMonth1NoDouble() != null) importedMonths.add(String.format("%d01", defaultYear));
                if (r.getMonth2NoDouble() != null) importedMonths.add(String.format("%d02", defaultYear));
                if (r.getMonth3NoDouble() != null) importedMonths.add(String.format("%d03", defaultYear));
                if (r.getMonth4NoDouble() != null) importedMonths.add(String.format("%d04", defaultYear));
                if (r.getMonth5NoDouble() != null) importedMonths.add(String.format("%d05", defaultYear));
                if (r.getMonth6NoDouble() != null) importedMonths.add(String.format("%d06", defaultYear));
                if (r.getMonth7NoDouble() != null) importedMonths.add(String.format("%d07", defaultYear));
                if (r.getMonth8NoDouble() != null) importedMonths.add(String.format("%d08", defaultYear));
                if (r.getMonth9NoDouble() != null) importedMonths.add(String.format("%d09", defaultYear));
                if (r.getMonth10NoDouble() != null) importedMonths.add(String.format("%d10", defaultYear));
                if (r.getMonth11NoDouble() != null) importedMonths.add(String.format("%d11", defaultYear));
                if (r.getMonth12NoDouble() != null) importedMonths.add(String.format("%d12", defaultYear));
            }

            Short fiscalYearShort = (short) (int) Math.round(fy.doubleValue());

            // 查询已存在的记录
            List<SupplyVolume> existingRecords = supplyVolumeRepository
                    .findByFiscalYearAndPlantIdAndSupplierCodeAndPartCode(
                            fiscalYearShort, plantId, supplierCode, partCode);

            SupplyVolume e;
            if (!existingRecords.isEmpty()) {
                // 更新已存在的记录
                e = existingRecords.get(0);
            } else {
                // 创建新记录
                e = new SupplyVolume();
                e.setCreateDate(LocalDateTime.now());
                e.setDataSource("Excel导入");
                e.setDataVer(LocalDate.now());
                e.setEtlCreateDate(LocalDateTime.now());
                e.setFiscalYear(fiscalYearShort);
            }

            String baseCodeVal = normalizeNumericLike(trimToNull(r.getBaseCode()));
            if ((baseCodeVal == null || baseCodeVal.isBlank()) && defaultBaseCode != null && !defaultBaseCode.isBlank()) {
                baseCodeVal = defaultBaseCode;
            }
            // 如果 baseCode 仍为空，根据 plant_id 推断
            if (baseCodeVal == null || baseCodeVal.isBlank()) {
                baseCodeVal = inferBaseCodeFromPlantId(plantId);
            }

            e.setBaseCode(trunc(baseCodeVal, MAX_LEN_32));
            e.setPlantId(trunc(plantId, MAX_LEN_32));
            e.setSupplierCode(trunc(supplierCode, MAX_LEN_32));
            e.setSupplierName(trunc(r.getSupplierName(), MAX_LEN_128));
            e.setPartCode(trunc(partCode, MAX_LEN_64));
            e.setPartName(trunc(r.getPartName(), MAX_LEN_128));
            e.setMonth1No(r.getMonth1NoDouble() == null ? 0 : r.getMonth1NoDouble().intValue());
            e.setMonth2No(r.getMonth2NoDouble() == null ? 0 : r.getMonth2NoDouble().intValue());
            e.setMonth3No(r.getMonth3NoDouble() == null ? 0 : r.getMonth3NoDouble().intValue());
            e.setMonth4No(r.getMonth4NoDouble() == null ? 0 : r.getMonth4NoDouble().intValue());
            e.setMonth5No(r.getMonth5NoDouble() == null ? 0 : r.getMonth5NoDouble().intValue());
            e.setMonth6No(r.getMonth6NoDouble() == null ? 0 : r.getMonth6NoDouble().intValue());
            e.setMonth7No(r.getMonth7NoDouble() == null ? 0 : r.getMonth7NoDouble().intValue());
            e.setMonth8No(r.getMonth8NoDouble() == null ? 0 : r.getMonth8NoDouble().intValue());
            e.setMonth9No(r.getMonth9NoDouble() == null ? 0 : r.getMonth9NoDouble().intValue());
            e.setMonth10No(r.getMonth10NoDouble() == null ? 0 : r.getMonth10NoDouble().intValue());
            e.setMonth11No(r.getMonth11NoDouble() == null ? 0 : r.getMonth11NoDouble().intValue());
            e.setMonth12No(r.getMonth12NoDouble() == null ? 0 : r.getMonth12NoDouble().intValue());

            // 计算年度总供货量
            int total = (r.getMonth1NoDouble() == null ? 0 : r.getMonth1NoDouble().intValue()) +
                    (r.getMonth2NoDouble() == null ? 0 : r.getMonth2NoDouble().intValue()) +
                    (r.getMonth3NoDouble() == null ? 0 : r.getMonth3NoDouble().intValue()) +
                    (r.getMonth4NoDouble() == null ? 0 : r.getMonth4NoDouble().intValue()) +
                    (r.getMonth5NoDouble() == null ? 0 : r.getMonth5NoDouble().intValue()) +
                    (r.getMonth6NoDouble() == null ? 0 : r.getMonth6NoDouble().intValue()) +
                    (r.getMonth7NoDouble() == null ? 0 : r.getMonth7NoDouble().intValue()) +
                    (r.getMonth8NoDouble() == null ? 0 : r.getMonth8NoDouble().intValue()) +
                    (r.getMonth9NoDouble() == null ? 0 : r.getMonth9NoDouble().intValue()) +
                    (r.getMonth10NoDouble() == null ? 0 : r.getMonth10NoDouble().intValue()) +
                    (r.getMonth11NoDouble() == null ? 0 : r.getMonth11NoDouble().intValue()) +
                    (r.getMonth12NoDouble() == null ? 0 : r.getMonth12NoDouble().intValue());
            e.setTotalNo(total);
            e.setIfDocking("N");

            supplyVolumeRepository.save(e);
        }

        if (outPpmMonthsImported != null) {
            outPpmMonthsImported.addAll(importedMonths);
        }

        return rows.size();
    }

    /**
     * 从文件名推断供货量基地名。支持格式：PPM可疑物料分析_宝骏供货量2507、重庆供货量2510 等。
     * 「供货量」前的基地名（河西/宝骏/青岛/重庆）即为基地。
     */
    private static String inferBaseCodeFromFilename(String filename) {
        if (filename == null) return null;
        for (String base : BASE_CODES) {
            if (filename.contains(base + "供货量")) {
                return base;
            }
        }
        return null;
    }

    /**
     * 从 plant_id 推断基地编码
     * plant_id -> base_code 映射：
     * - 8200 -> 重庆
     * - 8000 -> 宝骏
     * - 1000 -> 河西
     * - 3000 -> 青岛
     * - 6430 -> 河西
     * - 6400 -> 青岛
     */
    private static String inferBaseCodeFromPlantId(String plantId) {
        if (plantId == null) return null;
        String pid = plantId.trim();
        return switch (pid) {
            case "8200","8300" -> "重庆";
            case "8000","8100" -> "宝骏";
            case "1000", "4000" -> "河西";
            case "3000", "5000" -> "青岛";
            case "河西" -> "河西";
            case "宝骏" -> "宝骏";
            case "青岛" -> "青岛";
            case "重庆" -> "重庆";
            default -> null;
        };
    }

    private static String inferPlantIdFromFilename(String filename) {
        if (filename == null) return null;
        String base = inferBaseCodeFromFilename(filename);
        if (base != null) {
            return switch (base) {
                case "重庆" -> "8200";
                case "河西" -> "6430";
                case "宝骏" -> "8000";
                case "青岛" -> "6400";
                default -> null;
            };
        }
        if (filename.contains("重庆")) return "8200";
        if (filename.contains("河西")) return "6430";
        if (filename.contains("宝骏")) return "8000";
        if (filename.contains("青岛")) return "6400";
        return null;
    }

    private static Integer inferFiscalYearFromFilename(String filename) {
        if (filename == null) return null;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("20[12][0-9]").matcher(filename);
        if (m.find()) return Integer.parseInt(m.group());
        m = java.util.regex.Pattern.compile("[2-3][0-9][0-1][0-9]").matcher(filename);
        if (m.find()) return 2000 + Integer.parseInt(m.group().substring(0, 2));
        return null;
    }

    /**
     * 从文件名推断供货量所属月份。支持格式：PPM可疑物料分析_宝骏供货量2507、重庆供货量2510 等。
     * - yyyyMM：如 202510
     * - yymm：如 2507 -> 202507, 2510 -> 202510
     */
    private static String inferPpmMonthFromFilename(String filename) {
        if (filename == null) return null;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(20\\d{2})(0[1-9]|1[0-2])").matcher(filename);
        if (m.find()) return m.group(1) + m.group(2);
        m = java.util.regex.Pattern.compile("([2-3]\\d)(0[1-9]|1[0-2])").matcher(filename);
        if (m.find()) return "20" + m.group(1) + m.group(2);
        return null;
    }

    private List<PpmCalculateResult> triggerPpmRecalcFromSuspicious(List<SuspiciousMaterialExcelRow> rows) {
        if (rows == null || rows.isEmpty()) return List.of();
        Set<String> months = rows.stream()
                .map(SuspiciousMaterialExcelRow::getOrderDate)  // 使用开单日期 orderDate 而非录入日期 recordDate
                .filter(java.util.Objects::nonNull)
                .map(d -> {
                    LocalDate ld = Instant.ofEpochMilli(d.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
                    return String.format("%04d%02d", ld.getYear(), ld.getMonthValue());
                })
                .collect(Collectors.toSet());
        if (months.isEmpty()) {
            months = Set.of(YearMonth.now().toString().replace("-", ""));
        }
        flushIfTransactional();
        List<PpmCalculateResult> results = new ArrayList<>();
        for (String ppmMonth : months) {
            try {
                results.add(ppmCalculateService.calculate(ppmMonth));
            } catch (Exception e) {
                log.warn("PPM 自动计算失败: ppmMonth={}", ppmMonth, e);
            }
        }
        return results;
    }

    private List<PpmCalculateResult> triggerPpmRecalcFromSupply(List<String> ppmMonths) {
        if (ppmMonths == null || ppmMonths.isEmpty()) return List.of();
        flushIfTransactional();
        List<PpmCalculateResult> results = new ArrayList<>();
        for (String m : ppmMonths) {
            String norm = m != null ? m.trim().replace(".0", "") : "";
            if (!norm.matches("\\d{6}")) continue;
            try {
                results.add(ppmCalculateService.calculate(norm));
            } catch (Exception e) {
                log.warn("PPM 自动计算失败: ppmMonth={}", norm, e);
            }
        }
        return results;
    }

    private void flushIfTransactional() {
        if (entityManager == null) return;
        if (!TransactionSynchronizationManager.isActualTransactionActive()) return;
        entityManager.flush();
    }

    private static <T> List<T> readExcel(InputStream in, Class<T> clazz) {
        List<T> list = new ArrayList<>();
        EasyExcel.read(in, clazz, new AnalysisEventListener<T>() {
            @Override
            public void invoke(T data, AnalysisContext context) {
                list.add(data);
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext context) {
            }
        }).sheet(0).doRead();
        return list;
    }

    /**
     * 从 ppm 目录读取 Excel 并导入 MySQL。匹配文件名：*供应商PPM*、*可疑物料统计*、*供货量*。
     * 导入可疑物料或供货量时会自动触发当月 PPM 计算，并收集差异信息。
     */
    public ImportFromFolderResult importFromPpmFolder(Path folder) {
        if (!Files.isDirectory(folder)) {
            throw new IllegalArgumentException("ppm 导入目录不存在或不是目录: " + folder);
        }
        int ppmRows = 0;
        int suspRows = 0;
        int supplyRows = 0;
        List<PpmCalculateResult> allRecalc = new ArrayList<>();
        List<ImportErrorItem> allErrors = new ArrayList<>();
        try (var stream = Files.list(folder)) {
            var files = stream
                    .filter(p -> {
                        String n = p.getFileName().toString();
                        return n.endsWith(".xlsx") && !n.startsWith("~$");
                    })
                    .toList();
            for (Path f : files) {
                String name = f.getFileName().toString();
                try {
                    if (name.contains("供应商PPM")) {
                        ImportResult r = importSupplierPpm(() -> Files.newInputStream(f));
                        ppmRows += r.getRowsImported();
                        appendErrors(allErrors, r.getValidationErrors(), name);
                        log.info("已导入供应商 PPM: {} -> {} 行", name, r.getRowsImported());
                    } else if (name.contains("可疑物料统计")) {
                        ImportResult r = importSuspiciousMaterial(() -> Files.newInputStream(f));
                        suspRows += r.getRowsImported();
                        allRecalc.addAll(r.getPpmRecalculations());
                        appendErrors(allErrors, r.getValidationErrors(), name);
                        log.info("已导入可疑物料统计: {} -> {} 行", name, r.getRowsImported());
                    } else if (name.contains("供货量")) {
                        ImportResult r = importSupplyVolume(() -> Files.newInputStream(f), name);
                        supplyRows += r.getRowsImported();
                        allRecalc.addAll(r.getPpmRecalculations());
                        appendErrors(allErrors, r.getValidationErrors(), name);
                        log.info("已导入供货量: {} -> {} 行", name, r.getRowsImported());
                    }
                } catch (Exception e) {
                    log.warn("导入文件失败: {}", name, e);
                    throw new RuntimeException("导入 " + name + " 失败: " + e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            if (e instanceof RuntimeException re) throw re;
            throw new RuntimeException("读取 ppm 目录失败: " + e.getMessage(), e);
        }
        ImportFromFolderResult result = new ImportFromFolderResult(ppmRows, suspRows, supplyRows);
        result.setPpmRecalculations(allRecalc);
        result.setValidationErrors(allErrors);
        return result;
    }

    private int persistSupplierPpm(List<SupplierPpmExcelRow> rows, List<ImportErrorItem> errors) {
        if (rows == null || rows.isEmpty()) return 0;
        String ppmMonth = null;
        Map<String, SupplierPpmDetail> dedup = new LinkedHashMap<>();
        for (int i = 0; i < rows.size(); i++) {
            SupplierPpmExcelRow r = rows.get(i);
            int excelRow = i + 2;
            String sc = normalizeNumericLike(trimToNull(r.getSupplierCode()));
            String pm = trimToNull(r.getPpmMonth());
            if (sc == null || sc.isBlank()) {
                errors.add(new ImportErrorItem(excelRow, "供应商编码", "不能为空"));
                continue;
            }
            if (pm == null || pm.isBlank()) {
                errors.add(new ImportErrorItem(excelRow, "PPM月份", "不能为空"));
                continue;
            }
            if (!pm.matches("\\d{6}") && !pm.replace(".0", "").matches("\\d{6}")) {
                errors.add(new ImportErrorItem(excelRow, "PPM月份", "格式应为 yyyyMM，如 202510"));
                continue;
            }
            if (ppmMonth == null) ppmMonth = normalizePpmMonth(pm);
            if (!ppmMonth.equals(normalizePpmMonth(pm))) continue;
            String name = trunc(r.getSupplierName(), MAX_LEN_128);
            String pmNorm = normalizePpmMonth(pm);
            String scNorm = trunc(sc, MAX_LEN_32);
            
            // 按供应商聚合，不再按基地拆分
            String key = pmNorm + "|" + scNorm;
            SupplierPpmDetail e = dedup.getOrDefault(key, new SupplierPpmDetail());
            e.setPpmMonth(trunc(pmNorm, MAX_LEN_8));
            e.setSupplierCode(scNorm);
            e.setSupplierName(name != null ? name : "");
            
            // 累加各基地的数据
            int hexiDefect = n(r.getHexiDefect());
            int baojunDefect = n(r.getBaojunDefect());
            int qingdaoDefect = n(r.getQingdaoDefect());
            int chongqingDefect = n(r.getChongqingDefect());
            int hexiSupply = n(r.getHexiSupply());
            int baojunSupply = n(r.getBaojunSupply());
            int qingdaoSupply = n(r.getQingdaoSupply());
            int chongqingSupply = n(r.getChongqingSupply());
            
            // 设置或累加各基地数据
            e.setHexiSuspiciousCount(BigDecimal.valueOf(hexiDefect));
            e.setHexiSupplyQty(BigDecimal.valueOf(hexiSupply));
            e.setHexiSupplierPpm(ppmVal(r.getHexiPpm()));
            
            e.setBaojunSuspiciousCount(BigDecimal.valueOf(baojunDefect));
            e.setBaojunSupplyQty(BigDecimal.valueOf(baojunSupply));
            e.setBaojunSupplierPpm(ppmVal(r.getBaojunPpm()));
            
            e.setQingdaoSuspiciousCount(BigDecimal.valueOf(qingdaoDefect));
            e.setQingdaoSupplyQty(BigDecimal.valueOf(qingdaoSupply));
            e.setQingdaoSupplierPpm(ppmVal(r.getQingdaoPpm()));
            
            e.setChongqingSuspiciousCount(BigDecimal.valueOf(chongqingDefect));
            e.setChongqingSupplyQty(BigDecimal.valueOf(chongqingSupply));
            e.setChongqingSupplierPpm(ppmVal(r.getChongqingPpm()));
            
            // 计算供应商整体数据
            int totalDefect = hexiDefect + baojunDefect + qingdaoDefect + chongqingDefect;
            int totalSupply = hexiSupply + baojunSupply + qingdaoSupply + chongqingSupply;
            e.setMonthDefectCount(BigDecimal.valueOf(totalDefect));
            e.setMonthSupplyQty(BigDecimal.valueOf(totalSupply));
            if (totalSupply > 0) {
                e.setSupplierTotalPpm(BigDecimal.valueOf(totalDefect)
                        .multiply(BigDecimal.valueOf(1_000_000))
                        .divide(BigDecimal.valueOf(totalSupply), 2, RoundingMode.HALF_UP));
            } else {
                e.setSupplierTotalPpm(BigDecimal.ZERO);
            }
            
            dedup.put(key, e);
        }
        if (ppmMonth != null) detailRepository.deleteByPpmMonth(normalizePpmMonth(ppmMonth));
        List<SupplierPpmDetail> toSave = new ArrayList<>(dedup.values());
        return detailRepository.saveAll(toSave).size();
    }

    private static String normalizePpmMonth(String s) {
        return normalizeNumericLike(s);
    }

    private static String normalizeNumericLike(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty()) return null;
        if (t.endsWith(".0") && t.length() > 2) t = t.substring(0, t.length() - 2);
        return t;
    }

    private int persistSuspiciousMaterial(List<SuspiciousMaterialExcelRow> rows,
            List<ImportErrorItem> errors, List<SuspiciousMaterialExcelRow> validRows) {
        if (rows == null || rows.isEmpty()) return 0;
        List<SuspiciousMaterial> list = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            SuspiciousMaterialExcelRow r = rows.get(i);
            int excelRow = i + 2;
            String plant = normalizeNumericLike(trimToNull(r.getPlant()));
            String partCode = normalizeNumericLike(trimToNull(r.getPartCode()));
            String supplierCode = normalizeNumericLike(trimToNull(r.getSupplierCode()));
            if (plant == null || plant.isBlank()) {
                errors.add(new ImportErrorItem(excelRow, "区域工厂", "不能为空"));
                continue;
            }
            if (partCode == null || partCode.isBlank()) {
                errors.add(new ImportErrorItem(excelRow, "零件号", "不能为空"));
                continue;
            }
            if (supplierCode == null || supplierCode.isBlank()) {
                errors.add(new ImportErrorItem(excelRow, "供应商代码", "不能为空"));
                continue;
            }
            if (r.getQuantity() != null && r.getQuantity() < 0) {
                errors.add(new ImportErrorItem(excelRow, "数量", "不能为负数"));
                continue;
            }
            validRows.add(r);
            SuspiciousMaterial e = new SuspiciousMaterial();
            e.setPlant(trunc(plant, MAX_LEN_64));
            e.setRecordDate(toLocalDate(r.getRecordDate()));
            e.setRecorder(trunc(r.getRecorder(), MAX_LEN_32));
            e.setPartCode(trunc(partCode, MAX_LEN_64));
            e.setPartName(trunc(r.getPartName(), MAX_LEN_128));
            e.setFunctionModule(trunc(r.getFunctionModule(), MAX_LEN_64));
            e.setSupplierCode(trunc(supplierCode, MAX_LEN_32));
            e.setSupplierName(trunc(r.getSupplierName(), MAX_LEN_128));
            e.setModelMachine(trunc(r.getModelMachine(), MAX_LEN_64));
            e.setFailureDesc(trunc(r.getFailureDesc(), MAX_LEN_256));
            e.setFaultType(trunc(r.getFaultType(), MAX_LEN_64));
            e.setQuantity(r.getQuantity() != null ? r.getQuantity().intValue() : 0);
            e.setOrderDate(toLocalDate(r.getOrderDate()));
            e.setShiftSection(trunc(r.getShiftSection(), MAX_LEN_64));
            e.setProdArea(trunc(r.getProdArea(), MAX_LEN_64));
            e.setSupplierResp(firstChar(trimToNull(r.getSupplierResp())));
            e.setRemark(trunc(r.getRemark(), MAX_LEN_256));
            e.setSupplierPart(trunc(r.getSupplierPart(), MAX_LEN_128));
            e.setDefectCount(r.getDefectCount() != null ? r.getDefectCount().intValue() : null);
            e.setSupplyQty(r.getSupplyQty() != null ? r.getSupplyQty().intValue() : null);
            e.setBrand(trunc(r.getBrand(), MAX_LEN_64));
            list.add(e);
        }
        return suspiciousMaterialRepository.saveAll(list).size();
    }

    private int persistSupplyVolume(List<SupplyVolumeExcelRow> rows,
            List<ImportErrorItem> errors, List<SupplyVolumeExcelRow> validRows, String filenameHint,
            List<String> outPpmMonthsImported) {
        if (rows == null || rows.isEmpty()) return 0;
        String defaultBaseCode = inferBaseCodeFromFilename(filenameHint);
        String defaultPlantId = inferPlantIdFromFilename(filenameHint);
        Integer defaultYear = inferFiscalYearFromFilename(filenameHint);
        List<SupplyVolume> list = new ArrayList<>();
        // 用于检测Excel内的重复项：key -> 第一次出现的行号
        Map<String, Integer> seenKeys = new HashMap<>();
        // 收集所有被导入的月份，用于触发PPM计算
        Set<String> importedMonths = new HashSet<>();
        for (int i = 0; i < rows.size(); i++) {
            SupplyVolumeExcelRow r = rows.get(i);
            int excelRow = i + 2;
            Double fy = r.getFiscalYearDouble();
            if (fy == null && defaultYear != null) fy = defaultYear.doubleValue();
            String plantId = normalizeNumericLike(trimToNull(r.getPlantId()));
            if ((plantId == null || plantId.isBlank()) && defaultPlantId != null) {
                plantId = defaultPlantId;
            }
            String supplierCode = normalizeNumericLike(trimToNull(r.getSupplierCode()));
            String partCode = normalizeNumericLike(trimToNull(r.getPartCode()));
            // 读取12个月的供货量
            Double m1 = r.getMonth1NoDouble();
            Double m2 = r.getMonth2NoDouble();
            Double m3 = r.getMonth3NoDouble();
            Double m4 = r.getMonth4NoDouble();
            Double m5 = r.getMonth5NoDouble();
            Double m6 = r.getMonth6NoDouble();
            Double m7 = r.getMonth7NoDouble();
            Double m8 = r.getMonth8NoDouble();
            Double m9 = r.getMonth9NoDouble();
            Double m10 = r.getMonth10NoDouble();
            Double m11 = r.getMonth11NoDouble();
            Double m12 = r.getMonth12NoDouble();
            if (fy == null) {
                errors.add(new ImportErrorItem(excelRow, "fiscal_year", "不能为空"));
                continue;
            }
            if (plantId == null || plantId.isBlank()) {
                errors.add(new ImportErrorItem(excelRow, "plant_id", "不能为空"));
                continue;
            }
            if (supplierCode == null || supplierCode.isBlank()) {
                errors.add(new ImportErrorItem(excelRow, "supplier_code", "不能为空"));
                continue;
            }
            if (partCode == null || partCode.isBlank()) {
                errors.add(new ImportErrorItem(excelRow, "part_code", "不能为空"));
                continue;
            }
            // 检查是否所有月份的供货量都是null（未填写）
            boolean allMonthNull = (m1 == null) && (m2 == null) &&
                    (m3 == null) && (m4 == null) &&
                    (m5 == null) && (m6 == null) &&
                    (m7 == null) && (m8 == null) &&
                    (m9 == null) && (m10 == null) &&
                    (m11 == null) && (m12 == null);
            if (allMonthNull) {
                errors.add(new ImportErrorItem(excelRow, "month_x_no", "供货量不能全部为空"));
                continue;
            }
            // 生成唯一键用于检测重复：年份+工厂+供应商+零件号+12个月供货量
            Short fiscalYearShort = (short) (int) Math.round(fy.doubleValue());
            String duplicateKey = String.format("%d|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s",
                    fiscalYearShort, plantId, supplierCode, partCode,
                    m1 != null ? m1.toString() : "0", m2 != null ? m2.toString() : "0",
                    m3 != null ? m3.toString() : "0", m4 != null ? m4.toString() : "0",
                    m5 != null ? m5.toString() : "0", m6 != null ? m6.toString() : "0",
                    m7 != null ? m7.toString() : "0", m8 != null ? m8.toString() : "0",
                    m9 != null ? m9.toString() : "0", m10 != null ? m10.toString() : "0",
                    m11 != null ? m11.toString() : "0", m12 != null ? m12.toString() : "0");
            
            if (seenKeys.containsKey(duplicateKey)) {
                // 发现重复项，记录警告并跳过
                int firstRow = seenKeys.get(duplicateKey);
                errors.add(new ImportErrorItem(excelRow, "重复数据", 
                        String.format("与第%d行重复（年份=%d, 工厂=%s, 供应商=%s, 零件=%s, 供货量相同），已跳过", 
                                firstRow, fiscalYearShort, plantId, supplierCode, partCode)));
                continue;
            }
            seenKeys.put(duplicateKey, excelRow);
            validRows.add(r);
            // 记录被导入的月份（使用实际的 fiscalYear 而非文件名推断的 defaultYear）
            if (outPpmMonthsImported != null) {
                if (m1 != null && m1 > 0) importedMonths.add(String.format("%d01", fiscalYearShort));
                if (m2 != null && m2 > 0) importedMonths.add(String.format("%d02", fiscalYearShort));
                if (m3 != null && m3 > 0) importedMonths.add(String.format("%d03", fiscalYearShort));
                if (m4 != null && m4 > 0) importedMonths.add(String.format("%d04", fiscalYearShort));
                if (m5 != null && m5 > 0) importedMonths.add(String.format("%d05", fiscalYearShort));
                if (m6 != null && m6 > 0) importedMonths.add(String.format("%d06", fiscalYearShort));
                if (m7 != null && m7 > 0) importedMonths.add(String.format("%d07", fiscalYearShort));
                if (m8 != null && m8 > 0) importedMonths.add(String.format("%d08", fiscalYearShort));
                if (m9 != null && m9 > 0) importedMonths.add(String.format("%d09", fiscalYearShort));
                if (m10 != null && m10 > 0) importedMonths.add(String.format("%d10", fiscalYearShort));
                if (m11 != null && m11 > 0) importedMonths.add(String.format("%d11", fiscalYearShort));
                if (m12 != null && m12 > 0) importedMonths.add(String.format("%d12", fiscalYearShort));
            }
            // 优先级：Excel中的baseCode > 文件名推断 > plant_id推断
            String baseCodeVal = normalizeNumericLike(trimToNull(r.getBaseCode()));
            if ((baseCodeVal == null || baseCodeVal.isBlank()) && defaultBaseCode != null && !defaultBaseCode.isBlank()) {
                baseCodeVal = defaultBaseCode;
            }
            // 如果 baseCode 仍为空，根据 plant_id 推断
            if (baseCodeVal == null || baseCodeVal.isBlank()) {
                baseCodeVal = inferBaseCodeFromPlantId(plantId);
            }
            SupplyVolume e = new SupplyVolume();
            e.setCreateDate(LocalDateTime.now());
            e.setDataSource("Excel导入");
            e.setDataVer(LocalDate.now());
            e.setEtlCreateDate(LocalDateTime.now());
            e.setFiscalYear(fiscalYearShort);
            e.setBaseCode(trunc(baseCodeVal, MAX_LEN_32));
            e.setPlantId(trunc(plantId, MAX_LEN_32));
            e.setSupplierCode(trunc(supplierCode, MAX_LEN_32));
            e.setSupplierName(trunc(r.getSupplierName(), MAX_LEN_128));
            e.setPartCode(trunc(partCode, MAX_LEN_64));
            e.setPartName(trunc(r.getPartName(), MAX_LEN_128));
            e.setMonth1No(m1 == null ? 0 : m1.intValue());
            e.setMonth2No(m2 == null ? 0 : m2.intValue());
            e.setMonth3No(m3 == null ? 0 : m3.intValue());
            e.setMonth4No(m4 == null ? 0 : m4.intValue());
            e.setMonth5No(m5 == null ? 0 : m5.intValue());
            e.setMonth6No(m6 == null ? 0 : m6.intValue());
            e.setMonth7No(m7 == null ? 0 : m7.intValue());
            e.setMonth8No(m8 == null ? 0 : m8.intValue());
            e.setMonth9No(m9 == null ? 0 : m9.intValue());
            e.setMonth10No(m10 == null ? 0 : m10.intValue());
            e.setMonth11No(m11 == null ? 0 : m11.intValue());
            e.setMonth12No(m12 == null ? 0 : m12.intValue());
            // 计算年度总供货量
            int total = (m1 == null ? 0 : m1.intValue()) + (m2 == null ? 0 : m2.intValue()) +
                    (m3 == null ? 0 : m3.intValue()) + (m4 == null ? 0 : m4.intValue()) +
                    (m5 == null ? 0 : m5.intValue()) + (m6 == null ? 0 : m6.intValue()) +
                    (m7 == null ? 0 : m7.intValue()) + (m8 == null ? 0 : m8.intValue()) +
                    (m9 == null ? 0 : m9.intValue()) + (m10 == null ? 0 : m10.intValue()) +
                    (m11 == null ? 0 : m11.intValue()) + (m12 == null ? 0 : m12.intValue());
            e.setTotalNo(total);
            e.setIfDocking("N");
            list.add(e);
        }
        // 将导入的月份添加到输出列表
        if (outPpmMonthsImported != null) {
            outPpmMonthsImported.addAll(importedMonths);
        }
        return supplyVolumeRepository.saveAll(list).size();
    }

    private void appendErrors(List<ImportErrorItem> target, List<ImportErrorItem> from, String sourceFile) {
        if (from == null) return;
        for (ImportErrorItem e : from) {
            target.add(new ImportErrorItem(e.getRow(), e.getField(), e.getMessage(), sourceFile));
        }
    }

    @FunctionalInterface
    interface InputStreamProvider {
        InputStream open() throws java.io.IOException;
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String trunc(String s, int max) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty()) return null;
        return t.length() <= max ? t : t.substring(0, max);
    }

    private static int n(Number v) { return v == null ? 0 : v.intValue(); }

    private static BigDecimal ppmVal(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v.setScale(2, RoundingMode.HALF_UP);
    }

    private static LocalDate toLocalDate(java.util.Date d) {
        if (d == null) return null;
        return Instant.ofEpochMilli(d.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private static String firstChar(String s) {
        if (s == null || s.isEmpty()) return null;
        char c = s.trim().charAt(0);
        return String.valueOf(c);
    }
}
