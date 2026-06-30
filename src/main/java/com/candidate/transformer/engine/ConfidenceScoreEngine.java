package com.candidate.transformer.engine;

import com.candidate.transformer.entity.Candidate;
import com.candidate.transformer.entity.FieldProvenance;
import com.candidate.transformer.enums.SourceType;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class ConfidenceScoreEngine {

    public double calculateFieldConfidence(SourceType sourceType, String fieldName, String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0.0;
        }

        if (sourceType == SourceType.CSV) {
            // CSV is structured and highly reliable
            return 1.0;
        }

        // PDF Heuristics
        return switch (fieldName) {
            case "email" -> value.contains("@") ? 0.98 : 0.4;
            case "phone" -> value.replaceAll("[^0-9]", "").length() >= 10 ? 0.95 : 0.5;
            case "fullName" -> 0.85; // Name is heuristic-based
            case "headline" -> 0.80; // Heuristic
            case "location" -> 0.75; // Heuristic
            case "yearsOfExperience" -> 0.80; // Computed
            case "skill" -> 0.90; // Regex/dictionary lookup
            case "experience" -> value.length() > 50 ? 0.90 : 0.70;
            case "education" -> value.length() > 30 ? 0.90 : 0.65;
            case "project" -> value.length() > 40 ? 0.85 : 0.60;
            case "link" -> (value.contains("github.com") || value.contains("linkedin.com")) ? 0.98 : 0.80;
            default -> 0.70;
        };
    }

    public double calculateOverallConfidence(Candidate candidate) {
        double weightedSum = 0.0;
        double totalWeight = 0.0;

        // Weights: Name(0.20), Emails(0.20), Phones(0.15), YearsExp(0.10), Skills(0.15), Experiences(0.10), Educations(0.10)
        
        // 1. Name Confidence
        double nameConf = getProvenanceConfidence(candidate.getFieldProvenances(), "fullName");
        weightedSum += nameConf * 0.20;
        totalWeight += 0.20;

        // 2. Email Confidence
        double emailConf = candidate.getEmails().isEmpty() ? 0.0 :
                candidate.getEmails().stream().mapToDouble(e -> e.getProvenance().getConfidenceScore()).average().orElse(0.0);
        weightedSum += emailConf * 0.20;
        totalWeight += 0.20;

        // 3. Phone Confidence
        double phoneConf = candidate.getPhones().isEmpty() ? 0.0 :
                candidate.getPhones().stream().mapToDouble(p -> p.getProvenance().getConfidenceScore()).average().orElse(0.0);
        weightedSum += phoneConf * 0.15;
        totalWeight += 0.15;

        // 4. Years Exp Confidence
        double yoeConf = getProvenanceConfidence(candidate.getFieldProvenances(), "yearsOfExperience");
        weightedSum += yoeConf * 0.10;
        totalWeight += 0.10;

        // 5. Skills Confidence
        double skillsConf = candidate.getSkills().isEmpty() ? 0.0 :
                candidate.getSkills().stream().mapToDouble(s -> s.getProvenance().getConfidenceScore()).average().orElse(0.0);
        weightedSum += skillsConf * 0.15;
        totalWeight += 0.15;

        // 6. Experience Confidence
        double expConf = candidate.getExperiences().isEmpty() ? 0.0 :
                candidate.getExperiences().stream().mapToDouble(e -> e.getProvenance().getConfidenceScore()).average().orElse(0.0);
        weightedSum += expConf * 0.10;
        totalWeight += 0.10;

        // 7. Education Confidence
        double eduConf = candidate.getEducations().isEmpty() ? 0.0 :
                candidate.getEducations().stream().mapToDouble(e -> e.getProvenance().getConfidenceScore()).average().orElse(0.0);
        weightedSum += eduConf * 0.10;
        totalWeight += 0.10;

        if (totalWeight == 0.0) return 0.0;
        double overall = weightedSum / totalWeight;

        // Return rounded score with 2 decimal places (e.g. 0.92)
        return Math.round(overall * 100.0) / 100.0;
    }

    private double getProvenanceConfidence(List<FieldProvenance> provenances, String fieldName) {
        if (provenances == null) return 0.0;
        return provenances.stream()
                .filter(p -> p.getFieldName().equals(fieldName))
                .map(p -> p.getProvenance().getConfidenceScore())
                .findFirst()
                .orElse(0.0);
    }
}
