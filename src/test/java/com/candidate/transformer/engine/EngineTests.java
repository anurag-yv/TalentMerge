package com.candidate.transformer.engine;

import com.candidate.transformer.entity.FieldProvenance;
import com.candidate.transformer.entity.Provenance;
import com.candidate.transformer.enums.ExtractionMethod;
import com.candidate.transformer.enums.SourceType;
import com.candidate.transformer.dto.CanonicalProfileDto;
import com.candidate.transformer.dto.FieldProjection;
import com.candidate.transformer.dto.ProjectionConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

public class EngineTests {

    private NormalizationEngine normalizationEngine;
    private MatchingEngine matchingEngine;
    private ConflictResolutionEngine conflictResolutionEngine;
    private ConfidenceScoreEngine confidenceScoreEngine;

    @BeforeEach
    public void setup() {
        normalizationEngine = new NormalizationEngine();
        matchingEngine = new MatchingEngine();
        conflictResolutionEngine = new ConflictResolutionEngine(1, 2);
        confidenceScoreEngine = new ConfidenceScoreEngine();
    }

    @Test
    public void testJaroWinklerSimilarity() {
        // Exact match
        assertEquals(1.0, matchingEngine.calculateJaroWinkler("Alexander Wright", "Alexander Wright"));
        
        // Similar names
        double sim1 = matchingEngine.calculateJaroWinkler("MARTHA", "MARHTA");
        assertTrue(sim1 > 0.9);

        // Completely different
        double sim2 = matchingEngine.calculateJaroWinkler("Alexander Wright", "Emily Vance");
        assertTrue(sim2 < 0.6);
    }

    @Test
    public void testNormalizationEngine() {
        // Name normalization
        assertEquals("Alexander Wright", normalizationEngine.normalizeName("alexander wright"));
        assertEquals("Alice Smith", normalizationEngine.normalizeName("  ALICE    smith  "));

        // Email normalization
        assertEquals("alexander.wright@gmail.com", normalizationEngine.normalizeEmail("  ALEXANDER.WRIGHT@GMAIL.COM "));

        // Phone normalization
        assertEquals("+11234567890", normalizationEngine.normalizePhone("+1 (123) 456-7890"));
        assertEquals("+11234567890", normalizationEngine.normalizePhone("123-456-7890"));

        // Skill standardization
        assertEquals("Java", normalizationEngine.normalizeSkill("java se"));
        assertEquals("Spring Boot", normalizationEngine.normalizeSkill("springboot"));
        assertEquals("React", normalizationEngine.normalizeSkill("react.js"));
        assertEquals("Node.js", normalizationEngine.normalizeSkill("node"));
        assertEquals("My Custom Skill", normalizationEngine.normalizeSkill("my custom skill"));

        // Location normalization
        assertEquals("San Francisco, CA", normalizationEngine.normalizeLocation("san francisco, ca"));

        // Company normalization
        assertEquals("Google", normalizationEngine.normalizeCompany("Google Inc."));
        assertEquals("Microsoft", normalizationEngine.normalizeCompany("Microsoft Corporation"));
    }

    @Test
    public void testConflictResolution() {
        LocalDateTime time = LocalDateTime.now();
        
        // PDF (priority 2) vs CSV (priority 1)
        Provenance csvProv = Provenance.builder()
                .sourceType(SourceType.CSV)
                .confidenceScore(1.0)
                .extractedAt(time)
                .build();
        
        FieldProvenance fp = FieldProvenance.builder()
                .fieldName("headline")
                .rawValue("Software Engineer")
                .provenance(csvProv)
                .build();

        // PDF is higher priority by default in application properties (pdf=2, csv=1)
        // Here we test using the engine config fields. Let's make sure it handles priorities.
        // For testing, let's trigger a resolve
        ConflictResolutionEngine.ResolutionInput pdfInput = new ConflictResolutionEngine.ResolutionInput(
                "Lead Dev", SourceType.PDF, ExtractionMethod.PDF_HEURISTIC, "test.pdf", time, 0.90);
        
        // Since PDF priority > CSV priority, incoming PDF should win
        assertTrue(conflictResolutionEngine.shouldIncomingWin(fp, pdfInput));
    }

    @Test
    public void testConfidenceScoreEngine() {
        // CSV fields should have 1.0 confidence
        assertEquals(1.0, confidenceScoreEngine.calculateFieldConfidence(SourceType.CSV, "email", "test@test.com"));

        // PDF email should have 0.98 confidence
        assertEquals(0.98, confidenceScoreEngine.calculateFieldConfidence(SourceType.PDF, "email", "test@test.com"));

        // PDF invalid email should have 0.4
        assertEquals(0.4, confidenceScoreEngine.calculateFieldConfidence(SourceType.PDF, "email", "invalid-email"));
    }

    @Test
    public void testProjectionEngine() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        ProjectionEngine projectionEngine = new ProjectionEngine(normalizationEngine, objectMapper);

        com.candidate.transformer.dto.ProvenanceDto emailProv = com.candidate.transformer.dto.ProvenanceDto.builder()
                .sourceName("test.csv")
                .sourceType(SourceType.CSV)
                .confidenceScore(1.0)
                .build();
        com.candidate.transformer.dto.ProvenanceDto skillProv = com.candidate.transformer.dto.ProvenanceDto.builder()
                .sourceName("resume.pdf")
                .sourceType(SourceType.PDF)
                .confidenceScore(0.9)
                .build();

        List<com.candidate.transformer.dto.CandidateEmailDto> emails = List.of(
                new com.candidate.transformer.dto.CandidateEmailDto(1L, "test@test.com", emailProv)
        );
        List<com.candidate.transformer.dto.CandidatePhoneDto> phones = List.of(
                new com.candidate.transformer.dto.CandidatePhoneDto(1L, "123-456-7890", emailProv)
        );
        List<com.candidate.transformer.dto.CandidateSkillDto> skills = List.of(
                new com.candidate.transformer.dto.CandidateSkillDto(1L, "react.js", skillProv),
                new com.candidate.transformer.dto.CandidateSkillDto(2L, "java se", skillProv)
        );

        CanonicalProfileDto profile = CanonicalProfileDto.builder()
                .id(1L)
                .fullName("Alexander Wright")
                .headline("Architect")
                .location("New York, NY")
                .overallConfidence(0.95)
                .emails(emails)
                .phones(phones)
                .skills(skills)
                .build();

        List<FieldProjection> fields = List.of(
                FieldProjection.builder().path("full_name").from("fullName").type("string").required(true).build(),
                FieldProjection.builder().path("primary_email").from("emails[0]").type("string").required(true).build(),
                FieldProjection.builder().path("phone").from("phones[0]").type("string").normalize("E164").build(),
                FieldProjection.builder().path("skills").from("skills[*].name").type("string[]").normalize("canonical").build(),
                FieldProjection.builder().path("missing_field").from("nonExistent").type("string").required(false).build()
        );

        ProjectionConfig config = ProjectionConfig.builder()
                .fields(fields)
                .includeConfidence(true)
                .includeProvenance(false)
                .onMissing("null")
                .build();

        Map<String, Object> projected = projectionEngine.project(profile, config);

        assertEquals("Alexander Wright", projected.get("full_name"));
        assertEquals("test@test.com", projected.get("primary_email"));
        assertEquals("+11234567890", projected.get("phone"));
        
        List<?> projectedSkills = (List<?>) projected.get("skills");
        assertNotNull(projectedSkills);
        assertEquals(2, projectedSkills.size());
        assertTrue(projectedSkills.contains("React"));
        assertTrue(projectedSkills.contains("Java"));

        assertTrue(projected.containsKey("missing_field"));
        assertNull(projected.get("missing_field"));

        assertEquals(0.95, projected.get("overall_confidence"));
        assertFalse(projected.containsKey("provenance"));
    }
}
