package com.jesa.interviewslotmanager.repository;

import com.jesa.interviewslotmanager.entity.Interview;
import com.jesa.interviewslotmanager.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InterviewRepository extends JpaRepository<Interview, Long> {
    List<Interview> findByJob(Job job);
}
