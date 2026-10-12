package com.krino.backend.repository;

import com.krino.backend.entity.Interview;
import com.krino.backend.entity.Slot;
import com.krino.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SlotRepository extends JpaRepository<Slot, Long> {
    Optional<Slot> findByPublicId(UUID publicId);

    Page<Slot> findByInterviewer(User interviewer, Pageable pageable);

    @Query("""
            SELECT s FROM Slot s JOIN s.interviewer i
            WHERE s.available = TRUE
              AND (:search = ''
                OR LOWER(i.firstName) LIKE CONCAT('%', :search, '%')
                OR LOWER(i.lastName) LIKE CONCAT('%', :search, '%')
                OR LOWER(concat(i.firstName, ' ', i.lastName)) LIKE CONCAT('%', :search, '%')
                OR LOWER(i.email) LIKE CONCAT('%', :search, '%'))
            """)
    Page<Slot> searchAvailableForScheduling(@Param("search") String search, Pageable pageable);

    List<Slot> findByInterview(Interview interview);

    List<Slot> findByAvailableTrue();

    boolean existsByInterviewer(User interviewer);
}
