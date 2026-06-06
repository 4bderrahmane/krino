package com.krino.backend.repository;

import com.krino.backend.entity.User;
import com.krino.backend.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>
{

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    List<User> findByRolesContaining(UserRole role);

    List<User> findByIsApprovedFalse();

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

}
