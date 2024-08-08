package com.example.exporter.model;

import lombok.*;
import java.util.Map;
import java.util.List;

@Data
@NoArgsConstructor
public class TableColumnMapTask {

    private String taskId;
    private String status;
    private Map<String, List<String>> tableColumnMap;

    public TableColumnMapTask(String taskId, String status, Map<String, List<String>> tableColumnMap) {
        this.taskId = taskId;
        this.status = status;
        this.tableColumnMap = tableColumnMap;
    }
}
