package com.example.exporter.controller;

import com.example.exporter.model.ExportTask;
import com.example.exporter.service.ExportTaskManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class FileControler {

    private final ExportTaskManager taskManager;

    @Autowired
    public FileControler(ExportTaskManager taskManager) {
        this.taskManager = taskManager;
    }

    // Mapping do wyświetlania wszystkich tabel i widoków
    @GetMapping("/tables")
    public ResponseEntity<List<String>> getAllTablesAndViews() {
        List<String> tablesAndViews = taskManager.getAllTablesAndViews();
        return ResponseEntity.ok(tablesAndViews);
    }

    // Mapping do rozpoczęcia zadania eksportu na podstawie nazwy tabeli/widoku
    @PostMapping("/export")
    public ResponseEntity<String> startExportTask(@RequestBody List<String> tableNames) {
        String taskId = UUID.randomUUID().toString();
        ExportTask task = new ExportTask(taskId, "IN_PROGRESS", tableNames);
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

        List<String> tableNames = exportTask.getTableNames();
        if (tableNames == null || tableNames.isEmpty()) {
            String tableName = exportTask.getTableName();
            if (tableName == null) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Table name is null");
                return;
            }
            tableNames = Collections.singletonList(tableName);
        }

        // Wywołanie metody exportToExcel z ExportTaskManager, aby stworzyć plik Excel z odpowiednią liczbą arkuszy
        taskManager.exportToExcel(tableNames, response, taskId);
    }
}
