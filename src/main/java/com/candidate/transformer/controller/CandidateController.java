package com.candidate.transformer.controller;

import com.candidate.transformer.dto.ApiResponse;
import com.candidate.transformer.dto.CanonicalProfileDto;
import com.candidate.transformer.service.CandidateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import com.candidate.transformer.dto.ProjectionConfig;
import com.candidate.transformer.engine.ProjectionEngine;

@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Candidate Controller", description = "Endpoints for retrieving, searching, deleting, and auditing canonical candidate profiles")
public class CandidateController {

    private final CandidateService candidateService;
    private final ProjectionEngine projectionEngine;

    @Operation(summary = "Search and query canonical candidates", 
               description = "Returns a paginated, sorted, and filtered list of canonical candidate profiles. Supports global text search (across name, skills, emails, location) and structured filter fields.")
    @GetMapping("/candidates")
    public ResponseEntity<ApiResponse<Page<CanonicalProfileDto>>> queryCandidates(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String skill,
            @RequestParam(required = false) String location,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "fullName") String sortBy,
            @RequestParam(defaultValue = "ASC") String direction) {
        
        Page<CanonicalProfileDto> candidates = candidateService.getCandidates(search, skill, location, page, size, sortBy, direction);
        return ResponseEntity.ok(ApiResponse.success(candidates, "Query results retrieved successfully"));
    }

    @Operation(summary = "Get detailed canonical profile by ID", 
               description = "Retrieves a candidate profile by its unique ID. Returns nested collections (emails, phones, experience, education) and complete field-level provenance logs.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Candidate profile found",
                    content = @Content(schema = @Schema(implementation = CanonicalProfileDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Candidate not found",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @GetMapping("/candidate/{id}")
    public ResponseEntity<ApiResponse<CanonicalProfileDto>> getCandidateById(
            @Parameter(description = "ID of the canonical candidate", required = true)
            @PathVariable Long id) {
        
        CanonicalProfileDto profile = candidateService.getCandidateById(id);
        return ResponseEntity.ok(ApiResponse.success(profile, "Candidate profile retrieved"));
    }

    @Operation(summary = "Delete canonical candidate profile", 
               description = "Deletes a canonical profile and all associated data, including emails, phone numbers, experience timelines, project logs, and field-level provenances.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Candidate profile deleted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Candidate profile not found")
    })
    @DeleteMapping("/candidate/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCandidate(
            @Parameter(description = "ID of the candidate to delete", required = true)
            @PathVariable Long id) {
        
        candidateService.deleteCandidate(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Candidate profile deleted successfully"));
    }

    @Operation(summary = "Retrieve unique location values", 
               description = "Gets a list of all unique candidate locations currently stored. Useful for populating filter dropdowns in the UI.")
    @GetMapping("/candidates/locations")
    public ResponseEntity<ApiResponse<List<String>>> getLocations() {
        List<String> locations = candidateService.getUniqueLocations();
        return ResponseEntity.ok(ApiResponse.success(locations, "Unique locations fetched"));
    }

    @Operation(summary = "Retrieve unique skill values", 
               description = "Gets a list of all unique standardized skills currently stored. Useful for populating filter dropdowns in the UI.")
    @GetMapping("/candidates/skills")
    public ResponseEntity<ApiResponse<List<String>>> getSkills() {
        List<String> skills = candidateService.getUniqueSkills();
        return ResponseEntity.ok(ApiResponse.success(skills, "Unique skills fetched"));
    }

    @Operation(summary = "Get candidate statistics for dashboard", 
               description = "Computes database-wide metrics, including total canonical counts, duplicate profiles merged, average profile confidence, and total uploads.")
    @GetMapping("/candidates/dashboard-stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardStats() {
        Map<String, Object> stats = candidateService.getDashboardStats();
        return ResponseEntity.ok(ApiResponse.success(stats, "Dashboard statistics computed"));
    }

    @Operation(summary = "Project candidate profile with dynamic runtime config", 
               description = "Accepts a runtime configuration JSON to reshape and normalize the output fields of a canonical candidate profile.")
    @PostMapping("/candidate/{id}/project")
    public ResponseEntity<ApiResponse<Map<String, Object>>> projectCandidate(
            @Parameter(description = "ID of the canonical candidate", required = true)
            @PathVariable Long id,
            @RequestBody ProjectionConfig config) {
        
        CanonicalProfileDto profile = candidateService.getCandidateById(id);
        Map<String, Object> projected = projectionEngine.project(profile, config);
        return ResponseEntity.ok(ApiResponse.success(projected, "Candidate profile projected successfully"));
    }

    @Operation(summary = "Project all candidates with dynamic runtime config", 
               description = "Retrieves all canonical candidate profiles and projects each of them according to the provided runtime configuration.")
    @PostMapping("/candidates/project")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> projectAllCandidates(
            @RequestBody ProjectionConfig config) {
        
        List<CanonicalProfileDto> profiles = candidateService.getAllCandidatesList();
        List<Map<String, Object>> projectedList = new ArrayList<>();
        for (CanonicalProfileDto profile : profiles) {
            projectedList.add(projectionEngine.project(profile, config));
        }
        return ResponseEntity.ok(ApiResponse.success(projectedList, "All candidate profiles projected successfully"));
    }
}
