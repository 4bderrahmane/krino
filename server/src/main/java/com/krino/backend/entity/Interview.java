package com.krino.backend.entity;

import com.krino.backend.entity.enums.InterviewRecommendation;
import com.krino.backend.entity.enums.InterviewStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.AccessLevel;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "interviews", indexes = {
                @Index(name = "idx_interviews_application", columnList = "application_id"),
                @Index(name = "idx_interviews_interviewer", columnList = "interviewer_id")
        }, uniqueConstraints = {
                @UniqueConstraint(name = "uk_interviews_slot", columnNames = "slot_id")
        }
)
public class Interview {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", unique = true, nullable = false, updatable = false)
    private UUID publicId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "application_id", nullable = false, foreignKey = @ForeignKey(name = "fk_interviews_application"))
    private Application application;

    @ManyToOne
    @JoinColumn(name = "interviewer_id", nullable = false, foreignKey = @ForeignKey(name = "fk_interviews_interviewer"))
    private User interviewer;

    @Setter(AccessLevel.NONE)
    @OneToOne
    @JoinColumn(name = "slot_id", foreignKey = @ForeignKey(name = "fk_interviews_slot"))
    private Slot slot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InterviewStatus status = InterviewStatus.SCHEDULED;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private InterviewRecommendation recommendation;

    @Column(nullable = false)
    private Boolean isOnline = false;

    @Column(length = 512)
    private String meetingUrl;

    public void setSlot(Slot s) {
        if (slot != null) {
            Slot previous = slot;
            slot = null;
            previous.setInterview(null);
        }

        if (s != null) {
            slot = s;
            slot.setInterview(this);
        }
    }

    @PrePersist
    void ensurePublicId() {
        if (publicId == null) publicId = UUID.randomUUID();
    }

    public User getCandidate() {
        return application != null ? application.getCandidate() : null;
    }

    public Job getJob() {
        return application != null ? application.getJob() : null;
    }
}
