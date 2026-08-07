package com.krino.backend.repository;

import com.krino.backend.entity.Department;
import com.krino.backend.entity.enums.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
    Optional<Department> findByName(String name);

    Optional<Department> findByPublicId(UUID publicId);

    @Query("""
            select d from Department d
            where exists (select 1 from Job j where j.department = d and j.status = :status)
            order by d.name
            """)
    List<Department> findHavingJobWithStatus(@Param("status") JobStatus status);
}
