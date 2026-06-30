package com.candidate.transformer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateSkillDto {
    private Long id;
    private String skillName;
    private ProvenanceDto provenance;
}
