package com.example.exporter.service;

import com.example.exporter.model.ExportTask;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.servlet.ServletContext;
import org.springframework.web.context.WebApplicationContext;


@Service
public class ExportTaskManager {
    private final Map<String, ExportTask> tasks = new ConcurrentHashMap<>();

    public void addTask(ExportTask task) {
        tasks.put(task.getTaskId(), task);
    }

    public ExportTask getTask(String taskId) {
        return tasks.get(taskId);
    }

    public void updateTask(String taskId, String status, String filePath) {
        ExportTask task = tasks.get(taskId);
        if (task != null) {
            task.setStatus(status);
            task.setFilePath(filePath);
        }
    }

/////////////////////
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public ExportTaskManager(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void exportToExcel(String tableName, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String headerKey = "Content-Disposition";
        String headerValue = "attachment; filename=" + tableName + ".xlsx";
        response.setHeader(headerKey, headerValue);

        String sql = "SELECT * FROM " + tableName;
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);

        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet(tableName);

        // Create header row
        XSSFRow headerRow = sheet.createRow(0);
        Set<String> columns = rows.get(0).keySet();
        int colIdx = 0;
        for (String column : columns) {
            headerRow.createCell(colIdx++).setCellValue(column);
        }

        // Fill data rows
        int rowIdx = 1;
        for (Map<String, Object> row : rows) {
            XSSFRow dataRow = sheet.createRow(rowIdx++);
            colIdx = 0;
            for (String column : columns) {
                dataRow.createCell(colIdx++).setCellValue(row.get(column).toString());
            }
        }

        for (int i = 0; i < columns.size(); i++) {
            sheet.autoSizeColumn(i);
        }

        workbook.write(response.getOutputStream());
        workbook.close();
    }
}
