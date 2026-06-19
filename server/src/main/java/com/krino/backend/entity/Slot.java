package com.krino.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "slots", indexes = {
        @Index(name = "idx_slots_public_id", columnList = "public_id")
})
public class Slot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @UuidGenerator
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "public_id", unique = true, nullable = false, updatable = false,
            columnDefinition = "VARCHAR(36)")
    private UUID publicId;


    // The interviewer whose availability this slot represents.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interviewer_id", nullable = false)
    private User interviewer;

    @Column(name = "is_available", nullable = false)
    private boolean available = true;

    private LocalDate interviewDate;
    private LocalTime startTime;
    private LocalTime endTime;

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