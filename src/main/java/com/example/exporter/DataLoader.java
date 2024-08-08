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
import java.util.Optional;

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

        // Dodawanie lub aktualizowanie danych do tabeli Address
        addOrUpdateAddress(new Address(null, "123 Main St", "Anytown", "12345", "USA"));
        addOrUpdateAddress(new Address(null, "456 Maple St", "Othertown", "67890", "Poland"));
        addOrUpdateAddress(new Address(null, "789 Elm St", "Sometown", "13579", "USA"));
        addOrUpdateAddress(new Address(null, "101 Oak St", "Someothertown", "24680", "Poland"));
        addOrUpdateAddress(new Address(null, "202 Pine St", "New City", "54321", "USA"));
        addOrUpdateAddress(new Address(null, "303 Cedar St", "Old Town", "98765", "Poland"));
        addOrUpdateAddress(new Address(null, "404 Birch St", "Capitol City", "11223", "USA"));
        addOrUpdateAddress(new Address(null, "505 Walnut St", "Lakeside", "33445", "Poland"));

        // Dodawanie lub aktualizowanie danych do tabeli Company
        addOrUpdateCompany(new Company(null, "Company One", "Big City", "NIP123456789", "USA", new HashSet<>()));
        addOrUpdateCompany(new Company(null, "Company Two", "Small Town", "NIP987654321", "USA", new HashSet<>()));
        addOrUpdateCompany(new Company(null, "Company Three", "Medium Town", "NIP555555555", "USA", new HashSet<>()));
        addOrUpdateCompany(new Company(null, "Company Four", "Tiny Town", "NIP666666666", "USA", new HashSet<>()));
        addOrUpdateCompany(new Company(null, "Company Five", "Major City", "NIP777777777", "USA", new HashSet<>()));
        addOrUpdateCompany(new Company(null, "Company Six", "Metro City", "NIP888888888", "USA", new HashSet<>()));

        // Dodawanie lub aktualizowanie danych do tabeli User
        addOrUpdateUser(new User("John", "Doe", "john.doe@example.com", addressRepository.findByStreet("123 Main St"), companyRepository.findByNip("NIP123456789")));
        addOrUpdateUser(new User("Jane", "Smith", "jane.smith@example.com", addressRepository.findByStreet("456 Maple St"), companyRepository.findByNip("NIP987654321")));
        addOrUpdateUser(new User("Alice", "Johnson", "alice.johnson@example.com", addressRepository.findByStreet("789 Elm St"), companyRepository.findByNip("NIP555555555")));
        addOrUpdateUser(new User("Bob", "Brown", "bob.brown@example.com", addressRepository.findByStreet("101 Oak St"), companyRepository.findByNip("NIP666666666")));
        addOrUpdateUser(new User("Charlie", "Davis", "charlie.davis@example.com", addressRepository.findByStreet("202 Pine St"), companyRepository.findByNip("NIP777777777")));
        addOrUpdateUser(new User("Diana", "Evans", "diana.evans@example.com", addressRepository.findByStreet("303 Cedar St"), companyRepository.findByNip("NIP888888888")));
        addOrUpdateUser(new User("Eve", "Martin", "eve.martin@example.com", addressRepository.findByStreet("404 Birch St"), companyRepository.findByNip("NIP123456789")));
        addOrUpdateUser(new User("Frank", "Wilson", "frank.wilson@example.com", addressRepository.findByStreet("505 Walnut St"), companyRepository.findByNip("NIP987654321")));
    }

    private void addOrUpdateAddress(Address address) {
        Address existingAddress = addressRepository.findByStreet(address.getStreet());
        if (existingAddress != null) {
            existingAddress.setCity(address.getCity());
            existingAddress.setZipCode(address.getZipCode());
            existingAddress.setCountry(address.getCountry());
            addressRepository.save(existingAddress);
        } else {
            addressRepository.save(address);
        }
    }

    private void addOrUpdateCompany(Company company) {
        Company existingCompany = companyRepository.findByNip(company.getNip());
        if (existingCompany != null) {
            existingCompany.setNip(company.getNip());
            existingCompany.setCity(company.getCity());
            existingCompany.setCountry(company.getCountry());
            companyRepository.save(existingCompany);
        } else {
            companyRepository.save(company);
        }
    }

    private void addOrUpdateUser(User user) {
        Optional<User> existingUser = userRepository.findByEmail(user.getEmail());
        if (existingUser.isPresent()) {
            User updatedUser = existingUser.get();
            updatedUser.setFirstName(user.getFirstName());
            updatedUser.setLastName(user.getLastName());
            updatedUser.setAddress(user.getAddress());
            updatedUser.setCompany(user.getCompany());
            userRepository.save(updatedUser);
        } else {
            userRepository.save(user);
        }
    }
}
