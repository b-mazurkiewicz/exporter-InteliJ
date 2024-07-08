package com.example.exporter.controller;

import com.example.exporter.service.ExportTaskManager;
import com.example.exporter.service.ViewService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/views")
public class ViewsControler {

    private final ViewService viewService;
    private final ExportTaskManager excelExportService;

    @Autowired
    public ViewsControler(ViewService viewService, ExportTaskManager excelExportService) {
        this.viewService = viewService;
        this.excelExportService = excelExportService;
    }

    @RequestMapping("/create")
    public void createView() {
        viewService.createViews();
    }

    @GetMapping("/tables-and-views")
    public List<String> getTablesAndViews() {
        return viewService.getTablesAndViews();
    }

    @GetMapping("/export/excel/{tableName}")
    public void exportToExcel(@PathVariable String tableName, HttpServletResponse response) throws IOException {
        excelExportService.exportToExcel(tableName, response);
    }
}

