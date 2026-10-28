package com.pro24_group4.pro24_group4.repository;

import com.pro24_group4.pro24_group4.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PermissionManagementRepository extends JpaRepository<Permission, Long> {

    List<Permission> findAllByDeletedAtIsNull();

    Optional<Permission> findByIdAndDeletedAtIsNull(Long id);

    Optional<Permission> findByName(String name);

    Optional<Permission> findByNameAndDeletedAtIsNull(String name);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);
}
