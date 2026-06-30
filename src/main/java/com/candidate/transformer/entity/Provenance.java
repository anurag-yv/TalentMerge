package com.candidate.transformer.entity;

import com.candidate.transformer.enums.ExtractionMethod;
import com.candidate.transformer.enums.SourceType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;
import java.time.LocalDateTime;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Provenance {

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type")
    private SourceType sourceType;

    @Column(name = "source_name")
    private String sourceName;

    @Enumerated(EnumType.STRING)
    @Column(name = "extraction_method")
    private ExtractionMethod extractionMethod;

    @Column(name = "extracted_at")
    private LocalDateTime extractedAt;

    @Column(name = "confidence_score")
    private Double confidenceScore;
}
