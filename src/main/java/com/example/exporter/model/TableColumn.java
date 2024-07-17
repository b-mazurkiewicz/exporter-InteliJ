package com.example.exporter.model;

import jakarta.persistence.*;

@Entity
public class TableColumn {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private ExportTask exportTask;

    private String tableName;

    private String columnName;
}
