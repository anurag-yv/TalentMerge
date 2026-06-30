package com.candidate.transformer.controller;

import com.candidate.transformer.dto.ApiResponse;
import com.candidate.transformer.dto.UploadHistoryDto;
import com.candidate.transformer.entity.UploadHistory;
import com.candidate.transformer.enums.SourceType;
import com.candidate.transformer.mapper.CandidateMapper;
import com.candidate.transformer.service.ParserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/upload")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Upload Controller", description = "Endpoints for uploading and parsing raw candidate data files")
public class UploadController {

    private final ParserService parserService;
    private final CandidateMapper candidateMapper;

    @Operation(summary = "Upload and parse recruiter CSV file", 
               description = "Uploads a structured CSV file containing candidate records. Columns are dynamically matched, validated, and saved as raw candidates.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "File uploaded and parsed successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = UploadHistoryDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid file or empty file uploaded",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @PostMapping(value = "/csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UploadHistoryDto>> uploadCSV(
            @Parameter(description = "Structured recruiter CSV file to parse", required = true)
            @RequestParam("file") MultipartFile file) {
        
        log.info("Request to upload CSV file: {}", file.getOriginalFilename());
        UploadHistory history = parserService.uploadAndParse(file, SourceType.CSV);
        UploadHistoryDto dto = candidateMapper.toDto(history);
        
        return ResponseEntity.ok(ApiResponse.success(dto, "CSV file parsed and stored as raw candidate data"));
    }

    @Operation(summary = "Upload and parse candidate resume PDF", 
               description = "Uploads an unstructured resume PDF. Apache PDFBox extracts raw text, which is parsed into candidate details using section-based heuristics and regexes.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Resume parsed and raw record created",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = UploadHistoryDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Corrupted, encrypted or unsupported file type",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @PostMapping(value = "/resume", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UploadHistoryDto>> uploadResume(
            @Parameter(description = "Unstructured resume PDF to parse", required = true)
            @RequestParam("file") MultipartFile file) {
        
        log.info("Request to upload Resume PDF: {}", file.getOriginalFilename());
        UploadHistory history = parserService.uploadAndParse(file, SourceType.PDF);
        UploadHistoryDto dto = candidateMapper.toDto(history);
        
        return ResponseEntity.ok(ApiResponse.success(dto, "Resume parsed and raw candidate data generated"));
    }
}
