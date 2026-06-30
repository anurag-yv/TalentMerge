package com.candidate.transformer.engine;

import com.candidate.transformer.entity.FieldProvenance;
import com.candidate.transformer.entity.Provenance;
import com.candidate.transformer.enums.ExtractionMethod;
import com.candidate.transformer.enums.SourceType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
@Slf4j
public class ConflictResolutionEngine {

    private final int csvPriority;
    private final int pdfPriority;

    public ConflictResolutionEngine(
            @Value("${app.conflict.priority.csv:1}") int csvPriority,
            @Value("${app.conflict.priority.pdf:2}") int pdfPriority) {
        this.csvPriority = csvPriority;
        this.pdfPriority = pdfPriority;
    }

    @Getter
    public static class ResolutionInput {
        private final String value;
        private final SourceType sourceType;
        private final ExtractionMethod extractionMethod;
        private final LocalDateTime extractedAt;
        private final double confidenceScore;
        private final String sourceName;

        public ResolutionInput(String value, SourceType sourceType, ExtractionMethod extractionMethod, 
                               String sourceName, LocalDateTime extractedAt, double confidenceScore) {
            this.value = value != null ? value.trim() : "";
            this.sourceType = sourceType;
            this.extractionMethod = extractionMethod;
            this.sourceName = sourceName;
            this.extractedAt = extractedAt != null ? extractedAt : LocalDateTime.now();
            this.confidenceScore = confidenceScore;
        }
    }

    public boolean shouldIncomingWin(FieldProvenance current, ResolutionInput incoming) {
        if (current == null || current.getProvenance() == null) {
            return true;
        }

        Provenance currentProv = current.getProvenance();

        // 1. Source Priority Check
        int currentPriority = getSourcePriority(currentProv.getSourceType());
        int incomingPriority = getSourcePriority(incoming.getSourceType());

        if (incomingPriority != currentPriority) {
            return incomingPriority > currentPriority;
        }

        // 2. Confidence Score Check
        double currentConf = currentProv.getConfidenceScore() != null ? currentProv.getConfidenceScore() : 0.0;
        double incomingConf = incoming.getConfidenceScore();
        if (Double.compare(incomingConf, currentConf) != 0) {
            return incomingConf > currentConf;
        }

        // 3. Completeness Check (String length comparison)
        String currentVal = current.getRawValue() != null ? current.getRawValue().trim() : "";
        String incomingVal = incoming.getValue();
        if (incomingVal.length() != currentVal.length()) {
            return incomingVal.length() > currentVal.length();
        }

        // 4. Recency Check (Newer wins)
        LocalDateTime currentExtracted = currentProv.getExtractedAt();
        LocalDateTime incomingExtracted = incoming.getExtractedAt();
        if (currentExtracted != null && incomingExtracted != null) {
            return incomingExtracted.isAfter(currentExtracted);
        }

        return true;
    }

    private int getSourcePriority(SourceType sourceType) {
        if (sourceType == null) return 0;
        return switch (sourceType) {
            case PDF -> pdfPriority;
            case CSV -> csvPriority;
        };
    }
}
