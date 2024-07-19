package com.example.exporter.service;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SchemaImportService {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public SchemaImportService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, List<String>> readTableAndColumnNames(MultipartFile file) throws IOException {
        Map<String, List<String>> tableColumnMap = new LinkedHashMap<>();

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            // Iteracja przez wszystkie arkusze
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);

                // Sprawdź, czy arkusz ma co najmniej dwa wiersze
                if (sheet.getPhysicalNumberOfRows() < 2) {
                    continue; // Przejdź do następnego arkusza
                }

                // Odczytaj drugi wiersz (indeks 1, bo indeksowanie zaczyna się od 0)
                Row row = sheet.getRow(1); // Zakładam, że drugi wiersz zawiera interesujące nas dane

                if (row == null) {
                    continue; // Przejdź do następnego arkusza, jeśli wiersz jest pusty
                }

                for (Cell cell : row) {
                    if (cell.getCellType() == CellType.STRING) {
                        String cellValue = cell.getStringCellValue();
                        String[] parts = cellValue.split("\\.");

                        if (parts.length == 2) {
                            String tableName = parts[0];
                            String columnName = parts[1];

                            tableColumnMap.computeIfAbsent(tableName, k -> new ArrayList<>()).add(columnName);
                        }
                    }
                }
            }
        }

        return tableColumnMap;
    }

    private List<String> readTableColumnNamesFromSheet(Sheet sheet) {
        List<String> tableColumnNames = new ArrayList<>();
        if (sheet != null) {
            Row row = sheet.getRow(1); // Drugi wiersz
            if (row != null) {
                int lastCellNum = row.getLastCellNum();
                for (int i = 0; i < lastCellNum; i++) {
                    Cell cell = row.getCell(i);
                    if (cell != null) {
                        tableColumnNames.add(cell.getStringCellValue().trim());
                    }
                }
            }
        }
        return tableColumnNames;
    }
    // Zapełnianie danych do pliku Excel
    public void exportFilledSchemaToExcel(Map<String, List<String>> tableColumnMap, HttpServletResponse response, String taskId) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            for (Map.Entry<String, List<String>> entry : tableColumnMap.entrySet()) {
                String tableName = entry.getKey();
                List<String> columnNames = entry.getValue();

                Sheet sheet = workbook.createSheet(tableName);
                Row headerRow = sheet.createRow(0);

                for (int i = 0; i < columnNames.size(); i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(columnNames.get(i));
                }
            }

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=" + "export-" + taskId + ".xlsx");

            try (OutputStream os = response.getOutputStream()) {
                workbook.write(os);
            }
        }
    }

    // Pobieranie danych tabeli z bazy danych
    private List<Map<String, Object>> getTableData(String tableName) {
        String sql = "SELECT * FROM " + tableName;
        return jdbcTemplate.queryForList(sql);
    }

    // Zapis danych do arkusza
    private void writeSchemaToSheet(List<String> tableColumns, List<Map<String, Object>> tableData, Sheet sheet) {
        // Dodanie nagłówków
        Row headerRow = sheet.createRow(0);
        int cellIdx = 0;
        for (String columnName : tableColumns) {
            Cell cell = headerRow.createCell(cellIdx++);
            cell.setCellValue(columnName);
        }

        // Dodanie danych
        int rowIdx = 1;
        for (Map<String, Object> rowData : tableData) {
            Row row = sheet.createRow(rowIdx++);
            cellIdx = 0;
            for (String columnName : tableColumns) {
                Cell cell = row.createCell(cellIdx++);
                Object value = rowData.get(columnName); // Pobranie wartości na podstawie kolumny
                setCellValueAndStyle(cell, value);
            }
        }

        // Automatyczne dostosowanie szerokości kolumn
        for (int i = 0; i < tableColumns.size(); i++) {
            sheet.autoSizeColumn(i);
        }
    }

    // Metoda do ustawiania wartości komórki i stylu
    private void setCellValueAndStyle(Cell cell, Object value) {
        if (value instanceof String) {
            cell.setCellValue((String) value);
        } else if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else if (value instanceof java.util.Date) {
            cell.setCellValue((java.util.Date) value);
            CellStyle dateStyle = cell.getSheet().getWorkbook().createCellStyle();
            dateStyle.setDataFormat(cell.getSheet().getWorkbook().createDataFormat().getFormat("yyyy-MM-dd"));
            cell.setCellStyle(dateStyle);
        } else {
            cell.setCellValue(value != null ? value.toString() : "");
        }
    }



    public void createAndFillExcelFile(Map<String, List<String>> tableColumnMap, Map<String, List<Map<String, Object>>> dataMap, HttpServletResponse response) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            for (Map.Entry<String, List<String>> entry : tableColumnMap.entrySet()) {
                String tableName = entry.getKey();
                List<String> columns = entry.getValue();
                List<Map<String, Object>> rows = dataMap.get(tableName);

                // Create a sheet for the table
                Sheet sheet = workbook.createSheet(tableName);

                // Create header row
                Row headerRow = sheet.createRow(0);
                for (int i = 0; i < columns.size(); i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(columns.get(i));
                }

                // Create data rows
                int rowNum = 1;
                for (Map<String, Object> rowData : rows) {
                    Row row = sheet.createRow(rowNum++);
                    for (int colNum = 0; colNum < columns.size(); colNum++) {
                        Cell cell = row.createCell(colNum);
                        Object value = rowData.get(columns.get(colNum));
                        if (value != null) {
                            cell.setCellValue(value.toString());
                        }
                    }
                }
            }

            // Write the workbook to the HTTP response
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=\"export.xlsx\"");
            workbook.write(response.getOutputStream());
        }
    }
}
