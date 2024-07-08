package com.example.exporter.service;

import com.example.exporter.model.Address;
import com.example.exporter.repository.AddressRepository;
import com.example.exporter.repository.UserRepository;
import com.example.exporter.model.User;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    @Autowired
    private final UserRepository userRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional
    public User saveUser(User user) {
        return userRepository.save(user);
    }

    @Transactional
    public User getUserById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    @Transactional
    public void deleteUserById(Long id) {
        userRepository.deleteById(id);
    }

    @Transactional
    public User getUserWithAddressById(Long id) {
        return userRepository.findUserWithAddressById(id);
    }

    public Address createAddress(Address address) {
        return addressRepository.save(address);
    }
}