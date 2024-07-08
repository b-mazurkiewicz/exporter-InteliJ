package com.example.exporter.model;

import jakarta.persistence.*;
import lombok.*;

@Entity(name="Excel_Data_Set")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "email")
    private String email;

    @ManyToOne
    @JoinColumn(name = "address_id")
    private Address address;

    @ManyToOne
    @JoinColumn(name = "company_id")
    private Company company;

    // Konstruktor z argumentami (bez id)
    public User(String firstName, String lastName, String email, Address address, Company company) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.address = address;
        this.company= company;
    }
}