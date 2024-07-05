package com.example.exporter.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.servlet.ServletContext;
import org.springframework.web.context.WebApplicationContext;


// Klasa reprezentująca zadanie eksportu
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExportTask {
    private String taskId;  // Unikalny identyfikator zadania
    private String status;  // Status zadania (IN_PROGRESS, COMPLETED, FAILED)
    private String filePath; // Ścieżka do wygenerowanego pliku

    // Gettery i settery
}
