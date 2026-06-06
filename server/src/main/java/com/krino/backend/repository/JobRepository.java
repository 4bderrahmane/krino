package com.krino.backend.repository;

import com.krino.backend.entity.Department;
import com.krino.backend.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Date;
import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long>
{

    List<Job> findByTitle(String title);

    List<Job> findByDepartment(Department department);

    List<Job> findByStatus(Job.JobStatus status);

    List<Job> findByApplyingDeadlineAfter(Date currentDate);

    List<Job> findByStatusAndApplyingDeadlineAfter(Job.JobStatus status, Date currentDate);

    List<Job> findByDepartmentAndApplyingDeadlineAfter(Department department, Date currentDate);

    List<Job> findByDepartmentAndStatusAndApplyingDeadlineAfter(Department department, Job.JobStatus status, Date currentDate);

    List<Job> findByTitleAndApplyingDeadlineAfter(String title, Date currentDate);

    List<Job> findByTitleAndStatusAndApplyingDeadlineAfter(String title, Job.JobStatus status, Date currentDate);

    List<Job> findByTitleAndDepartmentAndApplyingDeadlineAfter(String title, Department department, Date currentDate);

    List<Job> findByTitleAndDepartmentAndStatusAndApplyingDeadlineAfter(String title, Department department, Job.JobStatus status, Date currentDate);

}
