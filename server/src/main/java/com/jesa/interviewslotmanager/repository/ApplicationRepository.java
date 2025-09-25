package com.jesa.interviewslotmanager.repository;

import com.jesa.interviewslotmanager.entity.Application;
import com.jesa.interviewslotmanager.entity.ApplicationStatus;
import com.jesa.interviewslotmanager.entity.Job;
import com.jesa.interviewslotmanager.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Long>
{
    List<Application> findByCandidate(User candidate);

    List<Application> findByJob(Job job);

    List<Application> findByStatus(ApplicationStatus status);

    Optional<Application> findByJobAndCandidate(Job job, User candidate);
}
