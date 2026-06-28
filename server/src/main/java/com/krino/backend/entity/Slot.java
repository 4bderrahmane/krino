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

    // The interviewer whose availability this slot represents.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interviewer_id", nullable = false)
    private User interviewer;

    @Column(name = "is_available", nullable = false)
    private boolean available = true;

    private LocalDate interviewDate;
    private LocalTime startTime;
    private LocalTime endTime;

    // Prevents silent lost updates from concurrent edits.
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

    // Derived from start/end so the two can never disagree; not a column.
    public Integer getDurationInMinutes() {
        if (startTime == null || endTime == null)
            return null;
        return (int) Duration.between(startTime, endTime).toMinutes();
    }
}
