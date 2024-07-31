package com.example.exporter.service;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.logging.Logger;

@Service
public class SchemaImportService {

    private final List<List<String>> firstRows = new ArrayList<>();
    private final List<String> sheetNames = new ArrayList<>(); // Lista nazw arkuszy

    public Map<String, List<String>> readTableAndColumnNames(MultipartFile file) throws IOException {
        boolean isSorted = false;

        Map<String, List<String>> tableColumnMap = new LinkedHashMap<>();
        TreeMap<Integer, String> sortedMap = new TreeMap<>();

        firstRows.clear();
        sheetNames.clear(); // Czyść listę nazw arkuszy przed rozpoczęciem nowego importu

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            // Iteracja przez wszystkie arkusze
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                sheetNames.add(sheet.getSheetName()); // Dodaj nazwę arkusza do listy

                // Sprawdź, czy arkusz ma co najmniej dwa wiersze
                if (sheet.getPhysicalNumberOfRows() < 2) {
                    continue; // Przejdź do następnego arkusza
                }

                List<String> firstRow = new ArrayList<>();
                firstRows.add(firstRow);

                // Odczytaj pierwszy i drugi wiersz
                Row headerRow = sheet.getRow(0); // W pierwszym wierszu znajdują się nazwy, które użytkownik chce przepisać
                Row dataRow = sheet.getRow(1); // W drugim wierszu znajduje się lokalizacja danych w bazie
                Row sortingRow = sheet.getRow(2); // W trzecim wierszu znajdują się informacje potrzebne do sortowania danych

                if (dataRow == null || headerRow == null || sortingRow == null) {
                    continue; // Przejdź do następnego arkusza, jeśli wiersz jest pusty
                }

                // dataRow
                for (Cell cell : dataRow) {
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

                // headerRow
                for (Cell cell : headerRow) {
                    if (cell.getCellType() == CellType.STRING) {
                        String cellValue = cell.getStringCellValue();
                        firstRow.add(cellValue);
                    }
                }

                //sortingRow
                for (int colIndex = 0; colIndex < sortingRow.getLastCellNum(); colIndex++) {
                    Cell sortingCell = sortingRow.getCell(colIndex);
                    Cell dataCell = dataRow.getCell(colIndex);

                    if (sortingCell != null && sortingCell.getCellType() == CellType.NUMERIC) {
                        int cellKey = (int) sortingCell.getNumericCellValue();
                        String cellValue = dataCell != null ? dataCell.getStringCellValue() : "";

                        sortedMap.put(cellKey, cellValue);
                        isSorted = true;
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
            int sheetIndex = 0;

            for (Map.Entry<String, List<String>> entry : tableColumnMap.entrySet()) {
                String tableName = entry.getKey();
                List<String> columns = entry.getValue();
                List<Map<String, Object>> rows = dataMap.get(tableName);

                // Użyj nazwy arkusza z oryginalnego pliku, zamiast nazwy tabeli
                String sheetName = sheetNames.get(sheetIndex);

                // Utwórz arkusz dla tabeli
                Sheet sheet = workbook.createSheet(sheetName);

                // pobieranie odpowiedniej listy dla pierwszego wiersza dla tego arkusza
                List<String> firstRow;
                if (sheetIndex < firstRows.size()) {
                    firstRow = firstRows.get(sheetIndex);
                } else {
                    firstRow = new ArrayList<>(); //jeśli nie znalazł to po prostu tworzy pustą listę żeby nie wyrzuciło błędu
                }
                sheetIndex++;

                // Utwórz wiersz nagłówka z wartościami z pierwszego wiersza
                Row headerRow = sheet.createRow(0);
                for (int i = 0; i < firstRow.size(); i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(firstRow.get(i));
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
                for (int i = 0; i < firstRow.size(); i++) {
                    sheet.autoSizeColumn(i);
                    // Dodaj margines do szerokości kolumny
                    int columnWidth = sheet.getColumnWidth(i);
                    sheet.setColumnWidth(i, columnWidth + 2000); // Dodaj 2000 jednostek marginesu
                }

                // Ustaw minimalną wysokość wiersza (opcjonalne)
                for (int i = 0; i < rowNum; i++) {
                    sheet.getRow(i).setHeightInPoints(20); // Dostosuj wysokość do żądanego rozmiaru
                }
            }

            // Zapisz arkusz do odpowiedzi HTTP
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
