package com.candidate.transformer.service;

import com.candidate.transformer.dto.UploadHistoryDto;
import com.candidate.transformer.entity.UploadHistory;
import com.candidate.transformer.mapper.CandidateMapper;
import com.candidate.transformer.repository.UploadHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class HistoryService {

    private final UploadHistoryRepository uploadHistoryRepository;
    private final CandidateMapper candidateMapper;

    @Transactional(readOnly = true)
    public List<UploadHistoryDto> getUploadHistory() {
        log.info("Fetching upload and audit history");
        List<UploadHistory> history = uploadHistoryRepository.findAll(Sort.by(Sort.Direction.DESC, "uploadTime"));
        return history.stream()
                .map(candidateMapper::toDto)
                .collect(Collectors.toList());
    }
}
