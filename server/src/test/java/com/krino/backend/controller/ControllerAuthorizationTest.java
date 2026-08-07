package com.krino.backend.controller;

import com.krino.backend.dto.application.ApplicationResponseDTO;
import com.krino.backend.dto.department.DepartmentResponseDTO;
import com.krino.backend.dto.interview.InterviewResponseDTO;
import com.krino.backend.dto.job.JobResponseDTO;
import com.krino.backend.dto.slot.SlotResponseDTO;
import com.krino.backend.entity.enums.Permission;
import com.krino.backend.entity.enums.UserRole;
import com.krino.backend.service.ApplicationService;
import com.krino.backend.service.DepartmentService;
import com.krino.backend.service.InterviewService;
import com.krino.backend.service.JobService;
import com.krino.backend.service.SlotService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Verifies that the @PreAuthorize annotations on the entity controllers enforce the intended
 * role -> permission matrix. The controllers are loaded as real beans behind a method-security
 * proxy with mocked services, so the authority strings in the annotations are checked against the
 * authorities each role actually grants via {@link UserRole#getAuthorities()}.
 *
 * Intended matrix (C=create, R=read, U=update, D=delete):
 *   Job          ADMIN:CRUD  HR:CRUD   INTERVIEWER:R    CANDIDATE:item R
 *   Slot         ADMIN:CRUD  HR:CRUD   INTERVIEWER:CRUD CANDIDATE:item R
 *   Department   ADMIN:CRUD  HR:CRUD   INTERVIEWER:R    CANDIDATE:item R
 *   Application  ADMIN:CRUD  HR:RUD    INTERVIEWER:item R CANDIDATE:item CRUD
 *   Interview    ADMIN:CRUD  HR:CRUD   INTERVIEWER:item RU CANDIDATE:item R
 *
 * Collection endpoints for jobs, departments, slots, applications, and interviews are
 * intentionally staff-only. Candidate/interviewer item access is filtered in the service layer by
 * ownership/participation.
 *
 * The published catalogue is a deliberate exception: the Public* controllers carry no
 * @PreAuthorize because they serve anonymous visitors, and their OPEN-only rules are enforced in
 * the services (see JobVisibilityIntegrationTest, DepartmentVisibilityIntegrationTest).
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ControllerAuthorizationTest.Config.class)
class ControllerAuthorizationTest
{
    @EnableMethodSecurity
    static class Config
    {
        @Bean ApplicationService applicationService() { return Mockito.mock(ApplicationService.class); }
        @Bean DepartmentService departmentService() { return Mockito.mock(DepartmentService.class); }
        @Bean InterviewService interviewService() { return Mockito.mock(InterviewService.class); }
        @Bean JobService jobService() { return Mockito.mock(JobService.class); }
        @Bean SlotService slotService() { return Mockito.mock(SlotService.class); }

        @Bean ApplicationController applicationController(ApplicationService s) { return new ApplicationController(s); }
        @Bean DepartmentController departmentController(DepartmentService s) { return new DepartmentController(s); }
        @Bean InterviewController interviewController(InterviewService s) { return new InterviewController(s); }
        @Bean JobController jobController(JobService s) { return new JobController(s); }
        @Bean PublicJobController publicJobController(JobService s) { return new PublicJobController(s); }
        @Bean PublicDepartmentController publicDepartmentController(DepartmentService s) { return new PublicDepartmentController(s); }
        @Bean SlotController slotController(SlotService s) { return new SlotController(s); }
    }

    @Autowired ApplicationController applicationController;
    @Autowired DepartmentController departmentController;
    @Autowired InterviewController interviewController;
    @Autowired JobController jobController;
    @Autowired PublicJobController publicJobController;
    @Autowired PublicDepartmentController publicDepartmentController;
    @Autowired SlotController slotController;
    @Autowired ApplicationService applicationService;
    @Autowired DepartmentService departmentService;
    @Autowired InterviewService interviewService;
    @Autowired JobService jobService;
    @Autowired SlotService slotService;

    private static final UUID ID = UUID.randomUUID();
    private static final Pageable PAGEABLE = Pageable.unpaged();

    @BeforeEach
    void setUp()
    {
        SecurityContextHolder.clearContext();
        when(applicationService.createApplication(any())).thenReturn(applicationResponse());
        when(departmentService.createDepartment(any())).thenReturn(departmentResponse());
        when(interviewService.createInterview(any())).thenReturn(interviewResponse());
        when(jobService.createJob(any())).thenReturn(jobResponse());
        when(slotService.createSlot(any())).thenReturn(slotResponse());
    }

    private void authenticateAs(UserRole role)
    {
        Collection<? extends GrantedAuthority> authorities = role.getAuthorities();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user", "pwd", authorities));
    }

    private static void allowed(ThrowingCallable call)
    {
        assertThatCode(call).doesNotThrowAnyException();
    }

    private static void denied(ThrowingCallable call)
    {
        assertThatThrownBy(call).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void admin_canDoEverything()
    {
        authenticateAs(UserRole.ADMIN);
        allowed(() -> jobController.createJob(null));
        allowed(() -> jobController.deleteJobByPublicId(ID));
        allowed(() -> slotController.createSlot(null));
        allowed(() -> departmentController.createDepartment(null));
        allowed(() -> applicationController.createApplication(null));
        allowed(() -> applicationController.deleteApplication(ID));
        allowed(() -> interviewController.createInterview(null));
        allowed(() -> interviewController.deleteInterview(ID));
    }

    @Test
    void resourceCommandsUseRestfulStatusCodesAndLocationHeaders()
    {
        authenticateAs(UserRole.ADMIN);

        assertThat(jobController.createJob(null).getStatusCode().value()).isEqualTo(201);
        assertThat(jobController.createJob(null).getHeaders().getLocation()).hasToString("/api/jobs/" + ID);
        assertThat(departmentController.createDepartment(null).getStatusCode().value()).isEqualTo(201);
        assertThat(departmentController.createDepartment(null).getHeaders().getLocation()).hasToString("/api/departments/" + ID);
        assertThat(slotController.createSlot(null).getStatusCode().value()).isEqualTo(201);
        assertThat(slotController.createSlot(null).getHeaders().getLocation()).hasToString("/api/slots/" + ID);
        assertThat(applicationController.createApplication(null).getStatusCode().value()).isEqualTo(201);
        assertThat(applicationController.createApplication(null).getHeaders().getLocation()).hasToString("/api/applications/" + ID);
        assertThat(interviewController.createInterview(null).getStatusCode().value()).isEqualTo(201);
        assertThat(interviewController.createInterview(null).getHeaders().getLocation()).hasToString("/api/interviews/" + ID);

        assertThat(jobController.deleteJobByPublicId(ID).getStatusCode().value()).isEqualTo(204);
        assertThat(departmentController.deleteDepartmentByPublicId(ID).getStatusCode().value()).isEqualTo(204);
        assertThat(slotController.deleteSlot(ID).getStatusCode().value()).isEqualTo(204);
        assertThat(applicationController.deleteApplication(ID).getStatusCode().value()).isEqualTo(204);
        assertThat(interviewController.deleteInterview(ID).getStatusCode().value()).isEqualTo(204);
    }

    @Test
    void hrManager_runsRecruitmentButCannotCreateApplications()
    {
        authenticateAs(UserRole.HR_MANAGER);
        allowed(() -> jobController.createJob(null));
        allowed(() -> jobController.deleteJobByPublicId(ID));
        allowed(() -> slotController.createSlot(null));
        allowed(() -> departmentController.createDepartment(null));
        allowed(() -> interviewController.createInterview(null));
        allowed(() -> interviewController.deleteInterview(ID));
        allowed(() -> applicationController.getAllApplications(PAGEABLE));
        allowed(() -> applicationController.updateApplication(ID, null));
        allowed(() -> applicationController.deleteApplication(ID));

        // applications are created by candidates, not HR
        denied(() -> applicationController.createApplication(null));
    }

    @Test
    void interviewer_managesSlotsAndConductsInterviews()
    {
        authenticateAs(UserRole.INTERVIEWER);
        // own availability: full slot management
        allowed(() -> slotController.createSlot(null));
        allowed(() -> slotController.deleteSlot(ID));
        // conduct interviews: item read + update only; global list is staff-only
        allowed(() -> interviewController.getInterviewByPublicId(ID));
        allowed(() -> interviewController.updateInterview(ID, null));
        // read context to assess candidates
        allowed(() -> jobController.getAllJobs(PAGEABLE));
        allowed(() -> applicationController.getApplicationByPublicId(ID));
        allowed(() -> departmentController.getAllDepartments());

        denied(() -> interviewController.createInterview(null));
        denied(() -> interviewController.deleteInterview(ID));
        denied(() -> interviewController.getAllInterviews(PAGEABLE));
        denied(() -> jobController.createJob(null));
        denied(() -> applicationController.getAllApplications(PAGEABLE));
        denied(() -> applicationController.createApplication(null));
        denied(() -> departmentController.createDepartment(null));
    }

    @Test
    void candidate_browsesJobsAndManagesOwnApplications()
    {
        authenticateAs(UserRole.CANDIDATE);
        // Candidates browse the published catalogue, which needs no authentication at all.
        allowed(() -> publicJobController.getOpenJobs(PAGEABLE));
        allowed(() -> applicationController.createApplication(null));
        allowed(() -> applicationController.getApplicationByPublicId(ID));
        allowed(() -> applicationController.updateApplication(ID, null));
        allowed(() -> applicationController.deleteApplication(ID));
        allowed(() -> slotController.getSlotByPublicId(ID));
        allowed(() -> interviewController.getInterviewByPublicId(ID));
        allowed(() -> publicDepartmentController.getPublicDepartments());

        // candidates cannot author the catalogue, run interviews, or read the internal
        // catalogue and directory, which list drafts and unannounced work
        denied(() -> jobController.getAllJobs(PAGEABLE));
        denied(() -> departmentController.getAllDepartments());
        denied(() -> jobController.createJob(null));
        denied(() -> slotController.createSlot(null));
        denied(() -> slotController.getAllSlots(PAGEABLE));
        denied(() -> applicationController.getAllApplications(PAGEABLE));
        denied(() -> interviewController.getAllInterviews(PAGEABLE));
        denied(() -> departmentController.createDepartment(null));
        denied(() -> interviewController.createInterview(null));
        denied(() -> interviewController.updateInterview(ID, null));
    }

    /**
     * Guards against the silent-lockout failure mode: a typo'd authority string in an annotation
     * (e.g. CAN_READ_APPLICATIONS) that no role can ever satisfy, making the endpoint dead.
     */
    @Test
    void everyEntityAuthorityIsARealPermissionGrantedBySomeRole()
    {
        var grantedByAnyRole = Arrays.stream(UserRole.values())
                .flatMap(r -> r.getAuthorities().stream())
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        for (String entity : new String[]{"JOB", "SLOT", "DEPARTMENT", "APPLICATION", "INTERVIEW"})
        {
            for (String op : new String[]{"CREATE", "READ", "UPDATE", "DELETE"})
            {
                String permissionName = "CAN_" + op + "_" + entity;
                assertThatCode(() -> Permission.valueOf(permissionName)).doesNotThrowAnyException();
                String authority = Permission.valueOf(permissionName).getAuthority();
                assertThat(grantedByAnyRole)
                        .as("authority %s must be granted by at least one role, else the endpoint is unreachable", authority)
                        .contains(authority);
            }
        }
    }

    private static ApplicationResponseDTO applicationResponse()
    {
        ApplicationResponseDTO response = new ApplicationResponseDTO();
        response.setId(ID);
        return response;
    }

    private static DepartmentResponseDTO departmentResponse()
    {
        DepartmentResponseDTO response = new DepartmentResponseDTO();
        response.setId(ID);
        return response;
    }

    private static InterviewResponseDTO interviewResponse()
    {
        InterviewResponseDTO response = new InterviewResponseDTO();
        response.setId(ID);
        return response;
    }

    private static JobResponseDTO jobResponse()
    {
        JobResponseDTO response = new JobResponseDTO();
        response.setId(ID);
        return response;
    }

    private static SlotResponseDTO slotResponse()
    {
        SlotResponseDTO response = new SlotResponseDTO();
        response.setId(ID);
        return response;
    }
}
