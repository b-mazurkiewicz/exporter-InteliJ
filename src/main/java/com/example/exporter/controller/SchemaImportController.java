package com.example.exporter.controller;

import com.example.exporter.model.ExportTask;
import com.example.exporter.model.TableColumnMapTask;
import com.example.exporter.service.SchemaImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/schema")
public class SchemaImportController {

    private final SchemaImportService schemaImportService;
    private final Map<String, TableColumnMapTask> taskMap = new ConcurrentHashMap<>();

    @Autowired
    public SchemaImportController(SchemaImportService schemaImportService) {
        this.schemaImportService = schemaImportService;
    }

    // Endpoint do przesyłania pliku z schematem
    @PostMapping("/upload")
    public ResponseEntity<?> uploadExcelSchema(@RequestParam("file") MultipartFile file) {
        try {
            Map<String, List<String>> tableColumnMap = schemaImportService.readTableAndColumnNames(file);
            String taskId = UUID.randomUUID().toString();

            TableColumnMapTask task = new TableColumnMapTask(taskId, "IN_PROGRESS", tableColumnMap);
            taskMap.put(taskId, task);

            // Zwróć odpowiedź z taskId i mapą tabel oraz kolumn
            return ResponseEntity.ok(task);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to process Excel file: " + e.getMessage());
        }
    }

    // Endpoint do eksportowania wypełnionego schematu na podstawie ID zadania
    @GetMapping("/export/{taskId}")
    public void exportFilledSchemaByTaskId(@PathVariable String taskId, HttpServletResponse response) throws IOException {
        TableColumnMapTask task = taskMap.get(taskId);
        if (task == null || task.getTableColumnMap() == null || task.getTableColumnMap().isEmpty()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Task not found or no schema data available");
            return;
        }

        schemaImportService.exportFilledSchemaToExcel(task.getTableColumnMap(), response, taskId);
    }
}