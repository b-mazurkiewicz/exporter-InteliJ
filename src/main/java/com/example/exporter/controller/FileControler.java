package com.example.exporter.controller;

import com.example.exporter.model.*;
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
import java.util.*;
@RestController
@RequestMapping("/api")
public class FileControler {

    private final ExportTaskManager taskManager;

    @Autowired
    public FileControler(ExportTaskManager taskManager) {
        this.taskManager = taskManager;
    }

    //Mapping do wyswietlania wszystkich kolumn z wybranych wczesniej tabel
    @GetMapping("/columns")
    public ResponseEntity<Map<String, List<String>>> getColumnsForTables(@RequestParam List<String> tableNames) {
        Map<String, List<String>> columnsForTables = new HashMap<>();
        for (String tableName : tableNames) {
            List<String> columns = taskManager.getColumnsForTable(tableName);
            columnsForTables.put(tableName, columns);
        }
        return ResponseEntity.ok(columnsForTables);
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
        ExportTask task = new ExportTask(taskId, "IN_PROGRESS", Collections.singletonList(tableName));
        taskManager.addTask(task);
        return ResponseEntity.ok(taskId);
    }

    // Mapping do rozpoczęcia zadania eksportu
    @PostMapping("/export")
    public ResponseEntity<String> startExportTask(@RequestBody ExportTaskRequest request) {
        String taskId = UUID.randomUUID().toString();
        ExportTask task;

        if (request.getTableName() != null) {
            task = new ExportTask(taskId, "IN_PROGRESS", request.getTableName());
        } else if (request.getTableNames() != null && !request.getTableNames().isEmpty()) {
            task = new ExportTask(taskId, "IN_PROGRESS", request.getTableNames());
        } else {
            return ResponseEntity.badRequest().body("No valid table name(s) provided.");
        }

        taskManager.addTask(task);
        return ResponseEntity.ok(taskId);
    }

//    @PostMapping("/export")
//    public ResponseEntity<String> startExportTask(@RequestBody Map<String, List<String>> tableColumns) {
//        String taskId = UUID.randomUUID().toString();
//        ExportTask task = new ExportTask(taskId, "IN_PROGRESS", new ArrayList<>(tableColumns.keySet()));
//        taskManager.addTask(task);
//        return ResponseEntity.ok(taskId);
//    }


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

        Map<String, List<String>> tableColumns = exportTask.getTableColumns();
        if (tableColumns == null || tableColumns.isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No tables or columns selected");
            return;
        }

        taskManager.exportToExcel(tableColumns, response, taskId);
    }


    //bardzo dziwna sytuacja ze musze pisac komentarz w kodzie lololol
//    @GetMapping("/excel/{taskId}")
//    public void exportExcelByTaskId(@PathVariable String taskId, HttpServletResponse response) throws IOException {
//        ExportTask exportTask = taskManager.getTask(taskId);
//        if (exportTask == null) {
//            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Task not found");
//            return;
//        }
//
//        List<String> tableNames = exportTask.getTableNames();
//        if (tableNames == null || tableNames.isEmpty()) {
//            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Table names are missing");
//            return;
//        }
//
//        Map<String, List<String>> tableColumns = new HashMap<>();
//        for (String tableName : tableNames) {
//            List<String> columns = taskManager.getColumnsForTable(tableName);
//            tableColumns.put(tableName, columns);
//        }
//
//        taskManager.exportToExcel(tableColumns, response, taskId);
//    }

}