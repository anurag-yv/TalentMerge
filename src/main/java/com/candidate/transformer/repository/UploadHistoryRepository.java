package com.candidate.transformer.repository;

import com.candidate.transformer.entity.UploadHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UploadHistoryRepository extends JpaRepository<UploadHistory, Long> {
}
