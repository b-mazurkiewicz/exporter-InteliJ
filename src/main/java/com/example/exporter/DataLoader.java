package com.example.exporter;

import com.example.exporter.model.Address;
import com.example.exporter.model.Company;
import com.example.exporter.model.User;
import com.example.exporter.repository.AddressRepository;
import com.example.exporter.repository.CompanyRepository;
import com.example.exporter.repository.UserRepository;
import com.example.exporter.service.ViewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.HashSet;

@Component
public class DataLoader implements CommandLineRunner {

    private final ViewService viewService;
    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;

    @Autowired
    public DataLoader(ViewService viewService, AddressRepository addressRepository,
                      UserRepository userRepository, CompanyRepository companyRepository) {
        this.viewService = viewService;
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
    }

    @Override
    public void run(String... args) {
        // Tworzenie lub aktualizacja widoków
        viewService.createViews();

        // Sprawdzenie i dodanie danych do tabeli Address, jeśli jest pusta
        if (addressRepository.count() == 0) {
            Address address1 = new Address(null, "123 Main St", "Anytown", "12345", "USA");
            Address address2 = new Address(null, "456 Maple St", "Othertown", "67890", "USA");

            addressRepository.save(address1);
            addressRepository.save(address2);
        }

        // Sprawdzenie i dodanie danych do tabeli Company, jeśli jest pusta
        if (companyRepository.count() == 0) {
            Company company1 = new Company(null, "Company One", "Big City", "NIP123456789", "USA", new HashSet<>());
            Company company2 = new Company(null, "Company Two", "Small Town", "NIP987654321", "USA", new HashSet<>());

            companyRepository.save(company1);
            companyRepository.save(company2);
        }

        // Sprawdzenie i dodanie danych do tabeli User, jeśli jest pusta
        if (userRepository.count() == 0) {
            // Pobranie adresów z bazy danych na podstawie nazwy ulicy
            Address address1 = addressRepository.findByStreet("123 Main St");
            Address address2 = addressRepository.findByStreet("456 Maple St");

            // Pobranie firm z bazy danych na podstawie NIP
            Company company1 = companyRepository.findByNip("NIP123456789");
            Company company2 = companyRepository.findByNip("NIP987654321");

            // Utworzenie użytkowników z przypisanymi adresami i firmami
            User user1 = new User("John", "Doe", "john.doe@example.com", address1, company1);
            User user2 = new User("Jane", "Smith", "jane.smith@example.com", address2, company2);

            // Zapis użytkowników do bazy danych
            userRepository.save(user1);
            userRepository.save(user2);
        }
    }
}