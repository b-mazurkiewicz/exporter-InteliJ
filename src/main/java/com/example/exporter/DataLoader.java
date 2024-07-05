package com.example.exporter;

import com.example.exporter.model.Address;
import com.example.exporter.model.User;
import com.example.exporter.repository.AddressRepository;
import com.example.exporter.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;



@Component
public class DataLoader implements CommandLineRunner {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public DataLoader(AddressRepository addressRepository, UserRepository userRepository, JdbcTemplate jdbcTemplate) {
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        // Wstawianie danych do tabeli Address, jeśli tabela jest pusta
        if (addressRepository.count() == 0) {
            Address address1 = new Address(null, "123 Main St", "Anytown", "12345", "USA");
            Address address2 = new Address(null, "456 Maple St", "Othertown", "67890", "USA");

            addressRepository.save(address1);
            addressRepository.save(address2);
        }

        // Wstawianie danych do tabeli Excel_Data_Set, jeśli tabela jest pusta
        if (userRepository.count() == 0) {
            Address address1 = addressRepository.findByStreet("123 Main St");
            Address address2 = addressRepository.findByStreet("456 Maple St");

            User user1 = new User(null, "John", "Doe", "john.doe@example.com", address1);
            User user2 = new User(null, "Jane", "Smith", "jane.smith@example.com", address2);

            userRepository.save(user1);
            userRepository.save(user2);
        }

        // Tworzenie widoków w bazie danych
        //createViews();
    }

    /*
    private void createViews() {
        String createViewSql = "CREATE VIEW IF NOT EXISTS UserAddressView AS " +
                "SELECT u.id, u.first_name, u.last_name, u.email, a.street, a.city, a.zip_code, a.country " +
                "FROM Excel_Data_Set u " +
                "JOIN Address a ON u.address_id = a.id";

        jdbcTemplate.execute(createViewSql);
    }

     */
}

