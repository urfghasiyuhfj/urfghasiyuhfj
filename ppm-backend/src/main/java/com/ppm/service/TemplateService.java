package com.ppm.service;

import com.alibaba.excel.EasyExcel;
import com.ppm.dto.excel.SupplierPpmExcelRow;
import com.ppm.dto.excel.SupplyVolumeExcelRow;
import com.ppm.dto.excel.SuspiciousMaterialExcelRow;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

/**
 * Excel 导入模板下载服务：返回仅含表头的空 Excel。
 */
@Service
@RequiredArgsConstructor
public class TemplateService {

    public void downloadTemplate(String type, HttpServletResponse response) throws IOException {
        String fileName;
        switch (type) {
            case "supplier-ppm" -> {
                fileName = "供应商PPM导入模板.xlsx";
                writeTemplate(response, fileName, SupplierPpmExcelRow.class, Collections.emptyList());
            }
            case "suspicious-material" -> {
                fileName = "可疑物料统计导入模板.xlsx";
                writeTemplate(response, fileName, SuspiciousMaterialExcelRow.class, Collections.emptyList());
            }
            case "supply-volume" -> {
                fileName = "供货量导入模板.xlsx";
                writeTemplate(response, fileName, SupplyVolumeExcelRow.class, Collections.emptyList());
            }
            default -> throw new IllegalArgumentException("未知模板类型: " + type);
        }
    }

    private <T> void writeTemplate(HttpServletResponse response, String fileName, Class<T> clazz, List<T> rows)
            throws IOException {
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encoded);
        EasyExcel.write(response.getOutputStream(), clazz).sheet("Sheet1").doWrite(rows);
    }
}
