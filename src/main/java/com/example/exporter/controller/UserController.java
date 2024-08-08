package com.example.exporter.controller;

import com.example.exporter.model.Address;
import com.example.exporter.repository.AddressRepository;
import com.example.exporter.repository.UserRepository;
import com.example.exporter.service.AddressService;
import com.example.exporter.service.UserService;
import com.example.exporter.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private UserRepository userRepository;


    @GetMapping
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/{id}")
    public User getUserById(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    @PostMapping
    public User createUser(@RequestBody User user) {
        //sprawdzamy czy istnieje adres
        if (user.getAddress() != null && user.getAddress().getId() != null) {
            Address address = addressRepository.findById(user.getAddress().getId())
                    .orElseThrow(() -> new RuntimeException("Address not found"));
            user.setAddress(address);
        }
        return userRepository.save(user);
    }

    @PutMapping("/{id}")
    public User updateUser(@PathVariable Long id, @RequestBody User user){
        user.setId(id);
        return userService.saveUser(user);
    }

    @DeleteMapping("/{id}")
    public void deleteUser(@PathVariable Long id) {
        userService.deleteUserById(id);
    }
}