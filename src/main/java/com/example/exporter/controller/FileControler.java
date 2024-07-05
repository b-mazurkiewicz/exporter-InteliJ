package com.example.exporter.controller;

import com.example.exporter.model.Address;
import com.example.exporter.model.ExportTask;
import com.example.exporter.model.User;
import com.example.exporter.service.ExportTaskManager;
import com.example.exporter.service.UserService;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;



@Controller
@RequestMapping("/api/export")
public class FileControler {

    @Autowired
    private UserService userService;

    @Autowired
    public FileControler(UserService userService) {
        this.userService = userService;
    }

    @Autowired
    private ExportTaskManager taskManager;

    // Metoda rozpoczynająca zadanie eksportu
    @PostMapping("/excel")
    @ResponseBody
    public void exportExcel(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String headerKey = "Content-Disposition";
        String headerValue = "attachment; filename=ExcelDataSet.xlsx"; //ustawienie specyficznej nazwy dla pliku
        response.setHeader(headerKey, headerValue);

        //Dodanie numeru id dla każdego exportu
        String taskId = UUID.randomUUID().toString(); // Generowanie unikalnego ID dla zadania
        ExportTask task = new ExportTask(taskId, "IN_PROGRESS", null); // Tworzenie nowego zadania eksportu
        taskManager.addTask(task); // Dodanie zadania do menedżera zadań

        List<User> listUsers = userService.getAllUsers();

        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Users");

        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 12);
        headerFont.setColor(IndexedColors.BLUE.getIndex());

        CellStyle headerCellStyle = workbook.createCellStyle();
        headerCellStyle.setFont(headerFont);

        // Tworzenie nagłówków kolumn
        XSSFRow headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("ID");
        headerRow.createCell(1).setCellValue("FirstName");
        headerRow.createCell(2).setCellValue("LastName");
        headerRow.createCell(3).setCellValue("Email");
        headerRow.createCell(4).setCellValue("Street");
        headerRow.createCell(5).setCellValue("City");
        headerRow.createCell(6).setCellValue("ZipCode");
        headerRow.createCell(7).setCellValue("Country");

        headerRow.forEach(cell -> cell.setCellStyle(headerCellStyle));

        int rowCount = 1;

        for(User user : listUsers) {
            XSSFRow row = sheet.createRow(rowCount++);
            row.createCell(0).setCellValue(user.getId());
            row.createCell(1).setCellValue(user.getFirstName());
            row.createCell(2).setCellValue(user.getLastName());
            row.createCell(3).setCellValue(user.getEmail());
            Address address = user.getAddress();
            if( address != null ) {
                row.createCell(4).setCellValue(address.getStreet());
                row.createCell(5).setCellValue(address.getCity());
                row.createCell(6).setCellValue(address.getZipCode());
                row.createCell(7).setCellValue(address.getCountry());
            }
        }

        for (int i = 0; i <8; i++){
            sheet.autoSizeColumn(i);
        }

        workbook.write(response.getOutputStream());
        workbook.close();
/*
        // Asynchroniczne przetwarzanie zadania eksportu
        CompletableFuture.runAsync(() -> {
            try {
                String filePath = exportExcelFile(); // Wywołanie metody eksportującej dane do pliku
                taskManager.updateTask(taskId, "COMPLETED", filePath); // Aktualizacja statusu zadania na "COMPLETED"
            } catch (IOException e) {
                taskManager.updateTask(taskId, "FAILED", null); // Aktualizacja statusu zadania na "FAILED" w przypadku błędu
            }
        });

        return taskId; // Zwrócenie ID zadania */
    }

    // Metoda asynchronicznie eksportująca dane do pliku Excel

    public String exportExcelFile() throws IOException {
        List<User> listUsers = userService.getAllUsers(); // Pobranie listy użytkowników z bazy danych

        XSSFWorkbook workbook = new XSSFWorkbook(); // Utworzenie nowego workbook'a Excel
        XSSFSheet sheet = workbook.createSheet("Users"); // Utworzenie nowego arkusza

        // Styl dla nagłówków kolumn
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 12);
        headerFont.setColor(IndexedColors.BLUE.getIndex());

        CellStyle headerCellStyle = workbook.createCellStyle();
        headerCellStyle.setFont(headerFont);

        // Tworzenie nagłówków kolumn
        XSSFRow headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("ID");
        headerRow.createCell(1).setCellValue("FirstName");
        headerRow.createCell(2).setCellValue("LastName");
        headerRow.createCell(3).setCellValue("Email");
        headerRow.createCell(4).setCellValue("Street");
        headerRow.createCell(5).setCellValue("City");
        headerRow.createCell(6).setCellValue("ZipCode");
        headerRow.createCell(7).setCellValue("Country");

        // Ustawienie stylu dla nagłówków kolumn
        headerRow.forEach(cell -> cell.setCellStyle(headerCellStyle));

        int rowCount = 1;

        // Wypełnianie arkusza danymi użytkowników
        for (User user : listUsers) {
            XSSFRow row = sheet.createRow(rowCount++);
            row.createCell(0).setCellValue(user.getId());
            row.createCell(1).setCellValue(user.getFirstName());
            row.createCell(2).setCellValue(user.getLastName());
            row.createCell(3).setCellValue(user.getEmail());
            Address address = user.getAddress();
            if (address != null) {
                row.createCell(4).setCellValue(address.getStreet());
                row.createCell(5).setCellValue(address.getCity());
                row.createCell(6).setCellValue(address.getZipCode());
                row.createCell(7).setCellValue(address.getCountry());
            }
        }

        // Automatyczne dopasowanie szerokości kolumn
        for (int i = 0; i < 8; i++) {
            sheet.autoSizeColumn(i);
        }

        String filePath = "path/to/exported/ExcelDataSet.xlsx"; // Ścieżka do pliku wynikowego
        try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
            workbook.write(fileOut); // Zapisanie workbook'a do pliku
        }
        workbook.close(); // Zamknięcie workbook'a
        return filePath; // Zwrócenie ścieżki do pliku
    }

    // Metoda sprawdzająca status zadania
    @GetMapping("/status/{taskId}")
    @ResponseBody
    public ExportTask getStatus(@PathVariable String taskId) {
        return taskManager.getTask(taskId);
    }

    // Metoda pobierająca wygenerowany plik
    @GetMapping("/download/{taskId}")
    public void downloadFile(@PathVariable String taskId, HttpServletResponse response) throws IOException {
        ExportTask task = taskManager.getTask(taskId);
        if (task != null && "COMPLETED".equals(task.getStatus())) {
            File file = new File(task.getFilePath());
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=" + file.getName());
            response.setContentLength((int) file.length());
            try (FileInputStream inputStream = new FileInputStream(file)) {
                FileCopyUtils.copy(inputStream, response.getOutputStream());
            }
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND); // Zwrócenie statusu 404 jeśli zadanie nie zostało zakończone
        }
    }
}
