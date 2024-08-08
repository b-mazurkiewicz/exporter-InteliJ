package com.example.exporter.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DataService {

    private final JdbcTemplate jdbcTemplate;

    public DataService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, List<Map<String, Object>>> fetchDataForTables(Map<String, List<String>> tableColumnMap) {
        Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();

        for (Map.Entry<String, List<String>> entry : tableColumnMap.entrySet()) {
            String tableName = entry.getKey();
            List<String> columns = entry.getValue();
            String columnList = String.join(", ", columns);

            String sql = String.format("SELECT %s FROM %s", columnList, tableName);
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);

            result.put(tableName, rows);
        }

        return result;
    }
}
