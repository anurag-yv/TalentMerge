package com.candidate.transformer.entity;

import com.candidate.transformer.enums.ProcessingStatus;
import com.candidate.transformer.enums.SourceType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "raw_candidates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RawCandidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "upload_history_id", nullable = false)
    private UploadHistory uploadHistory;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    private SourceType sourceType;

    @Column(name = "source_name", nullable = false)
    private String sourceName;

    @Column(name = "extracted_at", nullable = false)
    private LocalDateTime extractedAt;

    @Column(name = "full_name")
    private String fullName;

    private String email;
    private String phone;
    private String headline;
    private String location;

    @Column(name = "years_of_experience")
    private Double yearsOfExperience;

    @Column(columnDefinition = "TEXT")
    private String skills; // Comma-separated or JSON list of skills

    @Column(name = "experience_json", columnDefinition = "TEXT")
    private String experienceJson;

    @Column(name = "education_json", columnDefinition = "TEXT")
    private String educationJson;

    @Column(name = "projects_json", columnDefinition = "TEXT")
    private String projectsJson;

    @Column(name = "links_json", columnDefinition = "TEXT")
    private String linksJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProcessingStatus status;
}
