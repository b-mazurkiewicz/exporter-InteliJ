package com.example.exporter.controller;

import com.example.exporter.model.Company;
import com.example.exporter.service.CompanyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/companies")
public class CompanyControler {
    @Autowired
    private CompanyService companyService;
    /*@GetMapping("/nip/{nip}")
    public Company getCompanyByNip(@PathVariable String nip) {
        return companyService.getCompanyByNip(nip);
    }*/
    @GetMapping
    public List<Company> getAllCompanies() {
        return companyService.getAllCompanies();
    }

    @GetMapping("/{id}")
    public Company getCompany(@PathVariable long id) {
        return companyService.getCompanyById(id);
    }

    @PostMapping
    public Company createCompany(@RequestBody Company company) {
        return companyService.saveCompany(company);
    }

    @PutMapping("/{id}")
    public Company updateCompany(@RequestBody Company company) {
        return companyService.updateCompany(company);
    }

    @DeleteMapping("/{id}")
    public void deleteCompany(@PathVariable long id) {
        companyService.deleteCompany(id);
    }
}