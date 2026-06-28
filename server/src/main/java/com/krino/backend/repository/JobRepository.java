package com.krino.backend.repository;

import com.krino.backend.entity.Department;
import com.krino.backend.entity.Job;
import com.krino.backend.entity.enums.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobRepository extends JpaRepository<Job, Long> {

    Optional<Job> findByPublicId(UUID publicId);

    List<Job> findByTitle(String title);

    List<Job> findByDepartment(Department department);

    List<Job> findByStatus(JobStatus status);

}
