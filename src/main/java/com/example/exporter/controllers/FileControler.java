package com.example.exporter.controllers;

import com.example.exporter.services.UserService;
import com.example.exporter.objects.User;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
public class FileControler {

    @Autowired
    private UserService userService;

    @GetMapping("/export/excel")
    public void exportExcel(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformat-officedocument.spreadsheetml.sheet");
        String headerKey = "Content-Disposition";
        String headerValue = "attachment; filename=ExcelDataSet.xlsx";
        response.setHeader(headerKey, headerValue);

        List<User> listUsers = userService.getAllUsers();

        XSSFWorkbook workbook = new XSSFWorkbook();

        //AKTUALNIE WPISUJEMY USERS ZE WZGLĘDU NA RODZAJ PRZECHOWYWANYCH DANYCH
        XSSFSheet sheet = workbook.createSheet("Users");        //DO ZMIANY POTEM

        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 12);
        headerFont.setColor(IndexedColors.BLUE.getIndex());

        CellStyle headerCellStyle = workbook.createCellStyle();
        headerCellStyle.setFont(headerFont);

        //NAZWY KOLUMN DO ZMIANY ZE WZGLĘDU NA ZAWARTOŚĆ DATASET
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("ID");
        headerRow.createCell(1).setCellValue("FirstName");
        headerRow.createCell(2).setCellValue("LastName");
        headerRow.createCell(3).setCellValue("Email");

        headerRow.forEach(cell -> cell.setCellStyle(headerCellStyle));

        int rowCount = 1;

        //ZWROCIC UWAGE NA NAZWY KOLUMN W POZNIEJSZYCH WERSJACH TABELI
        for (User user : listUsers) {
            Row row = sheet.createRow(rowCount++);
            row.createCell(0).setCellValue(user.getId());
            row.createCell(1).setCellValue(user.getFirstName());
            row.createCell(2).setCellValue(user.getLastName());
            row.createCell(3).setCellValue(user.getEmail());
        }

        //AUTOMATYCZNA WIELKOSC KOLUMN
        for (int i=0;i < listUsers.size(); i++){
            sheet.autoSizeColumn(i);
        }

        //ServletOutputStream outputStream = response.getOutputStream();
        workbook.write(response.getOutputStream());
        workbook.close();
        //outputStream.close();
    }
}
