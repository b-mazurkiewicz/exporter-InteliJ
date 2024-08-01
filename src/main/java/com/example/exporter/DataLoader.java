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
            Address address3 = new Address(null, "789 Oak St", "Bigcity", "54321", "USA");
            Address address4 = new Address(null, "101 Pine St", "Smallville", "98765", "USA");
            Address address5 = new Address(null, "202 Birch St", "Middletown", "11111", "USA");

            addressRepository.save(address1);
            addressRepository.save(address2);
            addressRepository.save(address3);
            addressRepository.save(address4);
            addressRepository.save(address5);
        }

        // Sprawdzenie i dodanie danych do tabeli Company, jeśli jest pusta
        if (companyRepository.count() == 0) {
            Company company1 = new Company(null, "Company One", "Big City", "NIP123456789", "USA", new HashSet<>());
            Company company2 = new Company(null, "Company Two", "Small Town", "NIP987654321", "USA", new HashSet<>());
            Company company3 = new Company(null, "Company Three", "Midtown", "NIP111111111", "USA", new HashSet<>());
            Company company4 = new Company(null, "Company Four", "Anytown", "NIP222222222", "USA", new HashSet<>());
            Company company5 = new Company(null, "Company Five", "Othertown", "NIP333333333", "USA", new HashSet<>());

            companyRepository.save(company1);
            companyRepository.save(company2);
            companyRepository.save(company3);
            companyRepository.save(company4);
            companyRepository.save(company5);
        }

        // Sprawdzenie i dodanie danych do tabeli User, jeśli jest pusta
        if (userRepository.count() == 0) {
            // Pobranie adresów z bazy danych na podstawie nazwy ulicy
            Address address1 = addressRepository.findByStreet("123 Main St");
            Address address2 = addressRepository.findByStreet("456 Maple St");
            Address address3 = addressRepository.findByStreet("789 Oak St");
            Address address4 = addressRepository.findByStreet("101 Pine St");
            Address address5 = addressRepository.findByStreet("202 Birch St");

            // Pobranie firm z bazy danych na podstawie NIP
            Company company1 = companyRepository.findByNip("NIP123456789");
            Company company2 = companyRepository.findByNip("NIP987654321");
            Company company3 = companyRepository.findByNip("NIP111111111");
            Company company4 = companyRepository.findByNip("NIP222222222");
            Company company5 = companyRepository.findByNip("NIP333333333");

            // Utworzenie użytkowników z przypisanymi adresami i firmami
            User user1 = new User("John", "Doe", "john.doe@example.com", address1, company1);
            User user2 = new User("Jane", "Smith", "jane.smith@example.com", address2, company2);
            User user3 = new User("Alice", "Johnson", "alice.johnson@example.com", address3, company3);
            User user4 = new User("Bob", "Brown", "bob.brown@example.com", address4, company4);
            User user5 = new User("Charlie", "Davis", "charlie.davis@example.com", address5, company5);

            // Zapis użytkowników do bazy danych
            userRepository.save(user1);
            userRepository.save(user2);
            userRepository.save(user3);
            userRepository.save(user4);
            userRepository.save(user5);
        }
    }
}