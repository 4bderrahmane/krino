package com.jesa.interviewslotmanager.repository;

import com.jesa.interviewslotmanager.entity.Department;
import com.jesa.interviewslotmanager.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Date;
import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long> {

    List<Job> findByDepartment(Department department);
    List<Job> findByStatus(Job.JobStatus status);
    List<Job> findByApplyingDeadlineAfter(Date currentDate);
    List<Job> findByStatusAndApplyingDeadlineAfter(Job.JobStatus status, Date currentDate);

}
