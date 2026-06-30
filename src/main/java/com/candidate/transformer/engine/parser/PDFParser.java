package com.candidate.transformer.engine.parser;

import com.candidate.transformer.entity.RawCandidate;
import com.candidate.transformer.enums.ProcessingStatus;
import com.candidate.transformer.enums.SourceType;
import com.candidate.transformer.exception.InvalidFileException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class PDFParser implements CandidateParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Regex patterns for contact details
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}");
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "\\+?\\d{1,3}[-.\\s]?\\(?\\d{3}\\)?[-.\\s]?\\d{3}[-.\\s]?\\d{4}");
    private static final Pattern GITHUB_PATTERN = Pattern.compile(
            "(?:https?:\\/\\/)?(?:www\\.)?github\\.com\\/([a-zA-Z0-9-_]+)");
    private static final Pattern LINKEDIN_PATTERN = Pattern.compile(
            "(?:https?:\\/\\/)?(?:www\\.)?linkedin\\.com\\/in\\/([a-zA-Z0-9-_]+)");
    
    // Heuristic patterns for years of experience
    private static final Pattern YOE_MENTION_PATTERN = Pattern.compile(
            "(\\d+(?:\\.\\d+)?)\\+?\\s*(?:years?|yrs?)\\s*(?:of)?\\s*(?:exp|experience)", Pattern.CASE_INSENSITIVE);

    // Heuristics for parsing date ranges in experience
    private static final Pattern DATE_RANGE_PATTERN = Pattern.compile(
            "((?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\s*\\d{4}|\\d{2}/\\d{4}|\\d{4})\\s*[-–to\\s]+\\s*((?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\s*\\d{4}|\\d{2}/\\d{4}|\\d{4}|Present|Current|Now)",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public List<RawCandidate> parse(InputStream inputStream, String fileName) {
        log.info("Parsing PDF file: {}", fileName);
        
        try (PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {
            if (document.isEncrypted()) {
                throw new InvalidFileException("The PDF file is encrypted and cannot be read: " + fileName);
            }

            PDFTextStripper stripper = new PDFTextStripper();
            String rawText = stripper.getText(document);

            if (rawText == null || rawText.trim().isEmpty()) {
                throw new InvalidFileException("Extracted PDF text is empty or corrupt: " + fileName);
            }

            RawCandidate rawCandidate = parseTextToCandidate(rawText, fileName);
            return Collections.singletonList(rawCandidate);

        } catch (IOException e) {
            log.error("Failed to read or parse PDF file {}: {}", fileName, e.getMessage());
            throw new InvalidFileException("Failed to read PDF file: " + e.getMessage());
        }
    }

    private RawCandidate parseTextToCandidate(String text, String fileName) {
        String[] lines = text.split("\\r?\\n");
        List<String> cleanedLines = new ArrayList<>();
        for (String line : lines) {
            if (line != null && !line.trim().isEmpty()) {
                cleanedLines.add(line.trim());
            }
        }

        // 1. Extract Contact Info
        String email = extractEmail(text);
        String phone = extractPhone(text);
        List<String> links = extractLinks(text);

        // 2. Extract Name (First non-blank line not containing contact info or header noise)
        String fullName = extractName(cleanedLines);

        // 3. Segment sections
        Map<String, List<String>> sections = segmentSections(cleanedLines);

        // 4. Extract nested entities
        List<Map<String, String>> experienceList = parseExperienceSection(sections.get("EXPERIENCE"));
        List<Map<String, String>> educationList = parseEducationSection(sections.get("EDUCATION"));
        List<Map<String, String>> projectList = parseProjectsSection(sections.get("PROJECTS"));
        List<String> skillList = parseSkillsSection(sections.get("SKILLS"), text);

        // 5. Compute Years of Experience
        Double yearsOfExperience = computeYearsOfExperience(text, experienceList);

        // 6. Set Headline (use first line of experience role, or first line after name if available)
        String headline = extractHeadline(cleanedLines, fullName, experienceList);

        // 7. Extract Location
        String location = extractLocation(cleanedLines);

        // Serialize sections to JSON strings
        String experienceJson = serializeToJson(experienceList);
        String educationJson = serializeToJson(educationList);
        String projectsJson = serializeToJson(projectList);
        String linksJson = serializeToJson(links);
        String skillsStr = String.join(", ", skillList);

        return RawCandidate.builder()
                .sourceType(SourceType.PDF)
                .sourceName(fileName)
                .extractedAt(LocalDateTime.now())
                .fullName(fullName)
                .email(email)
                .phone(phone)
                .headline(headline)
                .location(location)
                .yearsOfExperience(yearsOfExperience)
                .skills(skillsStr)
                .experienceJson(experienceJson)
                .educationJson(educationJson)
                .projectsJson(projectsJson)
                .linksJson(linksJson)
                .status(ProcessingStatus.PENDING)
                .build();
    }

    private String extractEmail(String text) {
        Matcher matcher = EMAIL_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group().trim();
        }
        return null;
    }

    private String extractPhone(String text) {
        Matcher matcher = PHONE_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group().trim();
        }
        return null;
    }

    private List<String> extractLinks(String text) {
        List<String> links = new ArrayList<>();
        Matcher gitMatcher = GITHUB_PATTERN.matcher(text);
        while (gitMatcher.find()) {
            links.add(gitMatcher.group().trim());
        }
        Matcher liMatcher = LINKEDIN_PATTERN.matcher(text);
        while (liMatcher.find()) {
            links.add(liMatcher.group().trim());
        }
        return links;
    }

    private String extractName(List<String> lines) {
        // Look in the first 5 lines for a name
        int checked = 0;
        for (String line : lines) {
            if (checked++ >= 5) break;
            String lower = line.toLowerCase();
            
            // Skip contact details or structural labels
            if (lower.contains("@") || lower.matches(".*\\d{5}.*") || PHONE_PATTERN.matcher(line).find() || 
                lower.startsWith("resume") || lower.startsWith("curriculum") || lower.startsWith("cv") ||
                lower.startsWith("profile") || lower.startsWith("summary") || lower.contains("linkedin") || 
                lower.contains("github") || lower.contains("http") || line.length() > 50) {
                continue;
            }
            // Capitalization check: candidate names are typically CamelCase
            return line;
        }
        return "Unknown Candidate";
    }

    private Map<String, List<String>> segmentSections(List<String> lines) {
        Map<String, List<String>> sections = new HashMap<>();
        String currentSection = "HEADER";
        sections.put(currentSection, new ArrayList<>());

        for (String line : lines) {
            String lowerLine = line.toLowerCase();
            String nextSection = null;

            if (lowerLine.matches("^(?:work\\s+)?experience(?:s)?$") || lowerLine.matches("^work\\s+history$") ||
                lowerLine.matches("^professional\\s+experience$") || lowerLine.matches("^employment(?:\\s+history)?$")) {
                nextSection = "EXPERIENCE";
            } else if (lowerLine.matches("^education(?:s)?$") || lowerLine.matches("^academic(?:s)?$") || 
                       lowerLine.matches("^academic\\s+background$") || lowerLine.matches("^studies$")) {
                nextSection = "EDUCATION";
            } else if (lowerLine.matches("^skills$") || lowerLine.matches("^technical\\s+skills$") || 
                       lowerLine.matches("^key\\s+skills$") || lowerLine.matches("^core\\s+competencies$") ||
                       lowerLine.matches("^skills\\s+&\\s+expertise$")) {
                nextSection = "SKILLS";
            } else if (lowerLine.matches("^projects$") || lowerLine.matches("^personal\\s+projects$") || 
                       lowerLine.matches("^academic\\s+projects$") || lowerLine.matches("^key\\s+projects$")) {
                nextSection = "PROJECTS";
            }

            if (nextSection != null) {
                currentSection = nextSection;
                sections.put(currentSection, new ArrayList<>());
            } else {
                sections.get(currentSection).add(line);
            }
        }
        return sections;
    }

    private List<Map<String, String>> parseExperienceSection(List<String> lines) {
        List<Map<String, String>> experiences = new ArrayList<>();
        if (lines == null || lines.isEmpty()) return experiences;

        Map<String, String> currentExp = null;
        StringBuilder descBuilder = new StringBuilder();

        for (String line : lines) {
            Matcher matcher = DATE_RANGE_PATTERN.matcher(line);
            if (matcher.find()) {
                // Save previous experience
                if (currentExp != null) {
                    currentExp.put("description", descBuilder.toString().trim());
                    experiences.add(currentExp);
                }

                currentExp = new HashMap<>();
                currentExp.put("startDate", matcher.group(1).trim());
                currentExp.put("endDate", matcher.group(2).trim());

                // Find company and role in the line or adjacent lines
                String roleAndCompany = line.replaceAll(DATE_RANGE_PATTERN.pattern(), "").trim();
                roleAndCompany = roleAndCompany.replaceAll("^[,|\\s-]+", "").replaceAll("[,|\\s-]+$", "");

                String role = "Software Engineer";
                String company = "Company";
                
                if (roleAndCompany.contains(" at ")) {
                    String[] parts = roleAndCompany.split(" at ");
                    role = parts[0].trim();
                    company = parts[1].trim();
                } else if (roleAndCompany.contains(" - ")) {
                    String[] parts = roleAndCompany.split(" - ");
                    role = parts[0].trim();
                    company = parts[1].trim();
                } else if (roleAndCompany.contains(",")) {
                    String[] parts = roleAndCompany.split(",");
                    role = parts[0].trim();
                    company = parts[1].trim();
                } else if (!roleAndCompany.isEmpty()) {
                    role = roleAndCompany;
                }

                currentExp.put("role", role);
                currentExp.put("company", company);
                descBuilder = new StringBuilder();
            } else {
                if (currentExp != null) {
                    descBuilder.append(line).append(" ");
                }
            }
        }

        if (currentExp != null) {
            currentExp.put("description", descBuilder.toString().trim());
            experiences.add(currentExp);
        }

        return experiences;
    }

    private List<Map<String, String>> parseEducationSection(List<String> lines) {
        List<Map<String, String>> educations = new ArrayList<>();
        if (lines == null || lines.isEmpty()) return educations;

        Map<String, String> currentEdu = null;

        for (String line : lines) {
            String lower = line.toLowerCase();
            boolean isInst = lower.contains("university") || lower.contains("college") || lower.contains("school") || lower.contains("institute");
            boolean isDeg = lower.contains("bachelor") || lower.contains("master") || lower.contains("b.s.") || lower.contains("m.s.") || lower.contains("b.tech") || lower.contains("m.tech") || lower.contains("phd");

            if (isInst || isDeg) {
                if (isInst && currentEdu != null && currentEdu.containsKey("institution")) {
                    educations.add(currentEdu);
                    currentEdu = null;
                }

                if (currentEdu == null) {
                    currentEdu = new HashMap<>();
                }

                if (isInst) {
                    currentEdu.put("institution", line);
                } else {
                    currentEdu.put("degree", line);
                }

                // Extract date in line if any
                Matcher matcher = DATE_RANGE_PATTERN.matcher(line);
                if (matcher.find()) {
                    currentEdu.put("startDate", matcher.group(1).trim());
                    currentEdu.put("endDate", matcher.group(2).trim());
                } else {
                    // Try to find a standalone year
                    Pattern yearPat = Pattern.compile("\\b(19|20)\\d{2}\\b");
                    Matcher yearMat = yearPat.matcher(line);
                    if (yearMat.find()) {
                        currentEdu.put("endDate", yearMat.group());
                    }
                }
            } else if (currentEdu != null) {
                // If it's a minor line, append to degree or field of study
                if (!currentEdu.containsKey("fieldOfStudy")) {
                    currentEdu.put("fieldOfStudy", line);
                }
            }
        }

        if (currentEdu != null) {
            educations.add(currentEdu);
        }

        // Fill defaults for mandatory fields in JPA
        for (Map<String, String> edu : educations) {
            edu.putIfAbsent("institution", "Institution");
            edu.putIfAbsent("degree", "Degree");
        }

        return educations;
    }

    private List<Map<String, String>> parseProjectsSection(List<String> lines) {
        List<Map<String, String>> projects = new ArrayList<>();
        if (lines == null || lines.isEmpty()) return projects;

        Map<String, String> currentProj = null;
        StringBuilder descBuilder = new StringBuilder();

        for (String line : lines) {
            // Assume uppercase lines or bold titles are projects, or bullet points starting with bullet symbol
            if (line.matches("^[A-Z][A-Za-z0-9\\s&|#+.-]{3,30}$") || line.contains("GitHub") || line.startsWith("*") || line.startsWith("-")) {
                if (currentProj != null) {
                    currentProj.put("description", descBuilder.toString().trim());
                    projects.add(currentProj);
                }

                currentProj = new HashMap<>();
                String title = line.replaceAll("^[*\\-\\s]+", "").trim();
                currentProj.put("title", title);
                descBuilder = new StringBuilder();
            } else {
                if (currentProj != null) {
                    descBuilder.append(line).append(" ");
                } else {
                    // Start first project
                    currentProj = new HashMap<>();
                    currentProj.put("title", "Project");
                    descBuilder.append(line).append(" ");
                }
            }
        }

        if (currentProj != null) {
            currentProj.put("description", descBuilder.toString().trim());
            projects.add(currentProj);
        }

        return projects;
    }

    private List<String> parseSkillsSection(List<String> lines, String rawText) {
        List<String> skills = new ArrayList<>();
        if (lines != null && !lines.isEmpty()) {
            for (String line : lines) {
                // Split by commas, semi-colons, vertical bars, or bullet markings
                String[] parts = line.split("[,;|•●○*]");
                for (String part : parts) {
                    String clean = part.trim();
                    if (!clean.isEmpty() && clean.length() < 30 && !clean.contains(" ") || clean.matches("[a-zA-Z\\s+#.0-9-]+")) {
                        skills.add(clean);
                    }
                }
            }
        }

        // Fallback: search raw text for common keywords
        if (skills.isEmpty()) {
            String[] keywords = {"Java", "Python", "C++", "JavaScript", "TypeScript", "React", "Angular", "Vue", "Spring Boot", "Node.js", "SQL", "PostgreSQL", "Docker", "AWS", "Git", "Machine Learning", "Data Science"};
            for (String kw : keywords) {
                Pattern p = Pattern.compile("\\b" + Pattern.quote(kw) + "\\b", Pattern.CASE_INSENSITIVE);
                if (p.matcher(rawText).find()) {
                    skills.add(kw);
                }
            }
        }

        return skills;
    }

    private Double computeYearsOfExperience(String text, List<Map<String, String>> experienceList) {
        // 1. Try to find direct mention in the text
        Matcher matcher = YOE_MENTION_PATTERN.matcher(text);
        if (matcher.find()) {
            try {
                return Double.parseDouble(matcher.group(1));
            } catch (NumberFormatException ignored) {}
        }

        // 2. Fallback: Parse date ranges from Experience section and sum
        double totalYears = 0.0;
        for (Map<String, String> exp : experienceList) {
            String start = exp.get("startDate");
            String end = exp.get("endDate");
            
            if (start != null && end != null) {
                totalYears += calculateDurationYears(start, end);
            }
        }

        return totalYears > 0 ? Math.round(totalYears * 10.0) / 10.0 : 0.0;
    }

    private double calculateDurationYears(String startStr, String endStr) {
        try {
            LocalDate startDate = parseFlexibleDate(startStr, true);
            LocalDate endDate = parseFlexibleDate(endStr, false);
            
            if (startDate != null && endDate != null) {
                long days = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
                return Math.max(0.0, (double) days / 365.0);
            }
        } catch (Exception ignored) {}
        return 0.0;
    }

    private LocalDate parseFlexibleDate(String dateStr, boolean isStart) {
        if (dateStr == null) return null;
        dateStr = dateStr.trim().toLowerCase();

        if (dateStr.equals("present") || dateStr.equals("current") || dateStr.equals("now")) {
            return LocalDate.now();
        }

        // Look for 4 digit year
        Pattern yearPat = Pattern.compile("\\b((?:19|20)\\d{2})\\b");
        Matcher yearMat = yearPat.matcher(dateStr);
        int year = LocalDate.now().getYear();
        if (yearMat.find()) {
            year = Integer.parseInt(yearMat.group());
        } else {
            return null;
        }

        // Extract month
        int month = isStart ? 1 : 12;
        String[] months = {"jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec"};
        for (int i = 0; i < months.length; i++) {
            if (dateStr.contains(months[i])) {
                month = i + 1;
                break;
            }
        }

        return LocalDate.of(year, month, 1);
    }

    private String extractHeadline(List<String> lines, String fullName, List<Map<String, String>> experienceList) {
        // Use first experience role if found
        if (experienceList != null && !experienceList.isEmpty()) {
            String role = experienceList.get(0).get("role");
            if (role != null && !role.isEmpty()) {
                return role;
            }
        }

        // Heuristic: check lines near name
        int nameIndex = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).equals(fullName)) {
                nameIndex = i;
                break;
            }
        }

        if (nameIndex != -1 && nameIndex + 1 < lines.size()) {
            String nextLine = lines.get(nameIndex + 1);
            if (nextLine.length() < 50 && !nextLine.contains("@") && !PHONE_PATTERN.matcher(nextLine).find()) {
                return nextLine;
            }
        }

        return "Professional Candidate";
    }

    private String extractLocation(List<String> lines) {
        // Simple heuristic lookup for city, state, or country indicators in top section of document
        int checked = 0;
        for (String line : lines) {
            if (checked++ >= 10) break;
            String lower = line.toLowerCase();
            
            // Matches formats like "New York, NY" or "London, UK" or "Bangalore, India" or "City, Country"
            if (line.matches("^[A-Z][a-zA-Z\\s]+,\\s*[A-Z][a-zA-Z\\s]+$") ||
                line.matches("^[A-Z][a-zA-Z\\s]+,\\s*[A-Z]{2}$")) {
                if (!lower.contains("linkedin") && !lower.contains("github") && !lower.contains("email") && !lower.contains("phone")) {
                    return line;
                }
            }
        }
        return null;
    }

    private String serializeToJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("JSON serialization failed", e);
            return null;
        }
    }
}
