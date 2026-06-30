package com.candidate.transformer.engine;

import com.candidate.transformer.entity.Candidate;
import com.candidate.transformer.entity.RawCandidate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Collections;

@Component
@Slf4j
public class MatchingEngine {

    @Value("${app.matching.threshold:0.8}")
    private double threshold;

    @Value("${app.matching.weight.email:0.4}")
    private double emailWeight;

    @Value("${app.matching.weight.phone:0.2}")
    private double phoneWeight;

    @Value("${app.matching.weight.links:0.2}")
    private double linksWeight;

    @Value("${app.matching.weight.name:0.2}")
    private double nameWeight;

    public boolean isDuplicate(RawCandidate raw, Candidate canonical) {
        // Short-circuit: if emails match exactly, they are definitely duplicates
        String rawEmail = cleanEmail(raw.getEmail());
        if (!rawEmail.isEmpty()) {
            boolean hasMatchingEmail = canonical.getEmails().stream()
                    .anyMatch(e -> cleanEmail(e.getEmail()).equals(rawEmail));
            if (hasMatchingEmail) {
                log.debug("Duplicate detected: Exact email match for {}", rawEmail);
                return true;
            }
        }

        // Short-circuit: if github/linkedin match exactly, they are duplicates
        // (We check links mapped in canonical)
        List<String> rawLinks = extractLinksList(raw.getLinksJson());
        for (String rLink : rawLinks) {
            String cleanRawLink = cleanLink(rLink);
            if (cleanRawLink.isEmpty()) continue;
            boolean hasMatchingLink = canonical.getLinks().stream()
                    .anyMatch(l -> cleanLink(l.getUrl()).equals(cleanRawLink));
            if (hasMatchingLink) {
                log.debug("Duplicate detected: Exact link match for {}", cleanRawLink);
                return true;
            }
        }

        // Weighted calculations
        double totalWeight = 0.0;
        double weightedScore = 0.0;

        // 1. Email check (if present in both but not exactly matching)
        if (!rawEmail.isEmpty() && !canonical.getEmails().isEmpty()) {
            boolean matches = canonical.getEmails().stream()
                    .anyMatch(e -> cleanEmail(e.getEmail()).equals(rawEmail));
            weightedScore += (matches ? 1.0 : 0.0) * emailWeight;
            totalWeight += emailWeight;
        }

        // 2. Phone check
        String rawPhone = cleanPhone(raw.getPhone());
        if (!rawPhone.isEmpty() && !canonical.getPhones().isEmpty()) {
            boolean matches = canonical.getPhones().stream()
                    .anyMatch(p -> cleanPhone(p.getPhone()).equals(rawPhone));
            weightedScore += (matches ? 1.0 : 0.0) * phoneWeight;
            totalWeight += phoneWeight;
        }

        // 3. Links check
        if (!rawLinks.isEmpty() && !canonical.getLinks().isEmpty()) {
            boolean matches = false;
            for (String rl : rawLinks) {
                String crl = cleanLink(rl);
                if (crl.isEmpty()) continue;
                if (canonical.getLinks().stream().anyMatch(l -> cleanLink(l.getUrl()).equals(crl))) {
                    matches = true;
                    break;
                }
            }
            weightedScore += (matches ? 1.0 : 0.0) * linksWeight;
            totalWeight += linksWeight;
        }

        // 4. Name check (Jaro-Winkler similarity)
        String rawName = raw.getFullName();
        String canonicalName = canonical.getFullName();
        if (rawName != null && !rawName.trim().isEmpty() && canonicalName != null && !canonicalName.trim().isEmpty()) {
            double sim = calculateJaroWinkler(rawName, canonicalName);
            weightedScore += sim * nameWeight;
            totalWeight += nameWeight;
        }

        if (totalWeight == 0.0) {
            return false;
        }

        double finalScore = weightedScore / totalWeight;
        log.debug("Matching score between Raw[{}] and Canonical[ID: {}]: {}", raw.getFullName(), canonical.getId(), finalScore);
        
        return finalScore >= threshold;
    }

    private String cleanEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private String cleanPhone(String phone) {
        return phone == null ? "" : phone.replaceAll("[^0-9]", "");
    }

    private String cleanLink(String link) {
        if (link == null) return "";
        return link.toLowerCase()
                .replace("https://", "")
                .replace("http://", "")
                .replace("www.", "")
                .replaceAll("/+$", "")
                .trim();
    }

    private List<String> extractLinksList(String linksJson) {
        if (linksJson == null || linksJson.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(linksJson, List.class);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public double calculateJaroWinkler(String s1, String s2) {
        if (s1 == null || s2 == null) return 0.0;
        s1 = s1.toLowerCase().trim();
        s2 = s2.toLowerCase().trim();
        if (s1.equals(s2)) return 1.0;

        int l1 = s1.length();
        int l2 = s2.length();
        int matchWindow = Math.max(1, Math.max(l1, l2) / 2 - 1);

        boolean[] m1 = new boolean[l1];
        boolean[] m2 = new boolean[l2];

        int matches = 0;
        for (int i = 0; i < l1; i++) {
            int start = Math.max(0, i - matchWindow);
            int end = Math.min(l2 - 1, i + matchWindow);
            for (int j = start; j <= end; j++) {
                if (!m2[j] && s1.charAt(i) == s2.charAt(j)) {
                    m1[i] = true;
                    m2[j] = true;
                    matches++;
                    break;
                }
            }
        }

        if (matches == 0) return 0.0;

        int transpositions = 0;
        int k = 0;
        for (int i = 0; i < l1; i++) {
            if (m1[i]) {
                while (!m2[k]) k++;
                if (s1.charAt(i) != s2.charAt(k)) {
                    transpositions++;
                }
                k++;
            }
        }

        double jaro = ((double) matches / l1 + (double) matches / l2 + (double) (matches - transpositions / 2.0) / matches) / 3.0;
        
        // Winkler modification
        int prefixLen = 0;
        for (int i = 0; i < Math.min(4, Math.min(l1, l2)); i++) {
            if (s1.charAt(i) == s2.charAt(i)) {
                prefixLen++;
            } else {
                break;
            }
        }

        double p = 0.1; // Scaling factor
        return jaro + prefixLen * p * (1.0 - jaro);
    }
}
