package com.candidate.transformer.repository;

import com.candidate.transformer.entity.Candidate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CandidateRepository extends JpaRepository<Candidate, Long> {

    @Query("SELECT DISTINCT c FROM Candidate c JOIN c.emails e WHERE LOWER(e.email) = LOWER(:email)")
    List<Candidate> findByEmailIgnoreCase(@Param("email") String email);

    @Query("SELECT DISTINCT c FROM Candidate c JOIN c.phones p WHERE p.phone = :phone")
    List<Candidate> findByPhone(@Param("phone") String phone);

    @Query("SELECT DISTINCT c FROM Candidate c JOIN c.links l WHERE LOWER(l.url) = LOWER(:url)")
    List<Candidate> findByLinkIgnoreCase(@Param("url") String url);

    @Query("SELECT DISTINCT c FROM Candidate c " +
           "LEFT JOIN c.emails e " +
           "LEFT JOIN c.phones p " +
           "LEFT JOIN c.skills s " +
           "WHERE (:search IS NULL OR :search = '' OR " +
           "       LOWER(c.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "       LOWER(c.headline) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "       LOWER(c.location) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "       LOWER(e.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "       LOWER(p.phone) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "       LOWER(s.skillName) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:skill IS NULL OR :skill = '' OR LOWER(s.skillName) = LOWER(:skill)) AND " +
           "(:location IS NULL OR :location = '' OR LOWER(c.location) = LOWER(:location))")
    Page<Candidate> findAllWithFilters(@Param("search") String search,
                                       @Param("skill") String skill,
                                       @Param("location") String location,
                                       Pageable pageable);

    @Query("SELECT DISTINCT c.location FROM Candidate c WHERE c.location IS NOT NULL AND c.location != ''")
    List<String> findUniqueLocations();

    @Query("SELECT DISTINCT s.skillName FROM CandidateSkill s WHERE s.skillName IS NOT NULL AND s.skillName != ''")
    List<String> findUniqueSkills();

    @Query("SELECT COUNT(c) FROM Candidate c")
    long countTotalCandidates();

    @Query("SELECT AVG(c.overallConfidence) FROM Candidate c")
    Double getAverageConfidence();
}
