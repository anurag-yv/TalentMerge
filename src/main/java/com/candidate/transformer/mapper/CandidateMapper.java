package com.candidate.transformer.mapper;

import com.candidate.transformer.dto.*;
import com.candidate.transformer.entity.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CandidateMapper {

    CanonicalProfileDto toDto(Candidate candidate);

    CandidateEmailDto toDto(CandidateEmail email);

    CandidatePhoneDto toDto(CandidatePhone phone);

    CandidateSkillDto toDto(CandidateSkill skill);

    CandidateLinkDto toDto(CandidateLink link);

    ExperienceDto toDto(CandidateExperience experience);

    EducationDto toDto(CandidateEducation education);

    ProjectDto toDto(CandidateProject project);

    FieldProvenanceDto toDto(FieldProvenance fieldProvenance);

    ProvenanceDto toDto(Provenance provenance);

    @Mapping(source = "uploadHistory.id", target = "uploadHistoryId")
    RawCandidateDto toDto(RawCandidate rawCandidate);

    UploadHistoryDto toDto(UploadHistory uploadHistory);
}
