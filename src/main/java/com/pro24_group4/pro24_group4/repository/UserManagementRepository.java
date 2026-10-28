package com.pro24_group4.pro24_group4.repository;

import com.pro24_group4.pro24_group4.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserManagementRepository extends JpaRepository<User, Long> {

    List<User> findAllByDeletedAtIsNull();

    Optional<User> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByUsername(String username);

    boolean existsByUsernameAndIdNot(String username, Long id);

    Optional<User> findByUsernameAndDeletedAtIsNull(String username);
}
