package org.example.hrmanagement.common.excel;

import com.alibaba.excel.EasyExcel;
import jakarta.servlet.http.HttpServletResponse;
import org.example.hrmanagement.common.exception.BusinessException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class ExcelExportHelper {

    private ExcelExportHelper() {
    }

    public static <T> void write(
            HttpServletResponse response,
            String fileName,
            String sheetName,
            Class<T> headClass,
            List<T> rows) {
        try {
            String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encoded);
            EasyExcel.write(response.getOutputStream(), headClass)
                    .sheet(sheetName)
                    .doWrite(rows);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("导出 Excel 失败：" + e.getMessage());
        }
    }
}
