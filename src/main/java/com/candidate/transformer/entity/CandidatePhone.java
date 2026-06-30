package com.candidate.transformer.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "candidate_phones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidatePhone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    @Column(nullable = false)
    private String phone;

    @Embedded
    private Provenance provenance;
}
