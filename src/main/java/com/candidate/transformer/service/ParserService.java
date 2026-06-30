package com.candidate.transformer.service;

import com.candidate.transformer.entity.RawCandidate;
import com.candidate.transformer.entity.UploadHistory;
import com.candidate.transformer.enums.ProcessingStatus;
import com.candidate.transformer.enums.SourceType;
import com.candidate.transformer.exception.InvalidFileException;
import com.candidate.transformer.engine.parser.CSVParser;
import com.candidate.transformer.engine.parser.PDFParser;
import com.candidate.transformer.repository.RawCandidateRepository;
import com.candidate.transformer.repository.UploadHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParserService {

    private final UploadHistoryRepository uploadHistoryRepository;
    private final RawCandidateRepository rawCandidateRepository;
    private final CSVParser csvParser;
    private final PDFParser pdfParser;

    @Transactional
    public UploadHistory uploadAndParse(MultipartFile file, SourceType sourceType) {
        String fileName = file.getOriginalFilename();
        log.info("Received upload file: {}, type: {}", fileName, sourceType);

        if (file.isEmpty()) {
            throw new InvalidFileException("Uploaded file is empty");
        }

        // Validate File Type
        if (sourceType == SourceType.CSV) {
            if (fileName == null || !fileName.toLowerCase().endsWith(".csv")) {
                throw new InvalidFileException("Unsupported file type. Must be a .csv file.");
            }
        } else if (sourceType == SourceType.PDF) {
            if (fileName == null || !fileName.toLowerCase().endsWith(".pdf")) {
                throw new InvalidFileException("Unsupported file type. Must be a .pdf file.");
            }
        }

        // Initialize Audit Record
        UploadHistory history = UploadHistory.builder()
                .fileName(fileName)
                .fileType(sourceType)
                .uploadTime(LocalDateTime.now())
                .fileSize(file.getSize())
                .status(ProcessingStatus.PENDING)
                .recordsProcessed(0)
                .build();
        history = uploadHistoryRepository.save(history);

        long startTime = System.currentTimeMillis();
        try {
            List<RawCandidate> rawCandidates;
            if (sourceType == SourceType.CSV) {
                rawCandidates = csvParser.parse(file.getInputStream(), fileName);
            } else {
                rawCandidates = pdfParser.parse(file.getInputStream(), fileName);
            }

            for (RawCandidate raw : rawCandidates) {
                raw.setUploadHistory(history);
            }

            rawCandidateRepository.saveAll(rawCandidates);

            // Update history success
            long duration = System.currentTimeMillis() - startTime;
            history.setRecordsProcessed(rawCandidates.size());
            history.setStatus(ProcessingStatus.PROCESSED);
            history.setProcessingDurationMs(duration);
            history.setTransformationTime(LocalDateTime.now());
            uploadHistoryRepository.save(history);

            log.info("Finished parsing file: {}. Processed {} records in {} ms.", fileName, rawCandidates.size(), duration);
            return history;

        } catch (Exception ex) {
            log.error("Failed to parse file: {}", fileName, ex);
            
            // Log Error in Audit
            long duration = System.currentTimeMillis() - startTime;
            history.setStatus(ProcessingStatus.ERROR);
            history.setErrorMessage(ex.getMessage() != null ? ex.getMessage() : "Unknown parser exception");
            history.setProcessingDurationMs(duration);
            history.setTransformationTime(LocalDateTime.now());
            uploadHistoryRepository.save(history);
            
            throw new InvalidFileException("Parsing failed: " + ex.getMessage());
        }
    }
}
