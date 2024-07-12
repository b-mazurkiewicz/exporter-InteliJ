package com.example.exporter.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.Set;


@Entity
@Table(name = "Company")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Company {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "city")
    private String city;

    @Column(name = "NIP", unique = true)
    private String nip;

    @Column(name = "country")
    private String country;

    @OneToMany(mappedBy = "company")
    private Set<User> users;
}
