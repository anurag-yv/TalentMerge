package com.candidate.transformer.controller;

import com.candidate.transformer.dto.ApiResponse;
import com.candidate.transformer.dto.UploadHistoryDto;
import com.candidate.transformer.service.HistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "History Controller", description = "Endpoints for retrieving file upload audit logs and processing history")
public class HistoryController {

    private final HistoryService historyService;

    @Operation(summary = "Get upload history logs", 
               description = "Returns a chronological list of all file uploads, including file sizes, processing states (SUCCESS/ERROR), duration metrics, and error logs if any.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "History logs retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = UploadHistoryDto.class))))
    })
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<UploadHistoryDto>>> getHistory() {
        log.info("Request to fetch upload audit history logs");
        List<UploadHistoryDto> history = historyService.getUploadHistory();
        return ResponseEntity.ok(ApiResponse.success(history, "Audit history logs retrieved successfully"));
    }
}
