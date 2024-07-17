package com.example.exporter.controller;

import com.example.exporter.model.ExportTask;
import com.example.exporter.service.ExportTaskManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class FileControler {

    private final ExportTaskManager taskManager;

    @Autowired
    public FileControler(ExportTaskManager taskManager) {
        this.taskManager = taskManager;
    }

    @GetMapping("/tables")
    public ResponseEntity<List<String>> getAllTablesAndViews() {
        List<String> tablesAndViews = taskManager.getAllTablesAndViews();
        return ResponseEntity.ok(tablesAndViews);
    }

    @PostMapping("/export")
    public ResponseEntity<String> startExportTask(@RequestBody Map<String, List<String>> tableColumns) {
        String taskId = UUID.randomUUID().toString();
        ExportTask task = new ExportTask(taskId, "IN_PROGRESS", new ArrayList<>(tableColumns.keySet()), taskManager.getJdbcTemplate());
        taskManager.addTask(task);
        return ResponseEntity.ok(taskId);
    }

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

//    @GetMapping("/excel/{taskId}")
//    public void exportExcelByTaskId(@PathVariable String taskId, HttpServletResponse response) throws IOException {
//        ExportTask exportTask = taskManager.getTask(taskId);
//        if (exportTask == null) {
//            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Task not found");
//            return;
//        }
//
//        Map<String, List<String>> tableColumns = exportTask.getTableColumns(); // Ensure getTableColumns uses JdbcTemplate
//        if (tableColumns == null || tableColumns.isEmpty()) {
//            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No tables or columns selected");
//            return;
//        }
//
//        taskManager.exportToExcel(tableColumns, response, taskId);
//    }

    @GetMapping("/columns")
    public ResponseEntity<Map<String, List<String>>> getColumnsForTables(@RequestParam List<String> tableNames) {
        Map<String, List<String>> columnsForTables = taskManager.getColumnsForTables(tableNames);
        return ResponseEntity.ok(columnsForTables);
    }
}
