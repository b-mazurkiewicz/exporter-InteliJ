package com.example.exporter.model;

import lombok.*;
import jakarta.servlet.ServletContext;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ExportTask {

    private String taskId;
    private String status;
    private String tableName;
    private List<String> tableNames;

    public ExportTask(String taskId, String status, String tableName) {
        this.taskId = taskId;
        this.status = status;
        this.tableName = tableName;
    }

    public ExportTask(String taskId, String status, List<String> tableNames) {
        this.taskId = taskId;
        this.status = status;
        this.tableNames = tableNames;
    }
}