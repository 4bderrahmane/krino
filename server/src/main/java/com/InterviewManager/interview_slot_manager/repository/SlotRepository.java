package com.InterviewManager.interview_slot_manager.repository;

import com.InterviewManager.interview_slot_manager.entity.Slot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Date;
import java.util.List;

public interface SlotRepository extends JpaRepository<Slot, Long> {
//    List<Slot> findByInterview(Interview interview);
//    List<Slot> findByInterviewer(User interviewer);
//    List<Slot> findByCandidate(User candidate);
    List<Slot> findByIsAvailableTrue();
    List<Slot> findByInterviewDateAndIsAvailableTrue(Date interviewDate);
}
