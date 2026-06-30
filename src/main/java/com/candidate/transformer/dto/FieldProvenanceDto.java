package com.candidate.transformer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FieldProvenanceDto {
    private String fieldName;
    private ProvenanceDto provenance;
    private String rawValue;
}
