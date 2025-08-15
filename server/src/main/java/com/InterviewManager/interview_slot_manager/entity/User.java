package com.InterviewManager.interview_slot_manager.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.HashSet;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    @Column(nullable = true)
    private String phoneNumber;

    private boolean isApproved = false;

    public User(String email, String username, String password, String firstName, String lastName) {
        this.email = email;
        this.username = username;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    @ElementCollection(targetClass = UserRole.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    private Set<UserRole> roles = new HashSet<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public boolean hasPermission(Permission permission) {
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