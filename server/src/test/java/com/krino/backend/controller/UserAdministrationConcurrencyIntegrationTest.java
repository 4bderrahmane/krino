package com.krino.backend.controller;

import com.krino.backend.entity.User;
import com.krino.backend.entity.enums.UserRole;
import com.krino.backend.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockingDetails;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class UserAdministrationConcurrencyIntegrationTest extends AbstractControllerIntegrationTest {
    @MockitoSpyBean
    private UserRepository guardedUserRepository;

    @ParameterizedTest
    @CsvSource({"DEACTIVATE, DEACTIVATE", "DELETE, DELETE", "DEACTIVATE, DELETE"})
    void concurrentAdministratorRemovalsKeepOneApprovedAdministrator(Action firstAction, Action secondAction) throws Exception {

        User first = createUser("first-admin@test.local", true, UserRole.ADMIN);
        User second = createUser("second-admin@test.local", true, UserRole.ADMIN);
        Cookie firstCookie = loginAndGetAccessCookie(first.getEmail());
        Cookie secondCookie = loginAndGetAccessCookie(second.getEmail());

        CountDownLatch bothChecked = new CountDownLatch(2);
        ConcurrentLinkedQueue<Long> observedCounts = new ConcurrentLinkedQueue<>();
        var realRepositoryAnswer = mockingDetails(guardedUserRepository).getMockCreationSettings().getDefaultAnswer();
        // Execute the real PostgreSQL count, then prevent either write until both checks pass.
        doAnswer(invocation -> {
            long count = (long) realRepositoryAnswer.answer(invocation);
            observedCounts.add(count);
            bothChecked.countDown();
            if (!bothChecked.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Both administrator checks did not reach the starting gate.");
            }
            return count;
        }).when(guardedUserRepository)
                .countByRolesContainingAndIsApprovedTrueAndIdNot(eq(UserRole.ADMIN), anyLong());

        MockHttpServletRequestBuilder firstRequest = removalRequest(firstAction, second, firstCookie);
        MockHttpServletRequestBuilder secondRequest = removalRequest(secondAction, first, secondCookie);
        List<MvcResult> results;
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<MvcResult> firstResult = executor.submit(() -> mockMvc.perform(firstRequest).andReturn());
            Future<MvcResult> secondResult = executor.submit(() -> mockMvc.perform(secondRequest).andReturn());
            results = List.of(firstResult.get(30, TimeUnit.SECONDS), secondResult.get(30, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }

        assertThat(observedCounts).containsExactlyInAnyOrder(1L, 1L);
        assertThat(userRepository.findByRolesContaining(UserRole.ADMIN))
                .filteredOn(User::isApproved)
                .hasSize(1);
        assertThat(results).extracting(result -> result.getResponse().getStatus())
                .containsExactlyInAnyOrder(204, 409);

        MvcResult conflict = results.stream()
                .filter(result -> result.getResponse().getStatus() == 409)
                .findFirst().orElseThrow();
        assertThat(conflict.getResolvedException()).isInstanceOf(ConcurrencyFailureException.class);
        assertThat(sqlState(conflict.getResolvedException())).isEqualTo("40001");
        jsonPath("$.errorCode").value("DATA_CONFLICT").match(conflict);
        jsonPath("$.detail").value("Concurrent modification detected. Retry the request.").match(conflict);
    }

    private MockHttpServletRequestBuilder removalRequest(Action action, User target, Cookie actor) throws Exception {
        MockHttpServletRequestBuilder request = action == Action.DELETE
                ? delete("/api/users/" + target.getPublicId())
                : patch("/api/users/" + target.getPublicId() + "/approval")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"approved\":false}");
        return withCsrf(request).cookie(actor);
    }

    private String sqlState(Throwable failure) {
        for (Throwable cause = failure; cause != null && cause != cause.getCause(); cause = cause.getCause()) {
            if (cause instanceof SQLException sqlException) {
                return sqlException.getSQLState();
            }
        }
        return null;
    }

    private enum Action {
        DEACTIVATE, DELETE
    }
}
