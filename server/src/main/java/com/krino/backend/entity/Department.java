package com.krino.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "departments")
public class Department extends AbstractPublicEntity {

    @Column(unique = true, nullable = false)
    private String name;

    private String description;

    @OneToMany(mappedBy = "department")
    @JsonIgnore
    private Set<Job> jobs;

    @Version
    @Column(nullable = false)
    @Setter(AccessLevel.NONE)
    private long version;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Department)) return false;
        Long id = getId();
        return id != null && id.equals(((Department) o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
