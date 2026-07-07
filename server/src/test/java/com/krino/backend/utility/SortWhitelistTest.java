package com.krino.backend.utility;

import com.krino.backend.exception.InvalidSortFieldException;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class SortWhitelistTest {

    private static final SortWhitelist WHITELIST = SortWhitelist.of("id", "email", "createdDate");

    @Test
    void returnsSamePageableWhenEverySortPropertyIsAllowed() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Order.desc("email"), Sort.Order.asc("id")));

        assertThat(WHITELIST.sanitize(pageable)).isSameAs(pageable);
    }

    @Test
    void allowsUnsortedRequests() {
        Pageable pageable = PageRequest.of(0, 20);

        assertThat(WHITELIST.sanitize(pageable)).isSameAs(pageable);
    }

    @Test
    void rejectsAPropertyOutsideTheWhitelist() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by("password"));

        assertThatExceptionOfType(InvalidSortFieldException.class)
                .isThrownBy(() -> WHITELIST.sanitize(pageable))
                .withMessageContaining("password");
    }

    @Test
    void rejectsNestedRelationPaths() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by("application.candidate.email"));

        assertThatExceptionOfType(InvalidSortFieldException.class)
                .isThrownBy(() -> WHITELIST.sanitize(pageable));
    }

    @Test
    void rejectsWhenOnlyOneOfSeveralPropertiesIsDisallowed() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Order.asc("id"), Sort.Order.asc("secret")));

        assertThatExceptionOfType(InvalidSortFieldException.class)
                .isThrownBy(() -> WHITELIST.sanitize(pageable))
                .withMessageContaining("secret");
    }

    @Test
    void thrownExceptionCarriesTheRejectedFieldAndTheAllowedSet() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by("password"));

        assertThatExceptionOfType(InvalidSortFieldException.class)
                .isThrownBy(() -> WHITELIST.sanitize(pageable))
                .satisfies(ex -> {
                    assertThat(ex.getField()).isEqualTo("password");
                    assertThat(ex.getAllowedFields()).containsExactlyInAnyOrder("id", "email", "createdDate");
                });
    }

    @Test
    void isCaseSensitiveSoCasingTricksAreRejected() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by("Email"));

        assertThatExceptionOfType(InvalidSortFieldException.class)
                .isThrownBy(() -> WHITELIST.sanitize(pageable));
    }
}
