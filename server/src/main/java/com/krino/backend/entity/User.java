package com.krino.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_users_public_id", columnList = "public_id")
})
public class User
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @UuidGenerator
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "public_id", unique = true, nullable = false, updatable = false,
            columnDefinition = "VARCHAR(36)")
    private UUID publicId;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    private String phoneNumber;

    private boolean isApproved = false;

    public User(String email, String username, String password, String firstName, String lastName, String phoneNumber)
    {
        this.email = email;
        this.username = username;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
        this.roles = new HashSet<>();
    }

    @ElementCollection(targetClass = UserRole.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    private Set<UserRole> roles;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private Collection<RefreshToken> refreshTokens = new HashSet<>();

    public boolean hasPermission(Permission permission)
    {
        return roles.stream().anyMatch(role -> role.getPermissions().contains(permission));
    }

    public boolean hasRole(UserRole role)
    {
        return this.roles.contains(role);
    }

    public void addRole(UserRole role)
    {
        this.roles.add(role);
    }

    public void removeRole(UserRole role)
    {
        this.roles.remove(role);
    }

    public void clearRoles()
    {
        this.roles.clear();
    }


    public UserRole getPrimaryRole()
    {
        if (this.roles.contains(UserRole.ADMIN))
        {
            return UserRole.ADMIN;
        } else if (this.roles.contains(UserRole.HR_MANAGER))
        {
            return UserRole.HR_MANAGER;
        } else if (this.roles.contains(UserRole.INTERVIEWER))
        {
            return UserRole.INTERVIEWER;
        } else if (this.roles.contains(UserRole.CANDIDATE))
        {
            return UserRole.CANDIDATE;
        }
        return null;
    }

    public String getRolesAsString()
    {
        return String.join(", ", this.roles.stream().map(Enum::name).toList());
    }
}