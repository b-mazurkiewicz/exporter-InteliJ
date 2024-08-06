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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class SchemaImportService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final List<List<String>> firstRows = new ArrayList<>();
    private final List<String> sheetNames = new ArrayList<>(); // Lista nazw arkuszy
    private final Map<String, List<SortColumn>> sortingMap = new HashMap<>(); // Mapa do sortowania
    private final Map<String, List<String>> columnMap = new LinkedHashMap<>(); // Mapa kolumn dla tabel

    public Map<String, List<String>> readTableAndColumnNames(MultipartFile file) throws IOException {
        Map<String, List<String>> tableColumnMap = new LinkedHashMap<>();
        firstRows.clear();
        sheetNames.clear(); // Czyść listę nazw arkuszy przed rozpoczęciem nowego importu
        sortingMap.clear(); // Czyść mapę sortowania przed rozpoczęciem nowego importu
        columnMap.clear(); // Czyść mapę kolumn przed rozpoczęciem nowego importu

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            // Iteracja przez wszystkie arkusze
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                sheetNames.add(sheet.getSheetName()); // Dodaj nazwę arkusza do listy

                // Sprawdź, czy arkusz ma co najmniej trzy wiersze
                if (sheet.getPhysicalNumberOfRows() < 3) {
                    continue; // Przejdź do następnego arkusza
                }

                List<String> firstRow = new ArrayList<>();
                firstRows.add(firstRow);

                // Odczytaj pierwszy, drugi i trzeci wiersz
                Row headerRow = sheet.getRow(0); // W pierwszym wierszu znajdują się nazwy, które użytkownik chce przepisać
                Row dataRow = sheet.getRow(1); // W drugim wierszu znajduje się lokalizacja danych w bazie
                Row sortingRow = sheet.getRow(2); // W trzecim wierszu znajdują się informacje potrzebne do sortowania danych

                if (dataRow == null || headerRow == null || sortingRow == null) {
                    continue; // Przejdź do następnego arkusza, jeśli wiersz jest pusty
                }

                // dataRow
                List<String> columns = new ArrayList<>();
                for (Cell cell : dataRow) {
                    if (cell.getCellType() == CellType.STRING) {
                        String cellValue = cell.getStringCellValue();
                        String[] parts = cellValue.split("\\.");

                        if (parts.length == 2) {
                            String tableName = parts[0];
                            String columnName = parts[1];

                            tableColumnMap.computeIfAbsent(tableName, k -> new ArrayList<>()).add(columnName);
                            columns.add(columnName); // Dodaj kolumny do mapy
                        }
                    }
                }
                columnMap.put(sheet.getSheetName(), columns); // Zapisz kolumny dla arkusza

                // headerRow
                for (Cell cell : headerRow) {
                    if (cell.getCellType() == CellType.STRING) {
                        String cellValue = cell.getStringCellValue();
                        firstRow.add(cellValue);
                    }
                }

                // sortingRow
                List<SortColumn> sortColumns = new ArrayList<>();
                for (int colNum = 0; colNum < sortingRow.getLastCellNum(); colNum++) {
                    Cell cell = sortingRow.getCell(colNum);
                    if (cell != null && cell.getCellType() == CellType.STRING) {
                        String cellValue = cell.getStringCellValue();
                        // Parse sorting information, e.g., "1asc" or "2desc"
                        if (cellValue.matches("\\d+[a-zA-Z]{2,4}")) {
                            int priority = Integer.parseInt(cellValue.replaceAll("[^0-9]", ""));
                            String sortDirection = cellValue.replaceAll("\\d+", "");
                            sortColumns.add(new SortColumn(priority, colNum, sortDirection));
                        }
                    }
                }

                // Sort columns by priority
                sortColumns.sort(Comparator.comparingInt(SortColumn::getPriority));
                sortingMap.put(sheet.getSheetName(), sortColumns); // Zapisz informacje o sortowaniu dla danego arkusza
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

                // Uzyskaj informacje o sortowaniu dla bieżącego arkusza
                List<SortColumn> sortColumns = sortingMap.get(sheetName);
                List<String> columnNames = columnMap.get(sheetName); // Pobierz kolumny dla arkusza
                if (sortColumns != null && !sortColumns.isEmpty() && columnNames != null) {
                    // Sortuj wiersze na podstawie informacji o sortowaniu
                    rows.sort((row1, row2) -> {
                        for (SortColumn sortColumn : sortColumns) {
                            int colIndex = sortColumn.getColumnIndex();
                            if (colIndex < columnNames.size()) {
                                String columnName = columnNames.get(colIndex);
                                Object value1 = row1.get(columnName);
                                Object value2 = row2.get(columnName);

                                int comparison = compareValues(value1, value2);
                                if (comparison != 0) {
                                    return sortColumn.getDirection().equalsIgnoreCase("desc") ? -comparison : comparison;
                                }
                            }
                        }
                        return 0;
                    });
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
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            //response.setHeader("Content-Disposition", "attachment; filename=\"export.xlsx\"");
            workbook.write(response.getOutputStream());
        }
    }

    // Ustaw wartość komórki i styl na podstawie typu danych
    public void setCellValueAndStyle(Cell cell, Object value, XSSFWorkbook workbook) {
        if (value != null) {
            CellStyle cellStyle = workbook.createCellStyle();
            DataFormat dataFormat = workbook.createDataFormat();

            if (value instanceof String) {
                Date dateValue = parseDate((String) value);
                if (dateValue != null) {
                    cell.setCellValue(dateValue);
                    cellStyle.setDataFormat(dataFormat.getFormat("dd.MM.yyyy")); // Format daty w Excelu
                } else {
                    cell.setCellValue((String) value);
                    cellStyle.setDataFormat(dataFormat.getFormat("@")); // Format tekstowy
                }
            } else if (value instanceof Integer || value instanceof Long) {
                cell.setCellValue(((Number) value).doubleValue());
                cellStyle.setDataFormat(dataFormat.getFormat("0")); // Format liczbowy całkowity
            } else if (value instanceof Double) {
                cell.setCellValue((Double) value);
                cellStyle.setDataFormat(dataFormat.getFormat("#,##0.################")); // Format liczbowy
            } else if (value instanceof java.util.Date) {
                cell.setCellValue((java.util.Date) value);
                cellStyle.setDataFormat(dataFormat.getFormat("dd.MM.yyyy")); // Format daty
            } else {
                cell.setCellValue(value.toString());
                cellStyle.setDataFormat(dataFormat.getFormat("@")); // Format tekstowy
            }

            cell.setCellStyle(cellStyle);
        } else {
            cell.setCellValue("");
        }
    }

    // Próbuj analizować datę w różnych formatach
    private Date parseDate(String dateString) {
        String[] formats = {
                "yyyy-MM-dd", "MM/dd/yyyy", "dd/MM/yyyy", "yyyy/MM/dd",
                "yyyy-MM-dd HH:mm:ss", "MM/dd/yyyy HH:mm:ss", "dd/MM/yyyy HH:mm:ss",
                "dd-MM-yyyy", "dd.MM.yyyy" // Dodaj formaty, które mogą być używane
        };

        for (String format : formats) {
            try {
                return new SimpleDateFormat(format, Locale.ENGLISH).parse(dateString);
            } catch (ParseException ignored) {
                // Ignoruj wyjątek, próbuj następnego formatu
            }
        }
        return null;
    }


    // Porównaj dwie wartości
    private int compareValues(Object value1, Object value2) {
        if (value1 == null && value2 == null) return 0;
        if (value1 == null) return -1;
        if (value2 == null) return 1;
        if (value1 instanceof Comparable && value2 instanceof Comparable) {
            @SuppressWarnings("unchecked")
            Comparable<Object> comp1 = (Comparable<Object>) value1;
            @SuppressWarnings("unchecked")
            Comparable<Object> comp2 = (Comparable<Object>) value2;
            return comp1.compareTo(comp2);
        }
        return value1.toString().compareTo(value2.toString());
    }

    // Klasa do przechowywania informacji o sortowaniu
    private static class SortColumn {
        private final int priority;
        private final int columnIndex;
        private final String direction;

        public SortColumn(int priority, int columnIndex, String direction) {
            this.priority = priority;
            this.columnIndex = columnIndex;
            this.direction = direction;
        }

        public int getPriority() {
            return priority;
        }

        public int getColumnIndex() {
            return columnIndex;
        }

        public String getDirection() {
            return direction;
        }
    }
}
