package com.krino.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.AccessLevel;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "interviews", indexes = {
        @Index(name = "idx_interviews_public_id", columnList = "public_id")
})

public class Interview
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @UuidGenerator
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "public_id", unique = true, nullable = false, updatable = false,
            columnDefinition = "VARCHAR(36)")
    private UUID publicId;

    @ManyToOne
    @JoinColumn(name = "interviewer_id")
    private User interviewer;

    @ManyToOne
    @JoinColumn(name = "candidate_id")
    private User candidate;

    @ManyToOne
    @JoinColumn(name = "job_id")
    private Job job;

    @Setter(AccessLevel.NONE)
    @OneToOne
    @JoinColumn(name = "slot_id", unique = true)
    private Slot slot;

    @Lob
    private String notes;

    private Boolean isOnline;

    public void setSlot(Slot slot)
    {
        if (this.slot != null)
        {
            Slot previous = this.slot;
            this.slot = null;
            previous.setInterview(null);
        }

        if (slot != null)
        {
            this.slot = slot;
            slot.setInterview(this);
        }
    }
}
