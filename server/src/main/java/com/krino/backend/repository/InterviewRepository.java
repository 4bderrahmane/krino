package com.krino.backend.repository;

import com.krino.backend.entity.Interview;
import com.krino.backend.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InterviewRepository extends JpaRepository<Interview, Long>
{
    List<Interview> findByJob(Job job);

    Optional<Interview> findByPublicId(UUID publicId);
}
