package com.InterviewManager.interview_slot_manager.repository;

import com.InterviewManager.interview_slot_manager.entity.Department;
import com.InterviewManager.interview_slot_manager.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface JobRepository extends JpaRepository<Job, Long> {

    List<Job> findByDepartment(Department department);
    List<Job> findByStatus(Job.JobStatus status);
    List<Job> findByApplyingDeadlineAfter(Date currentDate);
    List<Job> findByStatusAndApplyingDeadlineAfter(Job.JobStatus status, Date currentDate);

}
