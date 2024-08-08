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
import java.util.*;

@Service
public class SchemaImportService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Lista list, która przechowuje wartości pierwszego wiersza z każdego arkusza
    private final List<List<String>> firstRows = new ArrayList<>();

    // Lista nazw arkuszy z pliku
    private final List<String> sheetNames = new ArrayList<>();

    // Mapa przechowująca informacje o kolumnach do sortowania w każdym arkuszu
    private final Map<String, List<SortColumn>> sortingMap = new HashMap<>();

    // Mapa przechowująca kolumny dla każdej tabeli w bazie danych
    private final Map<String, List<String>> columnMap = new LinkedHashMap<>();

    // Metoda odczytująca nazwy tabel i kolumn z załadowanego pliku Excel
    public Map<String, List<String>> readTableAndColumnNames(MultipartFile file) throws IOException {
        Map<String, List<String>> tableColumnMap = new LinkedHashMap<>();
        firstRows.clear(); // Wyczyść listę przed rozpoczęciem nowego importu
        sheetNames.clear(); // Wyczyść listę nazw arkuszy
        sortingMap.clear(); // Wyczyść mapę sortowania
        columnMap.clear(); // Wyczyść mapę kolumn

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

                // Przetwarzanie drugiego wiersza (dataRow) w celu odczytu nazw tabel i kolumn
                List<String> columns = new ArrayList<>();
                for (Cell cell : dataRow) {
                    if (cell.getCellType() == CellType.STRING) {
                        String cellValue = cell.getStringCellValue();
                        String[] parts = cellValue.split("\\.");

                        // Sprawdzenie, czy komórka zawiera nazwę tabeli i kolumny
                        if (parts.length == 2) {
                            String tableName = parts[0];
                            String columnName = parts[1];
                            // Dodanie kolumny do mapy, jeśli jeszcze nie istnieje
                            tableColumnMap.computeIfAbsent(tableName, k -> new ArrayList<>()).add(columnName);
                            columns.add(columnName); // Dodaj kolumny do mapy
                        }
                    }
                }
                columnMap.put(sheet.getSheetName(), columns); // Zapisz kolumny dla arkusza

                // Przetwarzanie pierwszego wiersza (headerRow) w celu odczytu nazw kolumn
                for (Cell cell : headerRow) {
                    if (cell.getCellType() == CellType.STRING) {
                        String cellValue = cell.getStringCellValue();
                        firstRow.add(cellValue);
                    }
                }

                // Przetwarzanie trzeciego wiersza (sortingRow) w celu odczytu informacji o sortowaniu
                List<SortColumn> sortColumns = new ArrayList<>();
                for (int colNum = 0; colNum < sortingRow.getLastCellNum(); colNum++) {
                    Cell cell = sortingRow.getCell(colNum);
                    if (cell != null && cell.getCellType() == CellType.STRING) {
                        String cellValue = cell.getStringCellValue();

                        // Sprawdzenie, czy wartość w komórce pasuje do wzoru (np. 1asc, 2desc)
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

    // Metoda tworząca i wypełniająca plik Excel na podstawie danych i kolumn z mapy
    public void createAndFillExcelFile(Map<String, List<String>> tableColumnMap,
                                       Map<String, List<Map<String, Object>>> dataMap,
                                       HttpServletResponse response) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            int sheetIndex = 0;

            // Iteracja przez wszystkie tabele i kolumny w mapie
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
                    firstRow = new ArrayList<>(); // Utworzenie pustej listy, jeśli brak danych
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

                                // Porównanie wartości w kolumnie
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
                    sheet.setColumnWidth(i, columnWidth + 2000); // Dodanie marginesu do szerokości kolumny
                     }

                // Ustaw minimalną wysokość wiersza (opcjonalne)
                for (int i = 0; i < rowNum; i++) {
                    sheet.getRow(i).setHeightInPoints(20); // Dostosuj wysokość do żądanego rozmiaru
                }
            }

            // Zapisz arkusz do odpowiedzi HTTP
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            workbook.write(response.getOutputStream());
        }
    }

    // Metoda ustawiająca wartość komórki i odpowiedni styl na podstawie typu danych
    private void setCellValueAndStyle(Cell cell, Object value, XSSFWorkbook workbook) {
        if (value != null) {
            CellStyle cellStyle = workbook.createCellStyle();
            if (value instanceof String) {
                cell.setCellValue((String) value);
                cellStyle.setDataFormat(workbook.createDataFormat().getFormat("@")); // Format tekstowy
            } else if (value instanceof Integer || value instanceof Long) {
                cell.setCellValue(((Number) value).doubleValue());
                cellStyle.setDataFormat(workbook.createDataFormat().getFormat("0")); // Format liczbowy
            } else if (value instanceof Double) {
                cell.setCellValue((Double) value);
                cellStyle.setDataFormat(workbook.createDataFormat().getFormat("0.00")); // Format liczbowy z miejscami po przecinku
            } else if (value instanceof java.util.Date) {
                cell.setCellValue((java.util.Date) value);
                cellStyle.setDataFormat(workbook.createDataFormat().getFormat("yyyy-MM-dd")); // Format daty
            } else {
                cell.setCellValue(value.toString());
            }
            cell.setCellStyle(cellStyle); // Ustawienie stylu komórki
        } else {
            cell.setCellValue(""); // Ustawienie pustej wartości dla null
        }
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
            return comp1.compareTo(comp2); // Porównanie wartości za pomocą Comparable
        }
        return value1.toString().compareTo(value2.toString()); // Porównanie wartości jako stringi, jeśli nie są Comparable
    }

    // Klasa wewnętrzna przechowująca informacje o kolumnach do sortowania
    private static class SortColumn {
        private final int priority; // Priorytet sortowania
        private final int columnIndex; // Indeks kolumny
        private final String direction; // Kierunek sortowania ("asc" lub "desc")

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
