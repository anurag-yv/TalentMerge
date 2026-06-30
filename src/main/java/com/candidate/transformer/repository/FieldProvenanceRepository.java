package com.candidate.transformer.repository;

import com.candidate.transformer.entity.FieldProvenance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FieldProvenanceRepository extends JpaRepository<FieldProvenance, Long> {
    List<FieldProvenance> findByCandidateId(Long candidateId);
}
