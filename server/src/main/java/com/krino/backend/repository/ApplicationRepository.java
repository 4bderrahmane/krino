package com.krino.backend.repository;

import com.krino.backend.entity.Application;
import com.krino.backend.entity.enums.ApplicationStatus;
import com.krino.backend.entity.Job;
import com.krino.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {
    Optional<Application> findByPublicId(UUID publicId);

    List<Application> findByCandidate(User candidate);

    Page<Application> findByCandidate(User candidate, Pageable pageable);

    @Query("""
            SELECT a FROM Application a JOIN a.candidate c JOIN a.job j
            WHERE :search = ''
               OR lower(c.firstName) LIKE concat('%', :search, '%')
               OR lower(c.lastName) LIKE concat('%', :search, '%')
               OR lower(concat(c.firstName, ' ', c.lastName)) LIKE concat('%', :search, '%')
               OR lower(c.email) LIKE concat('%', :search, '%')
               OR lower(j.title) LIKE concat('%', :search, '%')
            """)
    Page<Application> searchForScheduling(@Param("search") String search, Pageable pageable);

    List<Application> findByJob(Job job);

    List<Application> findByStatus(ApplicationStatus status);

    Optional<Application> findByJobAndCandidate(Job job, User candidate);

    boolean existsByJobAndCandidate(Job job, User candidate);

    boolean existsByJob_PublicIdAndCandidate_PublicId(UUID jobPublicId, UUID candidatePublicId);

    boolean existsByCandidate(User candidate);

    boolean existsByJob(Job job);
}
