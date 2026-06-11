package com.krino.backend.repository;

import com.krino.backend.entity.Application;
import com.krino.backend.entity.ApplicationStatus;
import com.krino.backend.entity.Job;
import com.krino.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApplicationRepository extends JpaRepository<Application, Long>
{
    Optional<Application> findByPublicId(UUID publicId);

    List<Application> findByCandidate(User candidate);

    List<Application> findByJob(Job job);

    List<Application> findByStatus(ApplicationStatus status);

    Optional<Application> findByJobAndCandidate(Job job, User candidate);
}
