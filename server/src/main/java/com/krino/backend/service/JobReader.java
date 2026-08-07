package com.krino.backend.service;

import com.krino.backend.dto.common.PageResponse;
import com.krino.backend.dto.job.JobResponseDTO;
import com.krino.backend.entity.Job;
import com.krino.backend.entity.enums.JobStatus;
import com.krino.backend.exception.ResourceNotFoundException;
import com.krino.backend.mapper.JobMapper;
import com.krino.backend.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.krino.backend.configuration.CachingConfiguration.JOBS_CACHE;
import static com.krino.backend.configuration.CachingConfiguration.JOB_LISTINGS_CACHE;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JobReader {

    private final JobRepository jobRepository;
    private final JobMapper jobMapper;

    @Cacheable(cacheNames = JOBS_CACHE, key = "#publicId")
    public JobResponseDTO readByPublicId(UUID publicId) {
        Job job = jobRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Job with public ID '%s' not found.", publicId)));
        return jobMapper.toResponse(job);
    }

    @Cacheable(
            cacheNames = JOB_LISTINGS_CACHE,
            key = "'open:' + #pageable.pageSize + ':' + #pageable.sort",
            condition = "#pageable.pageNumber == 0")
    public PageResponse<JobResponseDTO> readOpenListing(Pageable pageable) {
        return PageResponse.from(jobRepository.findByStatus(JobStatus.OPEN, pageable), jobMapper::toResponse);
    }
}
