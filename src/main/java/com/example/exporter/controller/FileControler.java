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
import java.util.Map;
import java.util.UUID;


@RestController
@RequestMapping("/api/export")
public class FileControler {

    private final UserService userService;
    private final ExportTaskManager taskManager;

    @Autowired
    public FileControler(UserService userService, ExportTaskManager taskManager) {
        this.userService = userService;
        this.taskManager = taskManager;
    }

    // Mapping do wyświetlania wszystkich tabel i widoków
    @GetMapping("/tables")
    public ResponseEntity<List<String>> getAllTablesAndViews() {
        List<String> tablesAndViews = taskManager.getAllTablesAndViews();
        return ResponseEntity.ok(tablesAndViews);
    }

    // Mapping do rozpoczęcia zadania eksportu na podstawie nazwy tabeli/widoku
    @GetMapping("/export/{tableName}")
    public ResponseEntity<String> startExportTask(@PathVariable String tableName) {
        String taskId = UUID.randomUUID().toString();
        ExportTask task = new ExportTask(taskId, "IN_PROGRESS", tableName);
        taskManager.addTask(task);
        return ResponseEntity.ok(taskId);
    }

    // Mapping do sprawdzania statusu zadania na podstawie ID
    @GetMapping("/status/{taskId}")
    public ResponseEntity<ExportTask> getStatus(@PathVariable String taskId, HttpServletResponse response) throws IOException {
        ExportTask task = taskManager.getTask(taskId);
        if (task != null) {
            return ResponseEntity.ok(task);
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Task not found");
            return ResponseEntity.notFound().build();
        }
    }

    // Mapping do pobierania pliku Excel na podstawie ID zadania
    @GetMapping("/excel/{taskId}")
    public void exportExcelByTaskId(@PathVariable String taskId, HttpServletResponse response) throws IOException {
        ExportTask exportTask = taskManager.getTask(taskId);
        if (exportTask == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Task not found");
            return;
        }

        String tableName = exportTask.getTableName();
        List<Map<String, Object>> data = taskManager.getTableData(tableName);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String headerKey = "Content-Disposition";
        String headerValue = "attachment; filename=Export_" + taskId + ".xlsx";
        response.setHeader(headerKey, headerValue);

        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Data");

        if (!data.isEmpty()) {
            XSSFRow headerRow = sheet.createRow(0);
            Map<String, Object> firstRow = data.get(0);
            int cellIdx = 0;
            for (String column : firstRow.keySet()) {
                headerRow.createCell(cellIdx++).setCellValue(column);
            }

            int rowIdx = 1;
            for (Map<String, Object> rowData : data) {
                XSSFRow row = sheet.createRow(rowIdx++);
                cellIdx = 0;
                for (Object value : rowData.values()) {
                    row.createCell(cellIdx++).setCellValue(value != null ? value.toString() : "");
                }
            }

            for (int i = 0; i < firstRow.size(); i++) {
                sheet.autoSizeColumn(i);
            }
        }

        workbook.write(response.getOutputStream());
        workbook.close();
    }
}