package com.krino.backend.repository;

import com.krino.backend.entity.Interview;
import com.krino.backend.entity.Slot;
import com.krino.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SlotRepository extends JpaRepository<Slot, Long> {
    Optional<Slot> findByPublicId(UUID publicId);

    List<Slot> findByInterview(Interview interview);

    List<Slot> findByAvailableTrue();

    List<Slot> findByInterviewDateAndAvailableTrue(Date interviewDate);

    boolean existsByInterviewer(User interviewer);
}
