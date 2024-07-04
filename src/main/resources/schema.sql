-- Tworzenie tabeli Address
CREATE TABLE IF NOT EXISTS Address (
                                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                       street VARCHAR(255),
                                       city VARCHAR(255),
                                       zip_code VARCHAR(255),
                                       country VARCHAR(255)
);

-- Tworzenie tabeli User
CREATE TABLE IF NOT EXISTS Excel_Data_Set (
                                              id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                              first_name VARCHAR(255),
                                              last_name VARCHAR(255),
                                              email VARCHAR(255),
                                              address_id BIGINT,
                                              FOREIGN KEY (address_id) REFERENCES Address(id)
);

