package com.example.exporter.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "export_tasks")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExportTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private String status;

    private String taskId;

    @ElementCollection
    private List<String> tableNames;

    @OneToMany(mappedBy = "exportTask", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TableColumn> tableColumns;


    // ... other methods

    public ExportTask(String taskId, String status, List<String> tableNames, Map<String, List<String>> tableColumns) {
        this.taskId = taskId;
        this.status = status;
        this.tableNames = tableNames;
        this.tableColumns = tableColumns;
    }

    public Map<String, List<String>> getTableColumns() {
        Map<String, List<String>> tableColumns = new HashMap<>();
        for (String tableName : tableNames) {
            List<String> columns = jdbcTemplate.queryForList(
                    "SELECT column_name FROM information_schema.columns WHERE table_name = ?",
                    String.class, tableName).stream().toList();
            tableColumns.put(tableName, columns);
        }
        return tableColumns;
    }


}