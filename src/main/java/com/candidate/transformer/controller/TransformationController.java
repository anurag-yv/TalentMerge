package com.candidate.transformer.controller;

import com.candidate.transformer.dto.ApiResponse;
import com.candidate.transformer.dto.TransformationSummaryDto;
import com.candidate.transformer.service.TransformationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Transformation Controller", description = "Endpoints for running the candidate normalization and deduplication pipeline")
public class TransformationController {

    private final TransformationService transformationService;

    @Operation(summary = "Execute candidate transformation pipeline", 
               description = "Processes all pending raw candidate data. Running this engine will normalize names, phone numbers, and skills, run similarity matches for deduplication, apply conflict resolution rules, and compile/update final canonical profiles.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Transformation pipeline completed successfully",
                    content = @Content(schema = @Schema(implementation = TransformationSummaryDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal processing or transformation pipeline error",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @PostMapping("/transform")
    public ResponseEntity<ApiResponse<TransformationSummaryDto>> transform() {
        log.info("Request to run transformation pipeline");
        TransformationSummaryDto summary = transformationService.transformPendingCandidates();
        return ResponseEntity.ok(ApiResponse.success(summary, "Transformation pipeline executed successfully"));
    }
}
