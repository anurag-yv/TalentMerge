package com.candidate.transformer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FieldProjection {
    private String path;         // Destination field name in output
    private String from;         // Source path in canonical record (optional)
    private String type;         // string, string[], number, etc. (optional)
    private boolean required;    // is field required (optional, defaults to false)
    private String normalize;    // E164, canonical, none, etc. (optional)
}
