package com.example.exporter;
/*
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataLoader implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Override
    public void run(String... args) throws Exception {
        Address address1 = new Address(null, "123 Main St", "Springfield", "12345", "USA");
        Address address2 = new Address(null, "456 Elm St", "Springfield", "12345", "USA");
        Address address3 = new Address(null, "789 Oak St", "Springfield", "12345", "USA");
        Address address4 = new Address(null, "101 Maple St", "Springfield", "12345", "USA");
        Address address5 = new Address(null, "202 Pine St", "Springfield", "12345", "USA");

        address1 = addressRepository.save(address1);
        address2 = addressRepository.save(address2);
        address3 = addressRepository.save(address3);
        address4 = addressRepository.save(address4);
        address5 = addressRepository.save(address5);

        userRepository.save(new User(null, "John", "Doe", "john.doe@example.com", address1));
        userRepository.save(new User(null, "Jane", "Smith", "jane.smith@example.com", address2));
        userRepository.save(new User(null, "Robert", "Brown", "robert.brown@example.com", address3));
        userRepository.save(new User(null, "Emily", "Davis", "emily.davis@example.com", address4));
        userRepository.save(new User(null, "Michael", "Wilson", "michael.wilson@example.com", address5));
    }
}
*/