package com.krino.backend.entity;

import com.krino.backend.entity.enums.UserRole;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * {@code User.roles} must never be null, whichever way the object was built, because
 * {@code hasRole}, {@code hasPermission} and {@code addRole} all dereference it directly.
 *
 * <p>Keeping that true is more delicate than it looks. The field carries {@code @Builder.Default},
 * which moves its initialiser into a generated {@code $default$} method that only Lombok's own
 * constructors call: a hand-written constructor that forgets to assign the field leaves it null,
 * and dropping {@code @Builder.Default} instead would leave every {@code User.builder()} call null.
 * Both mistakes compile, and both surface far away as an NPE inside a service.
 *
 * <p>So this pins every construction route the codebase actually uses. Hibernate is not covered
 * here because it installs its own collection on load.
 */
class UserRolesInitializationTest {

    @Test
    void noArgsConstructorInitializesRoles() {
        assertThatCode(() -> new User().addRole(UserRole.CANDIDATE)).doesNotThrowAnyException();
    }

    @Test
    void fieldConstructorInitializesRoles() {
        User user = new User("staff@test.local", "encoded", "Amina", "Cherkaoui", null);

        assertThatCode(() -> user.addRole(UserRole.HR_MANAGER)).doesNotThrowAnyException();
    }

    @Test
    void builderInitializesRolesWhenNoneAreGiven() {
        User user = User.builder().email("staff@test.local").password("encoded").build();

        assertThatCode(() -> user.addRole(UserRole.INTERVIEWER)).doesNotThrowAnyException();
    }

    @Test
    void builderStillHonoursExplicitlyProvidedRoles() {
        User user = User.builder().roles(java.util.Set.of(UserRole.ADMIN)).build();

        assertThatCode(() -> user.hasRole(UserRole.ADMIN)).doesNotThrowAnyException();
    }
}
