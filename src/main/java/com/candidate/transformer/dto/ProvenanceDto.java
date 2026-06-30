package com.candidate.transformer.dto;

import com.candidate.transformer.enums.ExtractionMethod;
import com.candidate.transformer.enums.SourceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProvenanceDto {
    private SourceType sourceType;
    private String sourceName;
    private ExtractionMethod extractionMethod;
    private LocalDateTime extractedAt;
    private Double confidenceScore;
}
