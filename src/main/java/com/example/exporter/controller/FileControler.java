package com.example.exporter.controller;

import com.example.exporter.model.Address;
import com.example.exporter.model.Company;
import com.example.exporter.model.ExportTask;
import com.example.exporter.model.User;
import com.example.exporter.service.ExportTaskManager;
import com.example.exporter.service.UserService;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;


@Controller
@RequestMapping("/api/export")
public class FileControler {

    private final UserService userService;
    private final ExportTaskManager taskManager;

    @Autowired
    public FileControler(UserService userService, ExportTaskManager taskManager) {
        this.userService = userService;
        this.taskManager = taskManager;
    }

    @GetMapping("/excel")
    public void exportExcel(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String headerKey = "Content-Disposition";
        String headerValue = "attachment; filename=ExcelDataSet.xlsx";
        response.setHeader(headerKey, headerValue);

        List<User> listUsers = userService.getAllUsers();

        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Users");

        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 12);
        headerFont.setColor(IndexedColors.BLUE.getIndex());

        CellStyle headerCellStyle = workbook.createCellStyle();
        headerCellStyle.setFont(headerFont);

        XSSFRow headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("ID");
        headerRow.createCell(1).setCellValue("FirstName");
        headerRow.createCell(2).setCellValue("LastName");
        headerRow.createCell(3).setCellValue("Email");
        headerRow.createCell(4).setCellValue("Street");
        headerRow.createCell(5).setCellValue("City");
        headerRow.createCell(6).setCellValue("ZipCode");
        headerRow.createCell(7).setCellValue("Country");
        headerRow.createCell(8).setCellValue("CompanyName"); // Dodanie nazwy firmy

        headerRow.forEach(cell -> cell.setCellStyle(headerCellStyle));

        int rowCount = 1;

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
            Company company = user.getCompany(); // Pobranie danych o firmie
            if (company != null) {
                row.createCell(8).setCellValue(company.getCompanyName()); // Ustawienie nazwy firmy
            }
        }

        for (int i = 0; i < 9; i++) { // Zmiana na 9 kolumn
            sheet.autoSizeColumn(i);
        }

        workbook.write(response.getOutputStream());
        workbook.close();
    }



    // Metoda rozpoczynająca zadanie eksportu i zwracająca ID zadania
    @GetMapping("/status")
    public ResponseEntity<String> exportExcel() {
        String taskId = UUID.randomUUID().toString(); // Generowanie unikalnego ID dla zadania
        ExportTask task = new ExportTask(taskId, "IN_PROGRESS", null); // Tworzenie nowego zadania eksportu
        taskManager.addTask(task); // Dodanie zadania do menedżera zadań
        return ResponseEntity.ok(taskId); // Zwrócenie ID zadania w ciele odpowiedzi
    }

    // Metoda zwracająca status zadania o podanym taskId
    @GetMapping("/status/{taskId}")
    public ResponseEntity<ExportTask> getStatus(@PathVariable String taskId) {
        ExportTask task = taskManager.getTask(taskId);
        if (task != null) {
            return ResponseEntity.ok(task); // Zwrócenie statusu zadania, jeśli istnieje
        } else {
            return ResponseEntity.notFound().build(); // Zwrócenie odpowiedzi 404, jeśli zadanie nie zostało znalezione
        }
    }
    @GetMapping("/excel/{taskId}")
    public void exportExcelByTaskId(@PathVariable String taskId, HttpServletResponse response) throws IOException {
        ExportTask exportTask = taskManager.getTask(taskId); // Pobierz zadanie eksportu na podstawie taskId
        if (exportTask == null) {
            // Obsłuż sytuację, gdy zadanie nie istnieje
            //throw new ResourceNotFoundException("Task not found with id: " + taskId);
        }

        // Pobierz dane, które mają być eksportowane do pliku Excel na podstawie taskId (np. użytkowników)
        List<User> listUsers = userService.getAllUsers();

        // Ustawienie odpowiedzi HTTP dla generowanego pliku Excel
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String headerKey = "Content-Disposition";
        String headerValue = "attachment; filename=ExcelDataSet_" + taskId + ".xlsx";
        response.setHeader(headerKey, headerValue);

        // Tworzenie nowego workbooka Excel
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Users");

        // Utworzenie stylu dla nagłówków
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 12);
        headerFont.setColor(IndexedColors.BLUE.getIndex());
        CellStyle headerCellStyle = workbook.createCellStyle();
        headerCellStyle.setFont(headerFont);

        // Utworzenie wiersza nagłówkowego
        XSSFRow headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("ID");
        headerRow.createCell(1).setCellValue("FirstName");
        headerRow.createCell(2).setCellValue("LastName");
        headerRow.createCell(3).setCellValue("Email");
        headerRow.createCell(4).setCellValue("Street");
        headerRow.createCell(5).setCellValue("City");
        headerRow.createCell(6).setCellValue("ZipCode");
        headerRow.createCell(7).setCellValue("Country");
        headerRow.createCell(8).setCellValue("CompanyName");

        // Ustawienie stylu nagłówka na każdej komórce wiersza nagłówkowego
        headerRow.forEach(cell -> cell.setCellStyle(headerCellStyle));

        // Dodawanie danych użytkowników do arkusza Excel
        int rowCount = 1;
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
            Company company = user.getCompany();
            if (company != null) {
                row.createCell(8).setCellValue(company.getCompanyName());
            }
        }

        // Automatyczne dostosowanie szerokości kolumn do zawartości
        for (int i = 0; i < 9; i++) {
            sheet.autoSizeColumn(i);
        }

        // Zapisanie workbooka do odpowiedzi HTTP
        workbook.write(response.getOutputStream());
        workbook.close();
    }
/*
    @Autowired
    public FileControler(ExportTaskManager excelExportService) {
        this.excelExportService = excelExportService;
    }

    @GetMapping("/export/excel/{tableName}")
    public void exportToExcel(@PathVariable String tableName, HttpServletResponse response) throws IOException {
        excelExportService.exportToExcel(tableName, response);
    }
*/
    /*
    @GetMapping("/status/{taskId}")
    @ResponseBody
    public ExportTask getStatus(@PathVariable String taskId) {
        return taskManager.getTask(taskId);
    }

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
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }
    */

}