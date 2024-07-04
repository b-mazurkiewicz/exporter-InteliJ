package com.example.exporter;

import com.example.exporter.model.Address;
import com.example.exporter.model.User;
import com.example.exporter.repository.AddressRepository;
import com.example.exporter.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataLoader implements CommandLineRunner {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    @Autowired
    public DataLoader(AddressRepository addressRepository, UserRepository userRepository) {
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
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
            Address address1 = addressRepository.findByStreet("123 Main St"); // Przykładowe pobranie adresu
            Address address2 = addressRepository.findByStreet("456 Maple St");

            User user1 = new User(null, "John", "Doe", "john.doe@example.com", address1);
            User user2 = new User(null, "Jane", "Smith", "jane.smith@example.com", address2);

            userRepository.save(user1);
            userRepository.save(user2);
        }
    }
}
