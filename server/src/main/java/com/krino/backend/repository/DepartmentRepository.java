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
            SELECT d FROM Department d
            WHERE EXISTS (
                SELECT 1 FROM Job j
                WHERE j.department = d
                  AND j.status = :status
            )
            ORDER BY d.name
            """)
    List<Department> findHavingJobWithStatus(@Param("status") JobStatus status);
}
