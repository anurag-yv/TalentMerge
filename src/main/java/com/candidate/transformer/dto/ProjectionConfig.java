package com.candidate.transformer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectionConfig {
    private List<FieldProjection> fields;
    
    @Builder.Default
    @JsonProperty("include_confidence")
    private boolean includeConfidence = true;
    
    @Builder.Default
    @JsonProperty("include_provenance")
    private boolean includeProvenance = true;
    
    @Builder.Default
    @JsonProperty("on_missing")
    private String onMissing = "null"; // null, omit, error
}
