package com.example.exporter.service;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SchemaImportService {


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

    public void createAndFillExcelFile(Map<String, List<String>> tableColumnMap,
                                       Map<String, List<Map<String, Object>>> dataMap,
                                       HttpServletResponse response) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            for (Map.Entry<String, List<String>> entry : tableColumnMap.entrySet()) {
                String tableName = entry.getKey();
                List<String> columns = entry.getValue();
                List<Map<String, Object>> rows = dataMap.get(tableName);

                // Utwórz arkusz dla tabeli
                Sheet sheet = workbook.createSheet(tableName);

                // Utwórz wiersz nagłówka
                Row headerRow = sheet.createRow(0);
                for (int i = 0; i < columns.size(); i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(columns.get(i));
                }

                // Utwórz wiersze danych
                int rowNum = 1;
                for (Map<String, Object> rowData : rows) {
                    Row row = sheet.createRow(rowNum++);
                    for (int colNum = 0; colNum < columns.size(); colNum++) {
                        Cell cell = row.createCell(colNum);
                        Object value = rowData.get(columns.get(colNum));
                        setCellValueAndStyle(cell, value, workbook);
                    }
                }

                // Automatyczne dostosowanie szerokości kolumn z dodatkowym marginesem
                for (int i = 0; i < columns.size(); i++) {
                    sheet.autoSizeColumn(i);
                    // Dodaj margines do szerokości kolumny
                    int columnWidth = sheet.getColumnWidth(i);
                    sheet.setColumnWidth(i, columnWidth + 2000); // Add 2000 units for padding
                }

                // Ustaw minimalną wysokość wiersza (opcjonalne)
                for (int i = 0; i < rowNum; i++) {
                    sheet.getRow(i).setHeightInPoints(20); // Adjust height to desired size
                }
            }

            // Zapisz arkusz do odpowiedzi HTTP
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=\"export.xlsx\"");
            workbook.write(response.getOutputStream());
        }
    }

    // Ustaw wartość komórki i styl na podstawie typu danych
    private void setCellValueAndStyle(Cell cell, Object value, XSSFWorkbook workbook) {
        if (value != null) {
            CellStyle cellStyle = workbook.createCellStyle();
            if (value instanceof String) {
                cell.setCellValue((String) value);
                cellStyle.setDataFormat(workbook.createDataFormat().getFormat("@"));
            } else if (value instanceof Integer || value instanceof Long) {
                cell.setCellValue(((Number) value).doubleValue());
                cellStyle.setDataFormat(workbook.createDataFormat().getFormat("0"));
            } else if (value instanceof Double) {
                cell.setCellValue((Double) value);
                cellStyle.setDataFormat(workbook.createDataFormat().getFormat("0.00"));
            } else if (value instanceof java.util.Date) {
                cell.setCellValue((java.util.Date) value);
                cellStyle.setDataFormat(workbook.createDataFormat().getFormat("yyyy-MM-dd"));
            } else {
                cell.setCellValue(value.toString());
            }
            cell.setCellStyle(cellStyle);
        } else {
            cell.setCellValue("");
        }
    }
}