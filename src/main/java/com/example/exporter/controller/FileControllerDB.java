package com.example.exporter.controller;

import com.example.exporter.message.ResponseFile;
import com.example.exporter.message.ResponseMessage;
import com.example.exporter.model.FileDB;
import com.example.exporter.model.TableColumnMapTask;
import com.example.exporter.service.DataService;
import com.example.exporter.service.FileStorageService;
import com.example.exporter.service.SchemaImportService;
import com.example.exporter.service.TaskMapService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Controller
@CrossOrigin("http://localhost:8080")
public class FileControllerDB {
    @Autowired
    private FileStorageService storageService;
    @Autowired
    private SchemaImportService schemaImportService;
    @Autowired
    private DataService dataService;
    @Autowired
    private TaskMapService taskMapService;
    private final Map<String, TableColumnMapTask> taskMap = new ConcurrentHashMap<>();

    @PostMapping("/upload")
    public ResponseEntity<?> uploadExcelSchema(@RequestParam("file") MultipartFile file) {
        try {
            storageService.store(file);

            Map<String, List<String>> tableColumnMap = schemaImportService.readTableAndColumnNames(file);
            String taskId = UUID.randomUUID().toString();

            TableColumnMapTask task = new TableColumnMapTask(taskId, "IN_PROGRESS", tableColumnMap);
            taskMap.put(taskId, task);

            return ResponseEntity.ok(task);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to process Excel file: " + e.getMessage());
        }
    }

    @GetMapping("/files")
    public ResponseEntity<List<ResponseFile>> getListFiles() {
        List<ResponseFile> files = storageService.getAllFiles().map(dbFile -> {
            String fileDownloadUri = ServletUriComponentsBuilder
                    .fromCurrentContextPath()
                    .path("/files/")
                    .path(dbFile.getId())
                    .toUriString();

            return new ResponseFile(
                    dbFile.getName(),
                    fileDownloadUri,
                    dbFile.getType(),
                    dbFile.getData().length);
        }).collect(Collectors.toList());

        return ResponseEntity.status(HttpStatus.OK).body(files);
    }

    @PostMapping("/files/{id}")
    public void processFile(@PathVariable String id, HttpServletResponse response) {
        try {
            FileDB fileDB = storageService.getFile(id);
            if (fileDB == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("File not found");
                return;
            }

            // Tworzymy plik MultipartFile z danych pliku
            MultipartFile file = new ByteArrayMultipartFile(fileDB.getName(), fileDB.getType(), fileDB.getData());

            // Utwórz mapę tabel i kolumn
            Map<String, List<String>> tableColumnMap = schemaImportService.readTableAndColumnNames(file);

            // Pobierz dane
            Map<String, List<Map<String, Object>>> dataMap = dataService.fetchDataForTables(tableColumnMap);

            // Ustaw nagłówki odpowiedzi
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=schema-" + id + ".xlsx");

            // Utwórz i wypełnij plik Excel
            schemaImportService.createAndFillExcelFile(tableColumnMap, dataMap, response);

            // Ustaw status odpowiedzi na OK
            response.setStatus(HttpServletResponse.SC_OK);
        } catch (NoSuchElementException e) {
            try {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("File not found: " + e.getMessage());
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        } catch (IOException e) {
            try {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("Failed to process file: " + e.getMessage());
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    @DeleteMapping("/files/{id}")
    public ResponseEntity<?> deleteSchema(@PathVariable String id) {
        try {
            boolean deleted = storageService.deleteFile(id);
            if (deleted) {
                return ResponseEntity.ok("Schema deleted successfully");
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Schema not found");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to delete schema: " + e.getMessage());
        }
    }

    private static class ByteArrayMultipartFile implements MultipartFile {

        private final byte[] content;
        private final String name;
        private final String contentType;

        public ByteArrayMultipartFile(String name, String contentType, byte[] content) {
            this.name = name;
            this.contentType = contentType;
            this.content = content;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getOriginalFilename() {
            return name;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return content.length == 0;
        }

        @Override
        public long getSize() {
            return content.length;
        }

        @Override
        public byte[] getBytes() {
            return content;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(content);
        }

        @Override
        public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
            // Metoda nieużywana w tym przypadku
        }
    }
}