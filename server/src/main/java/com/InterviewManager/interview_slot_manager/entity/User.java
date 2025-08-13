package com.InterviewManager.interview_slot_manager.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.HashSet;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {
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

    @ElementCollection(targetClass = UserRole.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    private Set<UserRole> roles;
    
    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public boolean hasRole(UserRole role) {
        return this.roles.contains(role);
    }

    public boolean isAdmin() {
        return this.roles.contains(UserRole.ADMIN);
    }

    public boolean isHrManager() {
        return this.roles.contains(UserRole.HR_MANAGER);
    }

    public boolean isInterviewer() {
        return this.roles.contains(UserRole.INTERVIEWER);
    }

    public boolean isCandidate() {
        return this.roles.contains(UserRole.CANDIDATE);
    }

    public boolean canManageUsers() {
        return this.roles.contains(UserRole.ADMIN);
    }

    public boolean canManageSlots() {
        return this.roles.contains(UserRole.ADMIN) || this.roles.contains(UserRole.HR_MANAGER);
    }

    public boolean canConductInterviews() {
        return this.roles.contains(UserRole.ADMIN) ||
                this.roles.contains(UserRole.HR_MANAGER) ||
                this.roles.contains(UserRole.INTERVIEWER);
    }

    public boolean canViewReports() {
        return this.roles.contains(UserRole.ADMIN) || this.roles.contains(UserRole.HR_MANAGER);
    }

    public boolean canBookSlots() {
        return this.roles.contains(UserRole.CANDIDATE);
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

    public boolean hasManagementRole() {
        return this.roles.contains(UserRole.ADMIN) || this.roles.contains(UserRole.HR_MANAGER);
    }

    public UserRole getPrimaryRole() {
        if (this.roles.contains(UserRole.ADMIN)) {
            return UserRole.ADMIN;
        } else if (this.roles.contains(UserRole.HR_MANAGER)) {
            return UserRole.HR_MANAGER;
        } else if (this.roles.contains(UserRole.INTERVIEWER)) {
            return UserRole.INTERVIEWER;
        } else if (this.roles.contains(UserRole.CANDIDATE)) {
            return UserRole.CANDIDATE;
        }
        return null;
    }

    public String getRolesAsString() {
        return String.join(", ", this.roles.stream().map(Enum::name).toList());
    }
}