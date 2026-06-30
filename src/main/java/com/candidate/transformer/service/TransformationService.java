package com.candidate.transformer.service;

import com.candidate.transformer.dto.TransformationSummaryDto;
import com.candidate.transformer.entity.*;
import com.candidate.transformer.enums.ExtractionMethod;
import com.candidate.transformer.enums.ProcessingStatus;
import com.candidate.transformer.enums.SourceType;
import com.candidate.transformer.engine.*;
import com.candidate.transformer.engine.ConflictResolutionEngine.ResolutionInput;
import com.candidate.transformer.repository.CandidateRepository;
import com.candidate.transformer.repository.RawCandidateRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransformationService {

    private final RawCandidateRepository rawCandidateRepository;
    private final CandidateRepository candidateRepository;
    private final NormalizationEngine normalizationEngine;
    private final MatchingEngine matchingEngine;
    private final ConflictResolutionEngine conflictResolutionEngine;
    private final ConfidenceScoreEngine confidenceScoreEngine;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public TransformationSummaryDto transformPendingCandidates() {
        log.info("Starting candidate transformation pipeline");
        long startTime = System.currentTimeMillis();

        List<RawCandidate> pendingCandidates = rawCandidateRepository.findByStatus(ProcessingStatus.PENDING);
        if (pendingCandidates.isEmpty()) {
            log.info("No pending raw candidates found for transformation");
            return TransformationSummaryDto.builder()
                    .totalRawProcessed(0)
                    .newProfilesCreated(0)
                    .profilesMerged(0)
                    .durationMs(0)
                    .build();
        }

        int newProfilesCount = 0;
        int mergedProfilesCount = 0;

        for (RawCandidate raw : pendingCandidates) {
            try {
                // 1. Normalize Raw candidate fields
                normalizeRawCandidate(raw);

                // 2. Find matches in database
                Candidate match = findExistingMatch(raw);

                if (match != null) {
                    log.info("Matching profile found (ID: {}). Merging Raw[{}] into existing.", match.getId(), raw.getFullName());
                    mergeIntoCandidate(raw, match);
                    mergedProfilesCount++;
                } else {
                    log.info("No matching profile found. Creating new Canonical profile for Raw[{}].", raw.getFullName());
                    Candidate newCandidate = createNewCandidate(raw);
                    candidateRepository.save(newCandidate);
                    newProfilesCount++;
                }

                raw.setStatus(ProcessingStatus.PROCESSED);
                rawCandidateRepository.save(raw);

            } catch (Exception ex) {
                log.error("Failed to transform raw candidate ID {}: {}", raw.getId(), ex.getMessage(), ex);
                raw.setStatus(ProcessingStatus.ERROR);
                rawCandidateRepository.save(raw);
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("Transformation pipeline completed. Processed: {}, Created: {}, Merged: {}, Duration: {} ms",
                 pendingCandidates.size(), newProfilesCount, mergedProfilesCount, duration);

        return TransformationSummaryDto.builder()
                .totalRawProcessed(pendingCandidates.size())
                .newProfilesCreated(newProfilesCount)
                .profilesMerged(mergedProfilesCount)
                .durationMs(duration)
                .build();
    }

    private void normalizeRawCandidate(RawCandidate raw) {
        raw.setFullName(normalizationEngine.normalizeName(raw.getFullName()));
        raw.setEmail(normalizationEngine.normalizeEmail(raw.getEmail()));
        raw.setPhone(normalizationEngine.normalizePhone(raw.getPhone()));
        raw.setLocation(normalizationEngine.normalizeLocation(raw.getLocation()));
    }

    private Candidate findExistingMatch(RawCandidate raw) {
        // Query candidates by Email
        if (raw.getEmail() != null && !raw.getEmail().isEmpty()) {
            List<Candidate> matches = candidateRepository.findByEmailIgnoreCase(raw.getEmail());
            for (Candidate c : matches) {
                if (matchingEngine.isDuplicate(raw, c)) return c;
            }
        }

        // Query candidates by Phone
        if (raw.getPhone() != null && !raw.getPhone().isEmpty()) {
            List<Candidate> matches = candidateRepository.findByPhone(raw.getPhone());
            for (Candidate c : matches) {
                if (matchingEngine.isDuplicate(raw, c)) return c;
            }
        }

        // Query candidates by Links
        List<String> links = deserializeLinks(raw.getLinksJson());
        for (String url : links) {
            List<Candidate> matches = candidateRepository.findByLinkIgnoreCase(url);
            for (Candidate c : matches) {
                if (matchingEngine.isDuplicate(raw, c)) return c;
            }
        }

        // Performance-optimized heuristic: query candidates by first character of name
        String name = raw.getFullName();
        if (name != null && !name.trim().isEmpty()) {
            String firstChar = name.substring(0, 1);
            List<Candidate> allCandidates = candidateRepository.findAll(); // Fast in-memory check for thousands
            for (Candidate c : allCandidates) {
                if (c.getFullName() != null && c.getFullName().startsWith(firstChar)) {
                    if (matchingEngine.isDuplicate(raw, c)) return c;
                }
            }
        }

        return null;
    }

    private Candidate createNewCandidate(RawCandidate raw) {
        Candidate candidate = new Candidate();
        candidate.setFullName(raw.getFullName());
        candidate.setHeadline(raw.getHeadline());
        candidate.setLocation(raw.getLocation());
        candidate.setYearsOfExperience(raw.getYearsOfExperience());

        // Setup provenances for single-value fields
        updateFieldProvenance(candidate, "fullName", raw.getFullName(), raw, getMethod(raw, "fullName"));
        updateFieldProvenance(candidate, "headline", raw.getHeadline(), raw, getMethod(raw, "headline"));
        updateFieldProvenance(candidate, "location", raw.getLocation(), raw, getMethod(raw, "location"));
        updateFieldProvenance(candidate, "yearsOfExperience", 
                               raw.getYearsOfExperience() != null ? String.valueOf(raw.getYearsOfExperience()) : "0.0", 
                               raw, getMethod(raw, "yearsOfExperience"));

        // Add collections
        addCollections(raw, candidate);

        // Calculate confidence
        double overall = confidenceScoreEngine.calculateOverallConfidence(candidate);
        candidate.setOverallConfidence(overall);

        return candidate;
    }

    private void mergeIntoCandidate(RawCandidate raw, Candidate candidate) {
        // Resolve single fields
        mergeSingleField(candidate, "fullName", raw.getFullName(), raw, getMethod(raw, "fullName"));
        mergeSingleField(candidate, "headline", raw.getHeadline(), raw, getMethod(raw, "headline"));
        mergeSingleField(candidate, "location", raw.getLocation(), raw, getMethod(raw, "location"));
        mergeSingleField(candidate, "yearsOfExperience", 
                         raw.getYearsOfExperience() != null ? String.valueOf(raw.getYearsOfExperience()) : "0.0", 
                         raw, getMethod(raw, "yearsOfExperience"));

        // Merge collections
        addCollections(raw, candidate);

        // Update overall confidence
        double overall = confidenceScoreEngine.calculateOverallConfidence(candidate);
        candidate.setOverallConfidence(overall);

        candidateRepository.save(candidate);
    }

    private void mergeSingleField(Candidate candidate, String fieldName, String incomingValue, 
                                  RawCandidate raw, ExtractionMethod method) {
        if (incomingValue == null || incomingValue.trim().isEmpty()) return;

        double conf = confidenceScoreEngine.calculateFieldConfidence(raw.getSourceType(), fieldName, incomingValue);
        ResolutionInput input = new ResolutionInput(
                incomingValue, raw.getSourceType(), method, raw.getSourceName(), raw.getExtractedAt(), conf);

        FieldProvenance current = candidate.getFieldProvenances().stream()
                .filter(p -> p.getFieldName().equals(fieldName))
                .findFirst()
                .orElse(null);

        if (conflictResolutionEngine.shouldIncomingWin(current, input)) {
            // Update value on candidate
            switch (fieldName) {
                case "fullName" -> candidate.setFullName(normalizationEngine.normalizeName(incomingValue));
                case "headline" -> candidate.setHeadline(incomingValue);
                case "location" -> candidate.setLocation(normalizationEngine.normalizeLocation(incomingValue));
                case "yearsOfExperience" -> {
                    try {
                        candidate.setYearsOfExperience(Double.parseDouble(incomingValue));
                    } catch (NumberFormatException ignored) {}
                }
            }
            // Update provenance
            updateFieldProvenance(candidate, fieldName, incomingValue, raw, method);
        }
    }

    private void updateFieldProvenance(Candidate candidate, String fieldName, String value, 
                                       RawCandidate raw, ExtractionMethod method) {
        double conf = confidenceScoreEngine.calculateFieldConfidence(raw.getSourceType(), fieldName, value);
        Provenance prov = Provenance.builder()
                .sourceType(raw.getSourceType())
                .sourceName(raw.getSourceName())
                .extractionMethod(method)
                .extractedAt(raw.getExtractedAt())
                .confidenceScore(conf)
                .build();

        FieldProvenance fp = candidate.getFieldProvenances().stream()
                .filter(p -> p.getFieldName().equals(fieldName))
                .findFirst()
                .orElse(null);

        if (fp != null) {
            fp.setProvenance(prov);
            fp.setRawValue(value);
        } else {
            fp = FieldProvenance.builder()
                    .candidate(candidate)
                    .fieldName(fieldName)
                    .provenance(prov)
                    .rawValue(value)
                    .build();
            candidate.addFieldProvenance(fp);
        }
    }

    private void addCollections(RawCandidate raw, Candidate candidate) {
        // 1. Email
        if (raw.getEmail() != null && !raw.getEmail().trim().isEmpty()) {
            String email = raw.getEmail().trim();
            boolean exists = candidate.getEmails().stream().anyMatch(e -> e.getEmail().equalsIgnoreCase(email));
            if (!exists) {
                double conf = confidenceScoreEngine.calculateFieldConfidence(raw.getSourceType(), "email", email);
                candidate.addEmail(CandidateEmail.builder()
                        .email(email)
                        .provenance(buildProvenance(raw, getMethod(raw, "email"), conf))
                        .build());
            }
        }

        // 2. Phone
        if (raw.getPhone() != null && !raw.getPhone().trim().isEmpty()) {
            String phone = raw.getPhone().trim();
            boolean exists = candidate.getPhones().stream().anyMatch(p -> p.getPhone().equals(phone));
            if (!exists) {
                double conf = confidenceScoreEngine.calculateFieldConfidence(raw.getSourceType(), "phone", phone);
                candidate.addPhone(CandidatePhone.builder()
                        .phone(phone)
                        .provenance(buildProvenance(raw, getMethod(raw, "phone"), conf))
                        .build());
            }
        }

        // 3. Links
        List<String> links = deserializeLinks(raw.getLinksJson());
        for (String link : links) {
            boolean exists = candidate.getLinks().stream().anyMatch(l -> l.getUrl().equalsIgnoreCase(link));
            if (!exists) {
                double conf = confidenceScoreEngine.calculateFieldConfidence(raw.getSourceType(), "link", link);
                candidate.addLink(CandidateLink.builder()
                        .url(link)
                        .provenance(buildProvenance(raw, getMethod(raw, "link"), conf))
                        .build());
            }
        }

        // 4. Skills
        if (raw.getSkills() != null && !raw.getSkills().trim().isEmpty()) {
            String[] rawSkills = raw.getSkills().split(",");
            for (String rs : rawSkills) {
                String normalizedSkill = normalizationEngine.normalizeSkill(rs);
                if (normalizedSkill.isEmpty()) continue;
                boolean exists = candidate.getSkills().stream().anyMatch(s -> s.getSkillName().equalsIgnoreCase(normalizedSkill));
                if (!exists) {
                    double conf = confidenceScoreEngine.calculateFieldConfidence(raw.getSourceType(), "skill", normalizedSkill);
                    candidate.addSkill(CandidateSkill.builder()
                            .skillName(normalizedSkill)
                            .provenance(buildProvenance(raw, getMethod(raw, "skill"), conf))
                            .build());
                }
            }
        }

        // 5. Experience
        List<Map<String, String>> experiences = deserializeJsonList(raw.getExperienceJson());
        for (Map<String, String> expMap : experiences) {
            String company = normalizationEngine.normalizeCompany(expMap.get("company"));
            String role = normalizationEngine.normalizeName(expMap.get("role"));
            String start = normalizationEngine.normalizeDate(expMap.get("startDate"));
            String end = normalizationEngine.normalizeDate(expMap.get("endDate"));
            String desc = expMap.get("description");

            boolean exists = candidate.getExperiences().stream()
                    .anyMatch(e -> e.getCompany().equalsIgnoreCase(company) && e.getRole().equalsIgnoreCase(role));
            if (!exists) {
                double conf = confidenceScoreEngine.calculateFieldConfidence(raw.getSourceType(), "experience", desc);
                candidate.addExperience(CandidateExperience.builder()
                        .company(company)
                        .role(role)
                        .startDate(start)
                        .endDate(end)
                        .description(desc)
                        .provenance(buildProvenance(raw, getMethod(raw, "experience"), conf))
                        .build());
            }
        }

        // 6. Education
        List<Map<String, String>> educations = deserializeJsonList(raw.getEducationJson());
        for (Map<String, String> eduMap : educations) {
            String inst = normalizationEngine.normalizeName(eduMap.get("institution"));
            String deg = normalizationEngine.normalizeName(eduMap.get("degree"));
            String field = normalizationEngine.normalizeName(eduMap.get("fieldOfStudy"));
            String start = normalizationEngine.normalizeDate(eduMap.get("startDate"));
            String end = normalizationEngine.normalizeDate(eduMap.get("endDate"));

            boolean exists = candidate.getEducations().stream()
                    .anyMatch(e -> e.getInstitution().equalsIgnoreCase(inst) && e.getDegree().equalsIgnoreCase(deg));
            if (!exists) {
                double conf = confidenceScoreEngine.calculateFieldConfidence(raw.getSourceType(), "education", inst);
                candidate.addEducation(CandidateEducation.builder()
                        .institution(inst)
                        .degree(deg)
                        .fieldOfStudy(field)
                        .startDate(start)
                        .endDate(end)
                        .provenance(buildProvenance(raw, getMethod(raw, "education"), conf))
                        .build());
            }
        }

        // 7. Projects
        List<Map<String, String>> projects = deserializeJsonList(raw.getProjectsJson());
        for (Map<String, String> projMap : projects) {
            String title = normalizationEngine.normalizeName(projMap.get("title"));
            String desc = projMap.get("description");
            String tech = projMap.get("technologies");

            boolean exists = candidate.getProjects().stream()
                    .anyMatch(p -> p.getTitle().equalsIgnoreCase(title));
            if (!exists) {
                double conf = confidenceScoreEngine.calculateFieldConfidence(raw.getSourceType(), "project", desc);
                candidate.addProject(CandidateProject.builder()
                        .title(title)
                        .description(desc)
                        .technologies(tech)
                        .provenance(buildProvenance(raw, getMethod(raw, "project"), conf))
                        .build());
            }
        }
    }

    private Provenance buildProvenance(RawCandidate raw, ExtractionMethod method, double confidence) {
        return Provenance.builder()
                .sourceType(raw.getSourceType())
                .sourceName(raw.getSourceName())
                .extractionMethod(method)
                .extractedAt(raw.getExtractedAt())
                .confidenceScore(confidence)
                .build();
    }

    private ExtractionMethod getMethod(RawCandidate raw, String fieldName) {
        if (raw.getSourceType() == SourceType.CSV) {
            return ExtractionMethod.CSV_PARSER;
        }
        // PDF heuristics
        return switch (fieldName) {
            case "email", "phone", "link" -> ExtractionMethod.PDF_REGEXP;
            default -> ExtractionMethod.PDF_HEURISTIC;
        };
    }

    private List<String> deserializeLinks(String json) {
        if (json == null || json.trim().isEmpty()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("Failed to deserialize links JSON", e);
            return Collections.emptyList();
        }
    }

    private List<Map<String, String>> deserializeJsonList(String json) {
        if (json == null || json.trim().isEmpty()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<Map<String, String>>>() {});
        } catch (Exception e) {
            log.warn("Failed to deserialize section JSON: {}", json, e);
            return Collections.emptyList();
        }
    }
}
