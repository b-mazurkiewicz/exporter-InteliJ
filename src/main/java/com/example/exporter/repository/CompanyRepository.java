package com.example.exporter.repository;

import com.example.exporter.model.Company;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository <Company, Long> {
}
