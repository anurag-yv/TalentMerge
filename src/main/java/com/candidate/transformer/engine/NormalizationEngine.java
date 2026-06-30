package com.candidate.transformer.engine;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class NormalizationEngine {

    private static final Map<String, String> SKILL_DICTIONARY = new HashMap<>();

    static {
        // Standardizing Skill Dictionary
        putSkillAliases("Java", "java", "java se", "java ee", "core java", "java dev", "jdk");
        putSkillAliases("Spring Boot", "spring boot", "springboot", "spring-boot", "spring", "spring framework");
        putSkillAliases("React", "react", "reactjs", "react js", "react.js", "react framework");
        putSkillAliases("Node.js", "node", "nodejs", "node js", "node.js");
        putSkillAliases("Machine Learning", "ml", "machine learning", "machinelearning", "deep learning");
        putSkillAliases("SQL", "sql", "mysql", "mssql", "structured query language", "sqlite");
        putSkillAliases("PostgreSQL", "postgres", "postgresql", "postgresql database", "pg");
        putSkillAliases("Angular", "angular", "angularjs", "angular.js");
        putSkillAliases("Docker", "docker", "docker container");
        putSkillAliases("AWS", "aws", "amazon web services");
        putSkillAliases("Git", "git", "github", "gitlab");
        putSkillAliases("Python", "python", "python3", "py");
    }

    private static void putSkillAliases(String standardName, String... aliases) {
        for (String alias : aliases) {
            SKILL_DICTIONARY.put(alias.toLowerCase().trim(), standardName);
        }
    }

    public String normalizeName(String name) {
        if (name == null || name.trim().isEmpty()) return "";
        String[] parts = name.trim().replaceAll("\\s+", " ").split(" ");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            sb.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                sb.append(part.substring(1).toLowerCase());
            }
            sb.append(" ");
        }
        return sb.toString().trim();
    }

    public String normalizeEmail(String email) {
        if (email == null) return "";
        return email.trim().toLowerCase();
    }

    public String normalizePhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) return "";
        // Keep only digits and starting '+' if present
        String clean = phone.trim();
        boolean startsWithPlus = clean.startsWith("+");
        String digits = clean.replaceAll("[^0-9]", "");
        
        if (digits.isEmpty()) return "";
        
        if (startsWithPlus) {
            return "+" + digits;
        }
        
        // Default to adding +1 for US/Canada if 10 digits, or just return digits
        if (digits.length() == 10) {
            return "+1" + digits;
        }
        
        return digits;
    }

    public String normalizeSkill(String skill) {
        if (skill == null || skill.trim().isEmpty()) return "";
        String lower = skill.trim().toLowerCase();
        
        // Dictionary lookup
        if (SKILL_DICTIONARY.containsKey(lower)) {
            return SKILL_DICTIONARY.get(lower);
        }

        // Default: Capitalize each word nicely
        return normalizeName(skill);
    }

    public String normalizeLocation(String location) {
        if (location == null || location.trim().isEmpty()) return "";
        // Capitalize each part of location, e.g. "san francisco, ca" -> "San Francisco, CA"
        String[] parts = location.trim().split(",");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i].trim();
            if (part.length() == 2) {
                // Keep state codes uppercase (e.g. CA, NY)
                sb.append(part.toUpperCase());
            } else {
                sb.append(normalizeName(part));
            }
            if (i < parts.length - 1) {
                sb.append(", ");
            }
        }
        return sb.toString().trim();
    }

    public String normalizeCompany(String company) {
        if (company == null || company.trim().isEmpty()) return "";
        // Clean corporate designations
        String clean = company.trim()
                .replaceAll("(?i)\\b(inc|corp|llc|ltd|co|corporation|incorporated|limited)\\b[.]?", "")
                .replaceAll("\\s+", " ")
                .trim();
        
        // Remove trailing commas if any
        if (clean.endsWith(",")) {
            clean = clean.substring(0, clean.length() - 1).trim();
        }

        if (clean.isEmpty()) {
            return normalizeName(company); // Fallback to raw if cleaned is empty
        }

        return normalizeName(clean);
    }

    public String normalizeDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) return "";
        dateStr = dateStr.trim().toLowerCase();

        if (dateStr.equals("present") || dateStr.equals("current") || dateStr.equals("now")) {
            return "Present";
        }

        // Try to match standard "May 2020" or "05/2020"
        Pattern p1 = Pattern.compile("^(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-z]*[\\s,]*(\\d{4})$");
        Matcher m1 = p1.matcher(dateStr);
        if (m1.find()) {
            String month = m1.group(1);
            month = Character.toUpperCase(month.charAt(0)) + month.substring(1).toLowerCase();
            return month + " " + m1.group(2);
        }

        Pattern p2 = Pattern.compile("^(\\d{1,2})/(\\d{4})$");
        Matcher m2 = p2.matcher(dateStr);
        if (m2.find()) {
            int m = Integer.parseInt(m2.group(1));
            String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
            if (m >= 1 && m <= 12) {
                return months[m - 1] + " " + m2.group(2);
            }
        }

        Pattern p3 = Pattern.compile("^(\\d{4})$");
        Matcher m3 = p3.matcher(dateStr);
        if (m3.find()) {
            return m3.group(1);
        }

        return normalizeName(dateStr); // Fallback
    }
}
