package com.pro24_group4.pro24_group4.repository;

import com.pro24_group4.pro24_group4.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoleManagementRepository extends JpaRepository<Role, Long> {

    List<Role> findAllByDeletedAtIsNull();

    Optional<Role> findByIdAndDeletedAtIsNull(Long id);

    Optional<Role> findByName(String name);

    Optional<Role> findByNameAndDeletedAtIsNull(String name);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);
}
