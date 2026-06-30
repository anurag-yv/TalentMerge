package com.candidate.transformer.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "field_provenances")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FieldProvenance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    @Column(name = "field_name", nullable = false)
    private String fieldName;

    @Embedded
    private Provenance provenance;

    @Column(name = "raw_value", columnDefinition = "TEXT")
    private String rawValue;
}
