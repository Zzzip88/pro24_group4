package com.pro24_group4.pro24_group4.repository;

import com.pro24_group4.pro24_group4.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);
}