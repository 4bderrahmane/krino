package com.krino.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

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
public class Slot
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @UuidGenerator
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "public_id", unique = true, nullable = false, updatable = false,
            columnDefinition = "VARCHAR(36)")
    private UUID publicId;


    @Column(name = "is_available", nullable = false)
    private boolean available = true;

    private Integer durationInMinutes;
    private LocalDate interviewDate;
    private LocalTime startTime;
    private LocalTime endTime;

    @Setter(AccessLevel.NONE)
    @OneToOne(mappedBy = "slot")
    private Interview interview;

    public void setInterview( Interview interview)
    {
        this.interview = interview;
        this.available = (interview == null);
    }
}