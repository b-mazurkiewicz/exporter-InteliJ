package com.example.exporter.service;

import com.example.exporter.model.ExportTask;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
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
import org.springframework.web.multipart.MultipartFile;

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
    public List<List<Map<String, Object>>> getAllTableData(List<String> tableNames) {
        List<List<Map<String, Object>>> allData = new ArrayList<>();
        for (String tableName : tableNames) {
            List<Map<String, Object>> tableData = getTableData(tableName);
            allData.add(tableData);
        }
        return allData;
    }

    // Metoda do eksportowania danych do pliku Excel
    public void exportToExcel(List<String> tableNames, HttpServletResponse response, String taskId) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            for (String tableName : tableNames) {
                List<Map<String, Object>> tableData = getTableData(tableName);
                String sheetName = tableName;
                Sheet sheet = workbook.createSheet(sheetName);
                writeDataToSheet(tableData, tableName, sheet);
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

    // Metoda do zapisu danych do arkusza
    private void writeDataToSheet(List<Map<String, Object>> data, String tableName, Sheet sheet) {
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
                cell.setCellValue(column); // Użyj nazwy tabeli
                cell.setCellStyle(headerStyle);
            }

            // Źródło danych (pierwszy wiersz)
            Row dataSourceRow = sheet.createRow(1);
            cellIdx = 0;
            for (String column : firstRow.keySet()) {
                Cell cell = dataSourceRow.createCell(cellIdx++);
                cell.setCellValue(tableName + "." + column); // Użyj nazwy tabeli
                cell.setCellStyle(headerStyle);
            }

            // Typ danych (drugi wiersz)
            Row dataTypeRow = sheet.createRow(2);
            cellIdx = 0;
            for (Object value : firstRow.values()) {
                Cell cell = dataTypeRow.createCell(cellIdx++);
                String dataType = determineDataType(value);
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
                    setCellValueAndStyle(cell, value, stringStyle, integerStyle, doubleStyle, dateStyle);
                }
            }

            // Automatyczne dostosowanie szerokości kolumn
            for (int i = 0; i < firstRow.size(); i++) {
                sheet.autoSizeColumn(i);
            }
        }
    }

    // Metoda do określania typu danych
    private String determineDataType(Object value) {
        if (value instanceof String) {
            return "String";
        } else if (value instanceof Integer || value instanceof Long) {
            return "Integer";
        } else if (value instanceof Double) {
            return "Double";
        } else if (value instanceof java.util.Date) {
            return "Date";
        } else {
            return "Unknown";
        }
    }

    // Metoda do ustawiania wartości komórki i stylu
    private void setCellValueAndStyle(Cell cell, Object value, CellStyle stringStyle, CellStyle integerStyle,
                                      CellStyle doubleStyle, CellStyle dateStyle) {
        if (value != null) {
            if (value instanceof String) {
                cell.setCellValue((String) value);
                cell.setCellStyle(stringStyle);
            } else if (value instanceof Integer || value instanceof Long) {
                cell.setCellValue(((Number) value).doubleValue()); // Konwersja na Double, ponieważ Excel nie obsługuje bezpośrednio typów całkowitych
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

    // Metoda do odczytu nazw tabel i kolumn z przesłanego pliku Excel
    public List<List<String>> readTableAndColumnNames(MultipartFile file) throws IOException {
        List<List<String>> allTableColumnNames = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            int numberOfSheets = workbook.getNumberOfSheets();
            for (int i = 0; i < numberOfSheets; i++) {
                Sheet sheet = workbook.getSheetAt(i);
                List<String> tableColumnNames = readTableColumnNamesFromSheet(sheet);
                allTableColumnNames.add(tableColumnNames);
            }
        } catch (InvalidFormatException e) {
            throw new IOException("Invalid format of Excel file", e);
        } catch (IOException e) {
            throw new IOException("Failed to read Excel file", e);
        }
        return allTableColumnNames;
    }

    // Metoda do odczytu nazw tabel i kolumn z pojedynczego arkusza
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
}