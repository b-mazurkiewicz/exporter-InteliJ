package com.example.exporter.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

@Entity
@Table(name = "Address")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "street")
    private String street;

    @Column(name = "city")
    private String city;

    @Column(name = "zip_code")
    private String zipCode;

    @Column(name = "country")
    private String country;

    @OneToMany(mappedBy = "address")
    private Set<User> users;

    

    // Konstruktor z argumentami (bez id i users)
    public Address(Long Id, String street, String city, String zipCode, String country) {
        this.id = Id;
        this.street = street;
        this.city = city;
        this.zipCode = zipCode;
        this.country = country;
    }
}