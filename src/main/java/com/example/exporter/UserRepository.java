package com.example.exporter;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @Query("SELECT u FROM Excel_Data_Set u LEFT JOIN FETCH u.address WHERE u.id = :userId")
    User findUserWithAddressById(@Param("userId") Long userId);
}