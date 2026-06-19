package com.krino.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.util.Set;
import java.util.UUID;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "departments", indexes = {
        @Index(name = "idx_departments_public_id", columnList = "public_id")
})

public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @UuidGenerator
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "public_id", unique = true, nullable = false, updatable = false, columnDefinition = "VARCHAR(36)")
    private UUID publicId;

    @Column(unique = true, nullable = false)
    private String name;

    private String description;

    @OneToMany(mappedBy = "department")
    @JsonIgnore
    private Set<Job> jobs;

}