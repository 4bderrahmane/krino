package com.krino.backend.repository;

import com.krino.backend.entity.Interview;
import com.krino.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InterviewRepository extends JpaRepository<Interview, Long> {
    Optional<Interview> findByPublicId(UUID publicId);

    Page<Interview> findByApplication_CandidateOrInterviewer(User candidate, User interviewer, Pageable pageable);

    boolean existsByInterviewer(User interviewer);
}
