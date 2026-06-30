package com.candidate.transformer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExperienceDto {
    private Long id;
    private String company;
    private String role;
    private String startDate;
    private String endDate;
    private String description;
    private ProvenanceDto provenance;
}
