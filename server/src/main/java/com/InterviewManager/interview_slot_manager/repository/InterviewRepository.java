package com.InterviewManager.interview_slot_manager.repository;

import com.InterviewManager.interview_slot_manager.entity.Interview;
import com.InterviewManager.interview_slot_manager.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InterviewRepository extends JpaRepository<Interview, Long> {
    List<Interview> findByJob(Job job);
}
