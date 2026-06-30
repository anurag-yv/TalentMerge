package com.candidate.transformer.entity;

import com.candidate.transformer.enums.ProcessingStatus;
import com.candidate.transformer.enums.SourceType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "upload_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false)
    private SourceType fileType;

    @Column(name = "upload_time", nullable = false)
    private LocalDateTime uploadTime;

    @Column(name = "transformation_time")
    private LocalDateTime transformationTime;

    @Column(name = "processing_duration_ms")
    private Long processingDurationMs;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProcessingStatus status;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "records_processed")
    private Integer recordsProcessed;
}
