package com.candidate.transformer.service;

import com.candidate.transformer.dto.CanonicalProfileDto;
import com.candidate.transformer.entity.Candidate;
import com.candidate.transformer.exception.CandidateNotFoundException;
import com.candidate.transformer.mapper.CandidateMapper;
import com.candidate.transformer.repository.CandidateRepository;
import com.candidate.transformer.repository.RawCandidateRepository;
import com.candidate.transformer.repository.UploadHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CandidateService {

    private final CandidateRepository candidateRepository;
    private final RawCandidateRepository rawCandidateRepository;
    private final UploadHistoryRepository uploadHistoryRepository;
    private final CandidateMapper candidateMapper;

    @Transactional(readOnly = true)
    public Page<CanonicalProfileDto> getCandidates(String search, String skill, String location, 
                                                   int page, int size, String sortBy, String direction) {
        log.info("Querying candidates with search: '{}', skill: '{}', location: '{}', Page: {}, Size: {}, SortBy: {}, Direction: {}", 
                 search, skill, location, page, size, sortBy, direction);
        
        Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<Candidate> candidatesPage = candidateRepository.findAllWithFilters(search, skill, location, pageable);
        return candidatesPage.map(candidateMapper::toDto);
    }

    @Transactional(readOnly = true)
    public CanonicalProfileDto getCandidateById(Long id) {
        log.info("Fetching canonical candidate profile for ID: {}", id);
        Candidate candidate = candidateRepository.findById(id)
                .orElseThrow(() -> new CandidateNotFoundException("Candidate not found with ID: " + id));
        return candidateMapper.toDto(candidate);
    }

    @Transactional
    public void deleteCandidate(Long id) {
        log.info("Deleting canonical candidate profile for ID: {}", id);
        Candidate candidate = candidateRepository.findById(id)
                .orElseThrow(() -> new CandidateNotFoundException("Candidate not found with ID: " + id));
        candidateRepository.delete(candidate);
    }

    @Transactional(readOnly = true)
    public List<String> getUniqueLocations() {
        return candidateRepository.findUniqueLocations();
    }

    @Transactional(readOnly = true)
    public List<String> getUniqueSkills() {
        return candidateRepository.findUniqueSkills();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardStats() {
        log.info("Generating dashboard statistics");
        
        long totalCandidates = candidateRepository.countTotalCandidates();
        long totalRaw = rawCandidateRepository.count();
        long totalUploads = uploadHistoryRepository.count();
        
        // Duplicates count = raw profiles processed - canonical profiles created
        long duplicateCandidates = Math.max(0, totalRaw - totalCandidates);
        
        Double avgConfidenceVal = candidateRepository.getAverageConfidence();
        double avgConfidence = avgConfidenceVal != null ? Math.round(avgConfidenceVal * 100.0) / 100.0 : 0.0;

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCandidates", totalCandidates);
        stats.put("duplicateCandidates", duplicateCandidates);
        stats.put("averageConfidence", avgConfidence);
        stats.put("filesUploaded", totalUploads);

        return stats;
    }

    @Transactional(readOnly = true)
    public List<CanonicalProfileDto> getAllCandidatesList() {
        log.info("Fetching all canonical profiles as a list for projection");
        List<Candidate> candidates = candidateRepository.findAll();
        return candidates.stream().map(candidateMapper::toDto).collect(java.util.stream.Collectors.toList());
    }
}
