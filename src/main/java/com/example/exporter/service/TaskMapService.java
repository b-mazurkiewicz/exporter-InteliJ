package com.example.exporter.service;
import com.example.exporter.model.TableColumnMapTask;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class TaskMapService {

    // Mapa przechowująca zadania eksportu, z identyfikatorem zadania jako kluczem
    private final Map<String, TableColumnMapTask> taskMap = new HashMap<>();

    // Metoda do dodawania zadania
    public void put(String taskId, TableColumnMapTask task) {
        taskMap.put(taskId, task);
    }

    // Metoda do pobierania zadania na podstawie identyfikatora
    public TableColumnMapTask get(String taskId) {
        return taskMap.get(taskId);
    }

    // Metoda do usuwania zadania na podstawie identyfikatora
    public void remove(String taskId) {
        taskMap.remove(taskId);
    }
}
