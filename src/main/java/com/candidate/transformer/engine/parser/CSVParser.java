package com.candidate.transformer.engine.parser;

import com.candidate.transformer.entity.RawCandidate;
import com.candidate.transformer.enums.ProcessingStatus;
import com.candidate.transformer.enums.SourceType;
import com.candidate.transformer.exception.InvalidFileException;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.time.LocalDateTime;
import java.util.*;

@Component
@Slf4j
public class CSVParser implements CandidateParser {

    @Override
    public List<RawCandidate> parse(InputStream inputStream, String fileName) {
        log.info("Parsing CSV file: {}", fileName);
        List<RawCandidate> rawCandidates = new ArrayList<>();
        Set<String> uniqueKeys = new HashSet<>();

        try (Reader reader = new InputStreamReader(inputStream);
             CSVReader csvReader = new CSVReader(reader)) {

            List<String[]> allRows = csvReader.readAll();
            if (allRows.isEmpty()) {
                throw new InvalidFileException("Uploaded CSV file is empty: " + fileName);
            }

            // Find first non-blank row as header
            int headerRowIndex = -1;
            String[] headers = null;
            for (int i = 0; i < allRows.size(); i++) {
                String[] row = allRows.get(i);
                if (row != null && row.length > 0 && Arrays.stream(row).anyMatch(cell -> cell != null && !cell.trim().isEmpty())) {
                    headerRowIndex = i;
                    headers = row;
                    break;
                }
            }

            if (headerRowIndex == -1 || headers == null) {
                throw new InvalidFileException("No headers found in CSV file: " + fileName);
            }

            // Clean headers and build column index mapping
            Map<String, Integer> headerMap = parseHeaders(headers);
            log.info("Mapped headers: {}", headerMap);

            // Process data rows
            for (int i = headerRowIndex + 1; i < allRows.size(); i++) {
                String[] row = allRows.get(i);
                
                // Skip blank rows
                if (row == null || row.length == 0 || Arrays.stream(row).allMatch(cell -> cell == null || cell.trim().isEmpty())) {
                    log.debug("Skipping blank row at line {}", i + 1);
                    continue;
                }

                try {
                    RawCandidate rawCandidate = parseRow(row, headerMap, fileName);
                    
                    // Basic validation: must have at least email or full name to be processed
                    if ((rawCandidate.getFullName() == null || rawCandidate.getFullName().trim().isEmpty()) &&
                        (rawCandidate.getEmail() == null || rawCandidate.getEmail().trim().isEmpty())) {
                        log.warn("Skipping invalid row at line {} in CSV: Both name and email are missing", i + 1);
                        continue;
                    }

                    // Handle duplicate rows in CSV in-memory
                    String dedupKey = generateDeduplicationKey(rawCandidate);
                    if (uniqueKeys.contains(dedupKey)) {
                        log.warn("Skipping duplicate row at line {} in CSV with key: {}", i + 1, dedupKey);
                        continue;
                    }

                    uniqueKeys.add(dedupKey);
                    rawCandidates.add(rawCandidate);

                } catch (Exception ex) {
                    log.error("Error parsing row at line {} in CSV file {}: {}", i + 1, fileName, ex.getMessage());
                }
            }

        } catch (IOException | CsvException e) {
            log.error("Failed to parse CSV file: {}", fileName, e);
            throw new InvalidFileException("Failed to read CSV file: " + e.getMessage());
        }

        log.info("Successfully parsed {} raw candidates from CSV", rawCandidates.size());
        return rawCandidates;
    }

    private Map<String, Integer> parseHeaders(String[] headers) {
        Map<String, Integer> headerMap = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            if (headers[i] == null) continue;
            String header = headers[i].trim().toLowerCase().replaceAll("[^a-z0-9]", "");
            
            if (header.contains("email") || header.equals("mail")) {
                headerMap.putIfAbsent("email", i);
            } else if (header.contains("fullname") || header.equals("name") || header.equals("candidatename")) {
                headerMap.putIfAbsent("fullName", i);
            } else if (header.equals("firstname") || header.equals("fname")) {
                headerMap.putIfAbsent("firstName", i);
            } else if (header.equals("lastname") || header.equals("lname")) {
                headerMap.putIfAbsent("lastName", i);
            } else if (header.contains("phone") || header.equals("contact") || header.equals("mobile")) {
                headerMap.putIfAbsent("phone", i);
            } else if (header.contains("headline") || header.equals("title") || header.equals("designation") || header.equals("role")) {
                headerMap.putIfAbsent("headline", i);
            } else if (header.equals("location") || header.equals("city") || header.equals("address")) {
                headerMap.putIfAbsent("location", i);
            } else if (header.contains("experienceyears") || header.contains("yearsofexperience") || header.equals("yoe") || header.contains("exp")) {
                headerMap.putIfAbsent("yearsOfExperience", i);
            } else if (header.contains("skills") || header.contains("techstack") || header.equals("technologies")) {
                headerMap.putIfAbsent("skills", i);
            } else if (header.equals("experience") || header.contains("workhistory") || header.contains("jobs")) {
                headerMap.putIfAbsent("experience", i);
            } else if (header.equals("education") || header.contains("academics")) {
                headerMap.putIfAbsent("education", i);
            } else if (header.contains("projects")) {
                headerMap.putIfAbsent("projects", i);
            } else if (header.contains("links") || header.contains("socials") || header.contains("urls")) {
                headerMap.putIfAbsent("links", i);
            }
        }
        return headerMap;
    }

    private RawCandidate parseRow(String[] row, Map<String, Integer> headerMap, String fileName) {
        String firstName = getCellValue(row, headerMap.get("firstName"));
        String lastName = getCellValue(row, headerMap.get("lastName"));
        String fullName = getCellValue(row, headerMap.get("fullName"));

        if (fullName.isEmpty() && (!firstName.isEmpty() || !lastName.isEmpty())) {
            fullName = (firstName + " " + lastName).trim();
        }

        String email = getCellValue(row, headerMap.get("email"));
        String phone = getCellValue(row, headerMap.get("phone"));
        String headline = getCellValue(row, headerMap.get("headline"));
        String location = getCellValue(row, headerMap.get("location"));
        String skills = getCellValue(row, headerMap.get("skills"));
        
        String yearsExpStr = getCellValue(row, headerMap.get("yearsOfExperience"));
        Double yearsOfExperience = 0.0;
        if (!yearsExpStr.isEmpty()) {
            try {
                yearsOfExperience = Double.parseDouble(yearsExpStr.replaceAll("[^0-9.]", ""));
            } catch (NumberFormatException e) {
                log.warn("Invalid years of experience format '{}' for candidate {}, defaulting to 0.0", yearsExpStr, fullName);
            }
        }

        String experienceJson = getCellValue(row, headerMap.get("experience"));
        String educationJson = getCellValue(row, headerMap.get("education"));
        String projectsJson = getCellValue(row, headerMap.get("projects"));
        String linksJson = getCellValue(row, headerMap.get("links"));

        return RawCandidate.builder()
                .sourceType(SourceType.CSV)
                .sourceName(fileName)
                .extractedAt(LocalDateTime.now())
                .fullName(fullName)
                .email(email)
                .phone(phone)
                .headline(headline)
                .location(location)
                .yearsOfExperience(yearsOfExperience)
                .skills(skills)
                .experienceJson(experienceJson.isEmpty() ? null : experienceJson)
                .educationJson(educationJson.isEmpty() ? null : educationJson)
                .projectsJson(projectsJson.isEmpty() ? null : projectsJson)
                .linksJson(linksJson.isEmpty() ? null : linksJson)
                .status(ProcessingStatus.PENDING)
                .build();
    }

    private String getCellValue(String[] row, Integer index) {
        if (index == null || index < 0 || index >= row.length) {
            return "";
        }
        String cell = row[index];
        return cell != null ? cell.trim() : "";
    }

    private String generateDeduplicationKey(RawCandidate rawCandidate) {
        String email = rawCandidate.getEmail() != null ? rawCandidate.getEmail().toLowerCase().trim() : "";
        String phone = rawCandidate.getPhone() != null ? rawCandidate.getPhone().replaceAll("[^0-9]", "") : "";
        String name = rawCandidate.getFullName() != null ? rawCandidate.getFullName().toLowerCase().replaceAll("[^a-z]", "") : "";
        return email + "|" + phone + "|" + name;
    }
}
