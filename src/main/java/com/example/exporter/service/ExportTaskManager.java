package com.example.exporter.service;

import com.example.exporter.model.ExportTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.servlet.ServletContext;
import org.springframework.web.context.WebApplicationContext;


@Service
public class ExportTaskManager {

    private final Map<String, ExportTask> tasks = new ConcurrentHashMap<>();

    // Metoda do dodawania nowego zadania eksportu do mapy zadań
    public void addTask(ExportTask task) {
        tasks.put(task.getTaskId(), task);
    }

    // Metoda do pobierania zadania eksportu na podstawie ID zadania
    public ExportTask getTask(String taskId) {
        return tasks.get(taskId);
    }

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public ExportTaskManager(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Metoda do pobierania wszystkich tabel i widoków z bazy danych
    public List<String> getAllTablesAndViews() {
        String sql = "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA='PUBLIC'";
        return jdbcTemplate.queryForList(sql, String.class);
    }

    // Metoda do pobierania danych z podanej tabeli
    public List<Map<String, Object>> getTableData(String tableName) {
        String sql = "SELECT * FROM " + tableName;
        return jdbcTemplate.queryForList(sql);
    }
}
