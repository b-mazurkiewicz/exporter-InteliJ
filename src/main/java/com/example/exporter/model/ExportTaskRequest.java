package com.example.exporter.model;

import java.util.List;

public class ExportTaskRequest {

    private String tableName;
    private List<String> tableNames;

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public List<String> getTableNames() {
        return tableNames;
    }

    public void setTableNames(List<String> tableNames) {
        this.tableNames = tableNames;
    }
}
