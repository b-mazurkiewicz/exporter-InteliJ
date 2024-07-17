package com.example.exporter.model;

import com.example.exporter.service.ExportTaskManager;
import lombok.*;
import jakarta.servlet.ServletContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

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

    public List<String> getTableColumns(JdbcTemplate jdbcTemplate) {
        if (this.tableName == null || this.tableName.isEmpty()) {
            throw new IllegalArgumentException("Table name is null or empty");
        }
        String query = String.format((
                        "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = '%s'"), this.tableName);
        return jdbcTemplate.query(query, (rs, rowNum) -> rs.getString("COLUMN_NAME"));
    }
}