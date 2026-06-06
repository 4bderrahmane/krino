package com.krino.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.AccessLevel;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "interviews")

public class Interview
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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
