package com.example.exporter.service;

import com.example.exporter.model.ExportTask;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
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

    /*
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
    }*/




    private void writeDataToSheet(List<Map<String, Object>> data, Sheet sheet) {
        if (!data.isEmpty()) {
            Map<String, Object> firstRow = data.get(0);
            
            // Utworzenie stylu dla nagłówków
            Workbook workbook = sheet.getWorkbook();
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerFont.setColor(IndexedColors.BLACK.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            // Utworzenie stylów dla różnych typów danych
            CellStyle stringStyle = workbook.createCellStyle();
            stringStyle.setDataFormat(workbook.createDataFormat().getFormat("@"));

            CellStyle integerStyle = workbook.createCellStyle();
            integerStyle.setDataFormat(workbook.createDataFormat().getFormat("0"));

            CellStyle doubleStyle = workbook.createCellStyle();
            doubleStyle.setDataFormat(workbook.createDataFormat().getFormat("0.00"));

            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(workbook.createDataFormat().getFormat("yyyy-MM-dd"));

            // Nazwy kolumn (zerowy wiersz)
            Row columnNameRow = sheet.createRow(0);
            int cellIdx = 0;
            for (String column : firstRow.keySet()) {
                Cell cell = columnNameRow.createCell(cellIdx++);
                cell.setCellValue(column);
                cell.setCellStyle(headerStyle);
            }

            // Źródło danych (pierwszy wiersz)
            Row dataSourceRow = sheet.createRow(1);
            cellIdx = 0;
            for (String column : firstRow.keySet()) {
                Cell cell = dataSourceRow.createCell(cellIdx++);
                cell.setCellValue("TableName." + column);
                cell.setCellStyle(headerStyle);
            }

            // Typ danych (drugi wiersz)
            Row dataTypeRow = sheet.createRow(2);
            cellIdx = 0;
            for (Object value : firstRow.values()) {
                Cell cell = dataTypeRow.createCell(cellIdx++);
                String dataType = value != null ? value.getClass().getSimpleName() : "Unknown";
                cell.setCellValue(dataType);
                cell.setCellStyle(headerStyle);
            }

            // Dane (od trzeciego wiersza)
            int rowIdx = 3;
            for (Map<String, Object> rowData : data) {
                Row row = sheet.createRow(rowIdx++);
                cellIdx = 0;
                for (Object value : rowData.values()) {
                    Cell cell = row.createCell(cellIdx++);
                    if (value != null) {
                        if (value instanceof String) {
                            cell.setCellValue((String) value);
                            cell.setCellStyle(stringStyle);
                        } else if (value instanceof Integer) {
                            cell.setCellValue((Integer) value);
                            cell.setCellStyle(integerStyle);
                        } else if (value instanceof Long) {
                            cell.setCellValue((Long) value);
                            cell.setCellStyle(integerStyle);
                        } else if (value instanceof Double) {
                            cell.setCellValue((Double) value);
                            cell.setCellStyle(doubleStyle);
                        } else if (value instanceof java.util.Date) {
                            cell.setCellValue((java.util.Date) value);
                            cell.setCellStyle(dateStyle);
                        } else {
                            cell.setCellValue(value.toString());
                        }
                    } else {
                        cell.setCellValue("");
                    }
                }
            }

            // Automatyczne dostosowanie szerokości kolumn
            for (int i = 0; i < firstRow.size(); i++) {
                sheet.autoSizeColumn(i);
            }
        }
    }



/*
    // Metoda do zapisu danych do arkusza
    private void writeDataToSheet(List<Map<String, Object>> data, Sheet sheet) {
        if (!data.isEmpty()) {
            Map<String, Object> firstRow = data.get(0);

            // Utworzenie stylu dla nagłówków
            Workbook workbook = sheet.getWorkbook();
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            // Nazwy kolumn (zerowy wiersz)
            Row columnNameRow = sheet.createRow(0);
            int cellIdx = 0;
            for (String column : firstRow.keySet()) {
                Cell cell = columnNameRow.createCell(cellIdx++);
                cell.setCellValue(column);
                cell.setCellStyle(headerStyle);
            }

            // Źródło danych (pierwszy wiersz)
            Row dataSourceRow = sheet.createRow(1);
            cellIdx = 0;
            for (String column : firstRow.keySet()) {
                Cell cell = dataSourceRow.createCell(cellIdx++);
                cell.setCellValue("TableName." + column);
                cell.setCellStyle(headerStyle);
            }

            // Typ danych (drugi wiersz)
            Row dataTypeRow = sheet.createRow(2);
            cellIdx = 0;
            for (Object value : firstRow.values()) {
                Cell cell = dataTypeRow.createCell(cellIdx++);
                cell.setCellValue(value != null ? value.getClass().getSimpleName() : "Unknown");
                cell.setCellStyle(headerStyle);
            }

            // Dane (od trzeciego wiersza)
            int rowIdx = 3;
            for (Map<String, Object> rowData : data) {
                Row row = sheet.createRow(rowIdx++);
                cellIdx = 0;
                for (Object value : rowData.values()) {
                    row.createCell(cellIdx++).setCellValue(value != null ? value.toString() : "");
                }
            }

            // Automatyczne dostosowanie szerokości kolumn
            for (int i = 0; i < firstRow.size(); i++) {
                sheet.autoSizeColumn(i);
            }
        }
    }

 */
}

