package com.candidate.transformer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CanonicalProfileDto {
    private Long id;
    private String fullName;
    private String headline;
    private String location;
    private Double yearsOfExperience;
    private Double overallConfidence;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    private List<CandidateEmailDto> emails;
    private List<CandidatePhoneDto> phones;
    private List<CandidateLinkDto> links;
    private List<CandidateSkillDto> skills;
    private List<ExperienceDto> experiences;
    private List<EducationDto> educations;
    private List<ProjectDto> projects;
    private List<FieldProvenanceDto> fieldProvenances;
}
