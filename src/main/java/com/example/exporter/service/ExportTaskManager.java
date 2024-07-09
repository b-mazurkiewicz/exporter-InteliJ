package com.example.exporter.service;

import com.example.exporter.model.ExportTask;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.servlet.ServletContext;
import org.springframework.web.context.WebApplicationContext;

@Service
public class ExportTaskManager {

    private final Map<String, ExportTask> tasks = new ConcurrentHashMap<>();
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public ExportTaskManager(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Metoda do dodawania nowego zadania eksportu do mapy zadań
    public void addTask(ExportTask task) {
        tasks.put(task.getTaskId(), task);
    }

    // Metoda do pobierania zadania eksportu na podstawie ID zadania
    public ExportTask getTask(String taskId) {
        return tasks.get(taskId);
    }

    // Metoda do pobierania wszystkich tabel i widoków z bazy danych
    public List<String> getAllTablesAndViews() {
        String sql = "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA='PUBLIC'";
        return jdbcTemplate.queryForList(sql, String.class);
    }

    // Metoda do pobierania danych z podanej tabeli
    public List<Map<String, Object>> getTableData(String tableName) {
        String sql = "SELECT * FROM " + tableName;
        return jdbcTemplate.queryForList(sql);
    }

    // Metoda do pobierania danych z listy tabel
    public List<Map<String, Object>> getTableData(List<String> tableNames) {
        List<Map<String, Object>> allData = new ArrayList<>();
        for (String tableName : tableNames) {
            allData.addAll(getTableData(tableName));
        }
        return allData;
    }

    // Metoda do eksportowania danych do pliku Excel
    public void exportToExcel(List<List<Map<String, Object>>> data, HttpServletResponse response, String taskId) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            int sheetNum = 1;
            for (List<Map<String, Object>> tableData : data) {
                String sheetName = "Data" + sheetNum++;
                Sheet sheet = workbook.createSheet(sheetName);
                writeDataToSheet(tableData, sheet);
            }

            // Ustawienia nagłówka i typu MIME w odpowiedzi HTTP
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            String headerKey = "Content-Disposition";
            String headerValue = "attachment; filename=Export_" + taskId + ".xlsx";
            response.setHeader(headerKey, headerValue);

            // Zapisz workbook do odpowiedzi HTTP
            try (OutputStream outputStream = response.getOutputStream()) {
                workbook.write(outputStream);
            }
        }
    }

    private void writeDataToSheet(List<Map<String, Object>> data, Sheet sheet) {
        if (!data.isEmpty()) {
            Map<String, Object> firstRow = data.get(0);
            Row headerRow = sheet.createRow(0);
            int cellIdx = 0;
            for (String column : firstRow.keySet()) {
                headerRow.createCell(cellIdx++).setCellValue(column);
            }

            int rowIdx = 1;
            for (Map<String, Object> rowData : data) {
                Row row = sheet.createRow(rowIdx++);
                cellIdx = 0;
                for (Object value : rowData.values()) {
                    row.createCell(cellIdx++).setCellValue(value != null ? value.toString() : "");
                }
            }

            for (int i = 0; i < firstRow.size(); i++) {
                sheet.autoSizeColumn(i);
            }
        }
    }
}