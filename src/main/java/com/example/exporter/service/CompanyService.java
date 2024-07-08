package com.example.exporter.service;

import com.example.exporter.model.Company;
import com.example.exporter.repository.CompanyRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@Transactional
public class CompanyService {

    @Autowired
    private CompanyRepository companyRepository;

    public List<Company> getAllCompanies(){return companyRepository.findAll();}

    public Company getCompanyById(long id){return companyRepository.findById(id).get();}

    public Company saveCompany(Company company){return companyRepository.save(company);}

    public Company updateCompany(Company company){return companyRepository.save(company);}

    public void deleteCompany(long id){companyRepository.deleteById(id);}

}
