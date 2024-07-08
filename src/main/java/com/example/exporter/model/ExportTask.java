package com.example.exporter.model;

import lombok.*;
import jakarta.servlet.ServletContext;
import org.springframework.web.context.WebApplicationContext;

// Klasa reprezentująca zadanie eksportu
@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ExportTask {
    private String taskId;  // Unikalny identyfikator zadania
    private String status;  // Status zadania (IN_PROGRESS, COMPLETED, FAILED)
    private String filePath; // Ścieżka do wygenerowanego pliku
}