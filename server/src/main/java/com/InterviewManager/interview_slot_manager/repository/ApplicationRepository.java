package com.InterviewManager.interview_slot_manager.repository;

import com.InterviewManager.interview_slot_manager.entity.Application;
import com.InterviewManager.interview_slot_manager.entity.ApplicationStatus;
import com.InterviewManager.interview_slot_manager.entity.Job;
import com.InterviewManager.interview_slot_manager.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Long> {
    List<Application> findByJobSeeker(User jobSeeker);
    List<Application> findByJob(Job job);
    List<Application> findByStatus(ApplicationStatus status);
    Optional<Application> findByJobAndJobSeeker(Job job, User jobSeeker);
}
