package com.example.exporter.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class ViewService {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public ViewService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void createViews() {
        String createViewSql = "CREATE VIEW IF NOT EXISTS UserAddressView AS " +
                "SELECT u.id, u.first_name, u.last_name, u.email, a.street, a.city, a.zip_code, a.country " +
                "FROM Excel_Data_Set u " +
                "JOIN Address a ON u.address_id = a.id";
        jdbcTemplate.execute(createViewSql);
    }
}
