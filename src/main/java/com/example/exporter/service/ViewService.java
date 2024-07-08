package com.example.exporter.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ViewService {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public ViewService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void createViews() {
        String createViewSql = "CREATE OR REPLACE VIEW UserAddressCompanyView AS " +
                "SELECT u.id AS user_id, u.first_name, u.last_name, u.email, " +
                "a.street AS address_street, a.city AS address_city, a.zip_code AS address_zip_code, a.country AS address_country, " +
                "c.company_name, c.city AS company_city, c.nip AS company_nip, c.country AS company_country " +
                "FROM Excel_Data_Set u " +
                "LEFT JOIN Address a ON u.address_id = a.id " +
                "LEFT JOIN Company c ON u.company_id = c.id";
        jdbcTemplate.execute(createViewSql);
    }
    public List<String> getTablesAndViews() {
        String sql = "SELECT table_name FROM information_schema.tables WHERE table_schema='PUBLIC' " +
                "UNION ALL " +
                "SELECT table_name FROM information_schema.views WHERE table_schema='PUBLIC'";
        return jdbcTemplate.queryForList(sql, String.class);
    }
}