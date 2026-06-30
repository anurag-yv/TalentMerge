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
public class UploadHistoryDto {
    private Long id;
    private String fileName;
    private SourceType fileType;
    private LocalDateTime uploadTime;
    private LocalDateTime transformationTime;
    private Long processingDurationMs;
    private ProcessingStatus status;
    private String errorMessage;
    private Long fileSize;
    private Integer recordsProcessed;
}
