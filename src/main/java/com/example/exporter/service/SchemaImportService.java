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

    // Mapa przechowująca wartości filtrów dla każdego arkusza
    private final Map<String, Map<String, String>> filterMap = new HashMap<>();

    // Metoda odczytująca nazwy tabel i kolumn z załadowanego pliku Excel
    public Map<String, List<String>> readTableAndColumnNames(MultipartFile file) throws IOException {
        Map<String, List<String>> tableColumnMap = new LinkedHashMap<>();
        firstRows.clear(); // Wyczyść listę przed rozpoczęciem nowego importu
        sheetNames.clear(); // Wyczyść listę nazw arkuszy
        sortingMap.clear(); // Wyczyść mapę sortowania
        columnMap.clear(); // Wyczyść mapę kolumn
        filterMap.clear(); // Wyczyść mapę filtrów

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            // Iteracja przez wszystkie arkusze
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                sheetNames.add(sheet.getSheetName()); // Dodaj nazwę arkusza do listy

                // Sprawdź, czy arkusz ma co najmniej dwa wiersze
                if (sheet.getPhysicalNumberOfRows() < 2) {
                    continue; // Przejdź do następnego arkusza, jeśli jest zbyt mało wierszy
                }

                List<String> firstRow = new ArrayList<>();
                firstRows.add(firstRow);

                // Odczytaj wiersze
                Row headerRow = sheet.getRow(0);
                Row dataRow = sheet.getRow(1);
                Row sortingRow = sheet.getRow(2);
                Row filterRow = sheet.getRow(3);

                // Sprawdź, czy wiersze nagłówka i danych są obecne
                if (dataRow == null || headerRow == null) {
                    continue; // Przejdź do następnego arkusza, jeśli dwa pierwsze wiersze są puste
                }

                // Przetwarzanie drugiego wiersza (dataRow) w celu odczytu nazw tabel i kolumn
                List<String> columns = new ArrayList<>();
                Map<String, String> filters = new HashMap<>();
                for (int colNum = 0; colNum < dataRow.getLastCellNum(); colNum++) {
                    Cell cell = dataRow.getCell(colNum);
                    if (cell != null && cell.getCellType() == CellType.STRING) {
                        String cellValue = cell.getStringCellValue();
                        String[] parts = cellValue.split("\\.");

                        if (parts.length == 2) {
                            String tableName = parts[0];
                            String columnName = parts[1];
                            tableColumnMap.computeIfAbsent(tableName, k -> new ArrayList<>()).add(columnName);
                            columns.add(columnName);

                            // Odczytaj wartość filtra z czwartego wiersza, jeśli filterRow nie jest null
                            if (filterRow != null) {
                                Cell filterCell = filterRow.getCell(colNum);
                                if (filterCell != null && filterCell.getCellType() == CellType.STRING) {
                                    String filterValue = filterCell.getStringCellValue();
                                    if (!filterValue.isEmpty()) {
                                        filters.put(columnName, filterValue);
                                    }
                                }
                            }
                        }
                    }
                }
                columnMap.put(sheet.getSheetName(), columns);
                filterMap.put(sheet.getSheetName(), filters);

                // Przetwarzanie pierwszego wiersza (headerRow) w celu odczytu nazw kolumn
                for (Cell cell : headerRow) {
                    if (cell.getCellType() == CellType.STRING) {
                        String cellValue = cell.getStringCellValue();
                        firstRow.add(cellValue);
                    }
                }

                // Przetwarzanie trzeciego wiersza (sortingRow) w celu odczytu informacji o sortowaniu
                List<SortColumn> sortColumns = new ArrayList<>();
                if (sortingRow != null) {
                    for (int colNum = 0; colNum < sortingRow.getLastCellNum(); colNum++) {
                        Cell cell = sortingRow.getCell(colNum);
                        if (cell != null && cell.getCellType() == CellType.STRING) {
                            String cellValue = cell.getStringCellValue();

                            if (cellValue.matches("\\d+[a-zA-Z]{2,4}")) {
                                int priority = Integer.parseInt(cellValue.replaceAll("[^0-9]", ""));
                                String sortDirection = cellValue.replaceAll("\\d+", "");
                                sortColumns.add(new SortColumn(priority, colNum, sortDirection));
                            }
                        }
                    }
                }

                // Sortowanie kolumn według priorytetu
                sortColumns.sort(Comparator.comparingInt(SortColumn::getPriority));
                sortingMap.put(sheet.getSheetName(), sortColumns);
            }
        }
        return tableColumnMap;
    }

    // Metoda tworzy i wypełnia plik Excel na podstawie przekazanych danych
    public void createAndFillExcelFile(Map<String, List<String>> tableColumnMap,
                                       Map<String, List<Map<String, Object>>> dataMap,
                                       HttpServletResponse response) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            int sheetIndex = 0;

            // Iteracja przez wszystkie wpisy w mapie tabel i kolumn
            for (Map.Entry<String, List<String>> entry : tableColumnMap.entrySet()) {
                String tableName = entry.getKey(); // Nazwa tabeli
                List<String> columns = entry.getValue(); // Lista kolumn
                List<Map<String, Object>> rows = dataMap.get(tableName); // Wiersze danych dla tabeli

                // Pobierz nazwę arkusza dla bieżącego indeksu
                String sheetName = sheetNames.get(sheetIndex);

                // Pobierz filtry dla arkusza
                Map<String, String> filters = filterMap.get(sheetName);
                if (filters != null && !filters.isEmpty()) {
                    // Zastosuj filtry do wierszy danych
                    rows = filterRows(rows, filters);
                }

                // Utwórz arkusz w skoroszycie
                Sheet sheet = workbook.createSheet(sheetName);

                // Pobierz pierwszy wiersz z nagłówkami dla arkusza
                List<String> firstRow = sheetIndex < firstRows.size() ? firstRows.get(sheetIndex) : new ArrayList<>();
                sheetIndex++;

                // Utwórz wiersz nagłówkowy w arkuszu
                Row headerRow = sheet.createRow(0);
                for (int i = 0; i < firstRow.size(); i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(firstRow.get(i)); // Ustaw wartość w komórce nagłówka
                }

                // Pobierz informacje o sortowaniu dla bieżącego arkusza
                List<SortColumn> sortColumns = sortingMap.get(sheetName);
                List<String> columnNames = columnMap.get(sheetName);

                // Sortowanie wierszy danych, jeśli zdefiniowano kolumny do sortowania
                if (sortColumns != null && !sortColumns.isEmpty() && columnNames != null) {
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

                // Utwórz wiersze danych w arkuszu
                int rowNum = 1;
                for (Map<String, Object> rowData : rows) {
                    Row row = sheet.createRow(rowNum++);
                    for (int colNum = 0; colNum < columns.size(); colNum++) {
                        Cell cell = row.createCell(colNum);
                        Object value = rowData.get(columns.get(colNum));
                        setCellValueAndStyle(cell, value, workbook); // Ustaw wartość i styl komórki
                    }
                }

                // Dostosuj szerokość kolumn
                for (int i = 0; i < firstRow.size(); i++) {
                    sheet.autoSizeColumn(i);
                    int columnWidth = sheet.getColumnWidth(i);
                    sheet.setColumnWidth(i, columnWidth + 2000); // Dodaj przestrzeń do szerokości kolumny
                }

                // Ustaw wysokość wierszy
                for (int i = 0; i < rowNum; i++) {
                    sheet.getRow(i).setHeightInPoints(20); // Ustaw wysokość wierszy na 20 punktów
                }
            }

            // Ustaw odpowiednie nagłówki dla odpowiedzi HTTP i zapisz plik
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            workbook.write(response.getOutputStream());
        }
    }

    // Metoda do filtrowania wierszy na podstawie wartości w czwartym wierszu z obsługą operatorów logicznych
    private List<Map<String, Object>> filterRows(List<Map<String, Object>> rows, Map<String, String> filters) {
        List<Map<String, Object>> filteredRows = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            boolean matches = true;
            for (Map.Entry<String, String> filter : filters.entrySet()) {
                Object value = row.get(filter.getKey());
                if (value == null || !matchesFilter(value, filter.getValue())) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                filteredRows.add(row);
            }
        }
        return filteredRows;
    }

    // Nowa metoda do porównywania wartości z filtrem, z obsługą operatorów
    private boolean matchesFilter(Object value, String filterValue) {
        if (value instanceof String) {
            return ((String) value).equalsIgnoreCase(filterValue); // Porównanie Stringów (bez rozróżniania wielkości liter)
        } else if (value instanceof Number) {
            return compareNumber((Number) value, filterValue); // Porównanie liczb z obsługą operatorów
        } else if (value instanceof java.util.Date) {
            return compareDate((java.util.Date) value, filterValue); // Porównanie dat z obsługą operatorów
        } else if (value instanceof Boolean) {
            return Boolean.parseBoolean(filterValue) == (Boolean) value; // Porównanie wartości boolean
        }
        return value.toString().equals(filterValue); // Domyślne porównanie jako String
    }

    // Metoda do porównywania liczb z obsługą operatorów
    private boolean compareNumber(Number value, String filterValue) {
        double numericValue = value.doubleValue();
        if (filterValue.startsWith(">=")) {
            return numericValue >= Double.parseDouble(filterValue.substring(2).trim());
        } else if (filterValue.startsWith("<=")) {
            return numericValue <= Double.parseDouble(filterValue.substring(2).trim());
        } else if (filterValue.startsWith(">")) {
            return numericValue > Double.parseDouble(filterValue.substring(1).trim());
        } else if (filterValue.startsWith("<")) {
            return numericValue < Double.parseDouble(filterValue.substring(1).trim());
        } else {
            return numericValue == Double.parseDouble(filterValue.trim());
        }
    }

    // Metoda do porównywania dat z obsługą operatorów
    private boolean compareDate(java.util.Date value, String filterValue) {
        java.sql.Date filterDate;
        try {
            filterDate = java.sql.Date.valueOf(filterValue.substring(1).trim());
        } catch (IllegalArgumentException e) {
            return false; // Jeśli filterValue nie jest poprawnym formatem daty, zwróć false
        }

        if (filterValue.startsWith(">=")) {
            return value.compareTo(filterDate) >= 0;
        } else if (filterValue.startsWith("<=")) {
            return value.compareTo(filterDate) <= 0;
        } else if (filterValue.startsWith(">")) {
            return value.compareTo(filterDate) > 0;
        } else if (filterValue.startsWith("<")) {
            return value.compareTo(filterDate) < 0;
        } else {
            return value.compareTo(filterDate) == 0;
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
