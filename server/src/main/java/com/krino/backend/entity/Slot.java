package com.krino.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "slots", indexes = {
        @Index(name = "idx_slots_interviewer", columnList = "interviewer_id")
})
public class Slot extends AbstractPublicEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interviewer_id", nullable = false)
    private User interviewer;

    @Setter(AccessLevel.NONE)
    @Column(name = "is_available", nullable = false)
    private boolean available = true;

    private LocalDate interviewDate;
    private LocalTime startTime;
    private LocalTime endTime;

    @Version
    @Column(nullable = false)
    @Setter(AccessLevel.NONE)
    private long version;

    @Setter(AccessLevel.NONE)
    @OneToOne(mappedBy = "slot")
    private Interview interview;

    public void setInterview(Interview interview) {
        this.interview = interview;
        this.available = (interview == null);
    }

    public Integer getDurationInMinutes() {
        if (startTime == null || endTime == null) return null;
        return (int) Duration.between(startTime, endTime).toMinutes();
    }

    @PrePersist
    @PreUpdate
    private void validateInvariants() {
        boolean anySet = interviewDate != null || startTime != null || endTime != null;
        boolean allSet = interviewDate != null && startTime != null && endTime != null;
        if (anySet && !allSet)
            throw new IllegalStateException("A slot must have its date, start time and end time set together, or none at all");

        if (startTime != null && endTime != null && !endTime.isAfter(startTime))
            throw new IllegalStateException("Slot end time must be after its start time");

        if (available != (interview == null))
            throw new IllegalStateException("A slot is available exactly when no interview is booked into it");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Slot)) return false;
        Long id = getId();
        return id != null && id.equals(((Slot) o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
