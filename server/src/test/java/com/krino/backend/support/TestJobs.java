package com.krino.backend.support;

import com.krino.backend.entity.Department;
import com.krino.backend.entity.Job;
import com.krino.backend.entity.enums.ContractType;
import com.krino.backend.entity.enums.EmploymentType;
import com.krino.backend.entity.enums.JobStatus;
import com.krino.backend.entity.enums.RemotePolicy;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Builds {@link Job} aggregates for tests. {@code Job} has no setters — it is
 * created through its constructor and behaviour methods — so tests go through
 * this helper instead of poking fields directly. For the few unit tests that
 * need a specific public id or lifecycle state without driving the whole
 * lifecycle, the {@code with*} helpers force internal fields reflectively.
 */
public final class TestJobs {

    private TestJobs() {
    }

    public static Job draft(String title) {
        return draft(new Department(), title);
    }

    public static Job draft(Department department, String title) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Job job = new Job("JOB-" + suffix, "job-" + suffix, department, title,
                EmploymentType.FULL_TIME, ContractType.PERMANENT, RemotePolicy.REMOTE);
        job.updateContent(title, "Build APIs");
        job.updateTimeline(Instant.now().plus(30, ChronoUnit.DAYS), null);
        return job;
    }

    public static Job open(Department department, String title) {
        Job job = draft(department, title);
        job.publish(Instant.now());
        return job;
    }

    public static Job withPublicId(Job job, UUID publicId) {
        ReflectionTestUtils.setField(job, "publicId", publicId);
        return job;
    }

    public static Job withState(Job job, UUID publicId, JobStatus status, Instant applicationDeadline) {
        if (publicId != null) {
            ReflectionTestUtils.setField(job, "publicId", publicId);
        }
        if (status != null) {
            ReflectionTestUtils.setField(job, "status", status);
        }
        ReflectionTestUtils.setField(job, "applicationDeadline", applicationDeadline);
        return job;
    }
}
