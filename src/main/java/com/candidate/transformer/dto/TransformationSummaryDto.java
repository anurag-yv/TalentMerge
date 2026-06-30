package com.candidate.transformer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransformationSummaryDto {
    private int totalRawProcessed;
    private int newProfilesCreated;
    private int profilesMerged;
    private long durationMs;
}
