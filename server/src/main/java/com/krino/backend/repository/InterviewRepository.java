package com.krino.backend.repository;

import com.krino.backend.entity.Interview;
import com.krino.backend.entity.Job;
import com.krino.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InterviewRepository extends JpaRepository<Interview, Long>
{
    List<Interview> findByJob(Job job);

    Optional<Interview> findByPublicId(UUID publicId);

    boolean existsByJob(Job job);

    boolean existsByCandidate(User candidate);

    boolean existsByInterviewer(User interviewer);
}
