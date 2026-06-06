package com.krino.backend.repository;

import com.krino.backend.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long>
{
    Optional<Department> findByName(String name);
}
