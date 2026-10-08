package com.krino.backend.service;

import com.krino.backend.dto.slot.SlotRequestDTO;
import com.krino.backend.entity.CustomUserDetails;
import com.krino.backend.entity.Slot;
import com.krino.backend.entity.User;
import com.krino.backend.entity.enums.UserRole;
import com.krino.backend.exception.ResourceConflictException;
import com.krino.backend.repository.SlotRepository;
import com.krino.backend.repository.UserRepository;
import com.krino.backend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Concurrent slot writes exercise PostgreSQL's exclusion constraint
 * {@code ex_slots_interviewer_no_overlap} and the service's conflict translation.
 */
class SlotConcurrencyIntegrationTest extends AbstractIntegrationTest
{
    private static final int CONCURRENT_CALLERS = 8;
    private static final Instant START = Instant.now().plus(30, ChronoUnit.DAYS);

    @Autowired
    private SlotService slotService;
    @Autowired
    private SlotRepository slotRepository;
    @Autowired
    private UserRepository userRepository;

    private User interviewer;

    @BeforeEach
    void resetTables()
    {
        slotRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
        interviewer = userRepository.save(User.builder()
                .email("interviewer@concurrency.test")
                .password("encoded")
                .firstName("Test")
                .lastName("Interviewer")
                .isApproved(true)
                .emailVerified(true)
                .roles(Set.of(UserRole.INTERVIEWER))
                .build());
    }

    @Test
    void concurrentCreatesOfTheSameWindowLeaveExactlyOneSlot() throws Exception
    {
        List<Outcome> outcomes = createConcurrently(callerIndex -> request(START, START.plus(Duration.ofMinutes(45))));

        assertThat(outcomes).filteredOn(Outcome::succeeded).hasSize(1);
        assertThat(slotRepository.count()).isEqualTo(1);
        // Constraint violations become overlap conflicts; PostgreSQL may also break up
        // waiting writers through deadlock detection, which the handler answers with "retry".
        assertThat(outcomes).filteredOn(outcome -> !outcome.succeeded())
                .hasSize(CONCURRENT_CALLERS - 1)
                .allSatisfy(outcome -> assertThat(outcome.failure())
                        .isInstanceOfAny(ResourceConflictException.class, ConcurrencyFailureException.class));
    }

    // Windows that merely start at the same moment for different callers are not a conflict;
    // the constraint must not serialise writes that do not actually collide.
    @Test
    void concurrentCreatesOfBackToBackWindowsAllSucceed() throws Exception
    {
        List<Outcome> outcomes = createConcurrently(callerIndex -> {
            Instant start = START.plus(Duration.ofHours(callerIndex));
            return request(start, start.plus(Duration.ofMinutes(45)));
        });

        assertThat(outcomes).filteredOn(Outcome::succeeded).hasSize(CONCURRENT_CALLERS);
        assertThat(slotRepository.count()).isEqualTo(CONCURRENT_CALLERS);
    }

    // Direct repository writes verify enforcement without the service's exception translation.
    @Test
    void concurrentRepositoryWritesOfTheSameWindowLeaveExactlyOneSlot() throws Exception
    {
        Instant endAt = START.plus(Duration.ofMinutes(45));
        List<Outcome> outcomes = runConcurrently(callerIndex -> () -> {
            Slot slot = new Slot();
            slot.setInterviewer(interviewer);
            slot.setStartAt(START);
            slot.setEndAt(endAt);
            slotRepository.saveAndFlush(slot);
        });

        assertThat(outcomes).filteredOn(Outcome::succeeded).hasSize(1);
        assertThat(slotRepository.count()).isEqualTo(1);
        // Whether the write was refused outright or broken out of a deadlock, the database
        // names the exclusion constraint as the thing that stopped it.
        assertThat(outcomes).filteredOn(outcome -> !outcome.succeeded())
                .allSatisfy(outcome -> assertThat(outcome.failure())
                        .hasStackTraceContaining("exclusion constraint"));
    }

    private List<Outcome> createConcurrently(RequestFactory requestFactory) throws Exception
    {
        return runConcurrently(callerIndex -> {
            SlotRequestDTO request = requestFactory.create(callerIndex);
            return () -> slotService.createSlot(request);
        });
    }

    /**
     * Fires {@link #CONCURRENT_CALLERS} writes at once, held behind a starting gate so they
     * reach the database together, and reports what each one got back.
     */
    private List<Outcome> runConcurrently(CallFactory callFactory) throws Exception
    {
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_CALLERS);
        CountDownLatch startingGate = new CountDownLatch(1);
        List<Future<Outcome>> futures = new ArrayList<>();

        try {
            for (int caller = 0; caller < CONCURRENT_CALLERS; caller++) {
                Runnable call = callFactory.create(caller);
                futures.add(executor.submit(() -> {
                    authenticateAsInterviewer();
                    startingGate.await();
                    try {
                        call.run();
                        return new Outcome(true, null);
                    } catch (RuntimeException failure) {
                        return new Outcome(false, failure);
                    } finally {
                        SecurityContextHolder.clearContext();
                    }
                }));
            }

            startingGate.countDown();

            List<Outcome> outcomes = new ArrayList<>();
            for (Future<Outcome> future : futures) {
                outcomes.add(future.get(30, TimeUnit.SECONDS));
            }
            return outcomes;
        } finally {
            executor.shutdownNow();
        }
    }

    private SlotRequestDTO request(Instant startAt, Instant endAt)
    {
        SlotRequestDTO request = new SlotRequestDTO();
        request.setInterviewerId(interviewer.getPublicId());
        request.setStartAt(startAt);
        request.setEndAt(endAt);
        return request;
    }

    private void authenticateAsInterviewer()
    {
        CustomUserDetails principal = new CustomUserDetails(interviewer);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(principal, null,
                principal.getAuthorities()));
        SecurityContextHolder.setContext(context);
    }

    @FunctionalInterface
    private interface RequestFactory
    {
        SlotRequestDTO create(int callerIndex);
    }

    @FunctionalInterface
    private interface CallFactory
    {
        Runnable create(int callerIndex);
    }

    private record Outcome(boolean succeeded, RuntimeException failure)
    {
    }
}
