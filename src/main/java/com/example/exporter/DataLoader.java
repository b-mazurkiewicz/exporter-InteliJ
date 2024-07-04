package com.example.exporter;

import com.example.exporter.objects.User;
import com.example.exporter.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataLoader implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Override
    public void run(String... args) throws Exception {
        userRepository.save(new User(null, "John", "Doe", "john.doe@example.com"));
        userRepository.save(new User(null, "Jane", "Smith", "jane.smith@example.com"));
        userRepository.save(new User(null, "Robert", "Brown", "robert.brown@example.com"));
        userRepository.save(new User(null, "Emily", "Davis", "emily.davis@example.com"));
        userRepository.save(new User(null, "Michael", "Wilson", "michael.wilson@example.com"));
    }
}
