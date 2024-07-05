package com.example.exporter.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Companies")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Company {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company name")
    private String street;

    @Column(name = "city")
    private String city;

    @Column(name = "NIP")
    private String zipCode;

    @Column(name = "country")
    private String country;

}
