package com.krino.backend.repository;

import com.krino.backend.entity.User;
import com.krino.backend.entity.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, Long>
{

    Optional<User> findByPublicId(UUID publicId);

    Optional<User> findByEmail(String email);

    List<User> findByRolesContaining(UserRole role);

    List<User> findByIsApprovedFalse();

    Page<User> findByIsApprovedFalse(Pageable pageable);

    boolean existsByEmail(String email);

}
