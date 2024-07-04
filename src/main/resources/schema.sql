-- Tworzenie tabeli Address
CREATE TABLE IF NOT EXISTS Address (
                                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                       street VARCHAR(255),
                                       city VARCHAR(255),
                                       zip_code VARCHAR(255),
                                       country VARCHAR(255)
);

-- Tworzenie tabeli User
CREATE TABLE IF NOT EXISTS ExcelDataSet (
                                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                            first_name VARCHAR(255),
                                            last_name VARCHAR(255),
                                            email VARCHAR(255),
                                            address_id BIGINT,
                                            FOREIGN KEY (address_id) REFERENCES Address(id)
);

-- Dodanie danych do tabeli Address
INSERT INTO Address (id, street, city, zip_code, country) VALUES (1, '123 Main St', 'Anytown', '12345', 'USA');
INSERT INTO Address (id, street, city, zip_code, country) VALUES (2, '456 Maple St', 'Othertown', '67890', 'USA');

-- Dodanie danych do tabeli User
INSERT INTO ExcelDataSet (id, first_name, last_name, email, address_id) VALUES (1, 'John', 'Doe', 'john.doe@example.com', 1);
INSERT INTO ExcelDataSet (id, first_name, last_name, email, address_id) VALUES (2, 'Jane', 'Smith', 'jane.smith@example.com', 2);
