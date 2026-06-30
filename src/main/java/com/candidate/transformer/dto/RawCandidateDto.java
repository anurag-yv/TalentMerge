package com.candidate.transformer.dto;

import com.candidate.transformer.enums.ProcessingStatus;
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
public class RawCandidateDto {
    private Long id;
    private Long uploadHistoryId;
    private SourceType sourceType;
    private String sourceName;
    private LocalDateTime extractedAt;
    private String fullName;
    private String email;
    private String phone;
    private String headline;
    private String location;
    private Double yearsOfExperience;
    private String skills;
    private String experienceJson;
    private String educationJson;
    private String projectsJson;
    private String linksJson;
    private ProcessingStatus status;
}
