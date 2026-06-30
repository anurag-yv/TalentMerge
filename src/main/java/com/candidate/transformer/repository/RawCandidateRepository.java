package com.candidate.transformer.repository;

import com.candidate.transformer.entity.RawCandidate;
import com.candidate.transformer.enums.ProcessingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RawCandidateRepository extends JpaRepository<RawCandidate, Long> {
    List<RawCandidate> findByStatus(ProcessingStatus status);
}
