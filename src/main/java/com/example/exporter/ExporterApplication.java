package com.example.exporter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ExporterApplication {

	public static void main(String[] args) {
		SpringApplication.run(ExporterApplication.class, args);
	}
	CREATE TABLE tutorials_tbl (
			id INT NOT NULL,
			title VARCHAR(50) NOT NULL,
	author VARCHAR(20) NOT NULL,
	submission_date DATE
);
}
