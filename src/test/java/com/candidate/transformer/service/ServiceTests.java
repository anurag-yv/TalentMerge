package com.candidate.transformer.service;

import com.candidate.transformer.dto.TransformationSummaryDto;
import com.candidate.transformer.dto.UploadHistoryDto;
import com.candidate.transformer.entity.Candidate;
import com.candidate.transformer.entity.RawCandidate;
import com.candidate.transformer.entity.UploadHistory;
import com.candidate.transformer.enums.ProcessingStatus;
import com.candidate.transformer.enums.SourceType;
import com.candidate.transformer.engine.*;
import com.candidate.transformer.mapper.CandidateMapper;
import com.candidate.transformer.repository.CandidateRepository;
import com.candidate.transformer.repository.RawCandidateRepository;
import com.candidate.transformer.repository.UploadHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ServiceTests {

    @Mock
    private RawCandidateRepository rawCandidateRepository;

    @Mock
    private CandidateRepository candidateRepository;

    @Mock
    private UploadHistoryRepository uploadHistoryRepository;

    @Mock
    private NormalizationEngine normalizationEngine;

    @Mock
    private MatchingEngine matchingEngine;

    @Mock
    private ConflictResolutionEngine conflictResolutionEngine;

    @Mock
    private ConfidenceScoreEngine confidenceScoreEngine;

    @Mock
    private CandidateMapper candidateMapper;

    @InjectMocks
    private TransformationService transformationService;

    @InjectMocks
    private HistoryService historyService;

    @BeforeEach
    public void setup() {
        // Mockito annotations will be handled by @ExtendWith
    }

    @Test
    public void testTransformationPipelineNoPending() {
        when(rawCandidateRepository.findByStatus(ProcessingStatus.PENDING))
                .thenReturn(Collections.emptyList());

        TransformationSummaryDto summary = transformationService.transformPendingCandidates();

        assertEquals(0, summary.getTotalRawProcessed());
        assertEquals(0, summary.getNewProfilesCreated());
        assertEquals(0, summary.getProfilesMerged());
    }

    @Test
    public void testTransformationPipelineCreatesNew() {
        RawCandidate raw = RawCandidate.builder()
                .id(1L)
                .fullName("Alexander Wright")
                .email("alexander.wright@gmail.com")
                .sourceType(SourceType.CSV)
                .sourceName("test.csv")
                .extractedAt(LocalDateTime.now())
                .status(ProcessingStatus.PENDING)
                .build();

        when(rawCandidateRepository.findByStatus(ProcessingStatus.PENDING))
                .thenReturn(List.of(raw));
        
        // No match found
        lenient().when(candidateRepository.findByEmailIgnoreCase(any())).thenReturn(Collections.emptyList());
        lenient().when(candidateRepository.findAll()).thenReturn(Collections.emptyList());

        TransformationSummaryDto summary = transformationService.transformPendingCandidates();

        assertEquals(1, summary.getTotalRawProcessed());
        assertEquals(1, summary.getNewProfilesCreated());
        assertEquals(0, summary.getProfilesMerged());

        verify(candidateRepository, times(1)).save(any(Candidate.class));
        verify(rawCandidateRepository, times(1)).save(raw); // Called to update status
    }

    @Test
    public void testHistoryService() {
        UploadHistory upload = UploadHistory.builder()
                .id(1L)
                .fileName("test.csv")
                .fileType(SourceType.CSV)
                .uploadTime(LocalDateTime.now())
                .status(ProcessingStatus.PROCESSED)
                .build();

        UploadHistoryDto dto = UploadHistoryDto.builder()
                .id(1L)
                .fileName("test.csv")
                .fileType(SourceType.CSV)
                .status(ProcessingStatus.PROCESSED)
                .build();

        when(uploadHistoryRepository.findAll(any(org.springframework.data.domain.Sort.class)))
                .thenReturn(List.of(upload));
        when(candidateMapper.toDto(upload)).thenReturn(dto);

        List<UploadHistoryDto> history = historyService.getUploadHistory();

        assertNotNull(history);
        assertEquals(1, history.size());
        assertEquals("test.csv", history.get(0).getFileName());
    }
}
