package com.krino.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.krino.backend.entity.enums.Permission;
import com.krino.backend.entity.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User extends AbstractPublicEntity {

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    private String phoneNumber;

    private boolean isApproved = false;

    // Proven ownership of the email address (via the verification link, or implicitly for
    // staff/admin accounts whose initial password is delivered by email). Login is refused
    // until this is true; is_approved stays the separate admin activation toggle.
    @Column(nullable = false)
    @ColumnDefault("false")
    private boolean emailVerified = false;

    @Column(nullable = false)
    @ColumnDefault("false")
    private boolean mustChangePassword = false;

    @Column(length = 512)
    private String resumeObjectKey;

    private String resumeOriginalFilename;

    @Column(length = 100)
    private String resumeContentType;

    private Long resumeSizeBytes;

    private Instant resumeUploadedAt;

    public User(String email, String password, String firstName, String lastName, String phoneNumber) {
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
        this.roles = new HashSet<>();
    }

    @ElementCollection(targetClass = UserRole.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"),
            uniqueConstraints = {
                    @UniqueConstraint(name = "uk_user_roles_user_role", columnNames = {"user_id", "roles"})
            },
            indexes = {
                    @Index(name = "idx_user_roles_roles", columnList = "roles")
            })
    @Column(name = "roles", nullable = false)
    @Enumerated(EnumType.STRING)
    private Set<UserRole> roles;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private transient Collection<RefreshToken> refreshTokens = new HashSet<>();

    public boolean hasPermission(Permission permission) {
        return roles.stream().anyMatch(role -> role.getPermissions().contains(permission));
    }

    public boolean hasRole(UserRole role) {
        return this.roles.contains(role);
    }

    public void addRole(UserRole role) {
        this.roles.add(role);
    }

    public void removeRole(UserRole role) {
        this.roles.remove(role);
    }

    public void clearRoles() {
        this.roles.clear();
    }


    public UserRole getPrimaryRole() {
        if (this.roles.contains(UserRole.ADMIN)) return UserRole.ADMIN;
        else if (this.roles.contains(UserRole.HR_MANAGER)) return UserRole.HR_MANAGER;
        else if (this.roles.contains(UserRole.INTERVIEWER)) return UserRole.INTERVIEWER;
        else if (this.roles.contains(UserRole.CANDIDATE)) return UserRole.CANDIDATE;
        return null;
    }

    public String getRolesAsString() {
        return String.join(", ", this.roles.stream().map(Enum::name).toList());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        Long id = getId();
        return id != null && id.equals(((User) o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
