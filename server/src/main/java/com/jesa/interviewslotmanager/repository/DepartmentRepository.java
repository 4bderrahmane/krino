package com.jesa.interviewslotmanager.repository;

import com.jesa.interviewslotmanager.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long>
{
    Optional<Department> findByName(String name);
}
