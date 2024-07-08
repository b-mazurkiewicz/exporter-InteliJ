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

    private String taskId;
    private String status;
    private String tableName;
}