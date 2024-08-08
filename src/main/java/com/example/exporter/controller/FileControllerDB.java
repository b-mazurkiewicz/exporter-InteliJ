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

//controller do importu i eksportu schematów
@Controller
@CrossOrigin("http://localhost:8080")
public class FileControllerDB {
    @Autowired
    private FileStorageService storageService;
    @Autowired
    private SchemaImportService schemaImportService;
    @Autowired
    private DataService dataService;

    // Mapa do przechowywania zadań mapowania tabel
    private final Map<String, TableColumnMapTask> taskMap = new ConcurrentHashMap<>();

    // Endpoint do uploadu pliku Excel z schematem
    @PostMapping("/upload")
    public ResponseEntity<?> uploadExcelSchema(@RequestParam("file") MultipartFile file) {
        try {
            // Przechowywanie pliku
            storageService.store(file);

            // Odczytywanie nazw tabel i kolumn z pliku
            Map<String, List<String>> tableColumnMap = schemaImportService.readTableAndColumnNames(file);

            // Generowanie unikalnego ID dla zadania
            String taskId = UUID.randomUUID().toString();
            TableColumnMapTask task = new TableColumnMapTask(taskId, "IN_PROGRESS", tableColumnMap);
            taskMap.put(taskId, task);

            // Zwrot ID zadania i oryginalnej nazwy pliku
            return ResponseEntity.ok(Map.of(
                    "taskId", taskId,
                    "originalFileName", file.getOriginalFilename()
            ));
        } catch (IOException e) {
            // Obsługa błędu podczas przetwarzania pliku
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to process Excel file: " + e.getMessage());
        }
    }

    // Endpoint do pobierania listy plików
    @GetMapping("/files")
    public ResponseEntity<List<ResponseFile>> getListFiles() {
        // Pobieranie wszystkich plików
        List<ResponseFile> files = storageService.getAllFiles().map(dbFile -> {
            // Tworzenie URI do pobrania pliku
            String fileDownloadUri = ServletUriComponentsBuilder
                    .fromCurrentContextPath()
                    .path("/files/")
                    .path(dbFile.getId())
                    .toUriString();

            // Tworzenie obiektu ResponseFile
            return new ResponseFile(
                    dbFile.getName(),
                    fileDownloadUri,
                    dbFile.getType(),
                    dbFile.getData().length);
        }).collect(Collectors.toList());

        // Zwrot listy plików
        return ResponseEntity.status(HttpStatus.OK).body(files);
    }

    // Endpoint do przetwarzania pliku o określonym ID
    @PostMapping("/files/{id}")
    public void processFile(@PathVariable String id, HttpServletResponse response) {
        try {
            // Pobieranie pliku z bazy danych
            FileDB fileDB = storageService.getFile(id);
            if (fileDB == null) {
                // Obsługa przypadku gdy plik nie został znaleziony
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("File not found");
                return;
            }

            // Tworzenie obiektu MultipartFile z danymi pliku
            MultipartFile file = new ByteArrayMultipartFile(fileDB.getName(), fileDB.getType(), fileDB.getData());

            // Odczytywanie nazw tabel i kolumn z pliku
            Map<String, List<String>> tableColumnMap = schemaImportService.readTableAndColumnNames(file);

            // Pobieranie danych dla tabel
            Map<String, List<Map<String, Object>>> dataMap = dataService.fetchDataForTables(tableColumnMap);

            // Tworzenie nazwy wyjściowego pliku
            String originalFileName = fileDB.getName();
            String outputFileName = constructOutputFileName(originalFileName, id);

            // Ustawianie nagłówków odpowiedzi
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + outputFileName + "\"");

            // Tworzenie i wypełnianie pliku Excel
            schemaImportService.createAndFillExcelFile(tableColumnMap, dataMap, response);
            response.setStatus(HttpServletResponse.SC_OK);
        } catch (NoSuchElementException e) {
            try {
                // Obsługa wyjątku gdy plik nie został znaleziony
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("File not found: " + e.getMessage());
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        } catch (IOException e) {
            try {
                // Obsługa błędu podczas przetwarzania pliku
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("Failed to process file: " + e.getMessage());
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    // Metoda do konstruowania nazwy wyjściowego pliku
    private String constructOutputFileName(String originalFileName, String id) {
        String fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        String baseFileName = originalFileName.substring(0, originalFileName.lastIndexOf("."));
        return baseFileName + "-" + id + fileExtension;
    }

    // Endpoint do usuwania pliku o określonym ID
    @DeleteMapping("/files/{id}")
    public ResponseEntity<?> deleteSchema(@PathVariable String id) {
        try {
            // Usuwanie pliku z bazy danych
            boolean deleted = storageService.deleteFile(id);
            if (deleted) {
                // Zwrot odpowiedzi gdy usunięcie zakończyło się sukcesem
                return ResponseEntity.ok("Schema deleted successfully");
            } else {
                // Zwrot odpowiedzi gdy plik nie został znaleziony
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Schema not found");
            }
        } catch (Exception e) {
            // Obsługa błędu podczas usuwania pliku
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to delete schema: " + e.getMessage());
        }
    }





    // Klasa wewnętrzna do reprezentacji MultipartFile na podstawie tablicy bajtów, można przenieść gdzieś indziej
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
        }
    }
}
