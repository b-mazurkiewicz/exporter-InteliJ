package com.example.exporter.service;

import com.example.exporter.model.ExportTask;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.servlet.ServletContext;
import org.springframework.web.context.WebApplicationContext;


@Service
public class ExportTaskManager {
    private final Map<String, ExportTask> tasks = new ConcurrentHashMap<>();

    public void addTask(ExportTask task) {
        tasks.put(task.getTaskId(), task);
    }

    public ExportTask getTask(String taskId) {
        return tasks.get(taskId);
    }

    public void updateTask(String taskId, String status, String filePath) {
        ExportTask task = tasks.get(taskId);
        if (task != null) {
            task.setStatus(status);
            task.setFilePath(filePath);
        }
    }
}
