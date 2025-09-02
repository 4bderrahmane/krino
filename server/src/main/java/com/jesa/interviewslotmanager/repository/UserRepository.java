package com.jesa.interviewslotmanager.repository;

import com.jesa.interviewslotmanager.entity.User;
import com.jesa.interviewslotmanager.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);

    List<User> findByRolesContaining(UserRole role);
    List<User> findByIsApprovedFalse();

    boolean existsByEmail(String email);
    boolean existsByUsername(String username);

}
