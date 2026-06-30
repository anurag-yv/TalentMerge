package com.candidate.transformer.engine;

import com.candidate.transformer.dto.CanonicalProfileDto;
import com.candidate.transformer.dto.FieldProjection;
import com.candidate.transformer.dto.ProjectionConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProjectionEngine {

    private final NormalizationEngine normalizationEngine;
    private final ObjectMapper objectMapper;

    /**
     * Projects a canonical candidate profile into a custom-configured schema.
     */
    public Map<String, Object> project(CanonicalProfileDto profile, ProjectionConfig config) {
        if (profile == null) {
            return null;
        }

        Map<String, Object> projected = new LinkedHashMap<>();
        JsonNode profileNode = objectMapper.valueToTree(profile);

        // Process configured fields
        if (config.getFields() != null) {
            for (FieldProjection field : config.getFields()) {
                String destinationPath = field.getPath();
                String sourcePath = field.getFrom() != null ? field.getFrom() : field.getPath();

                JsonNode rawValueNode = evaluatePath(profileNode, sourcePath);
                Object finalValue = processValue(rawValueNode, field);

                if (finalValue == null || isValueEmpty(finalValue)) {
                    if (field.isRequired()) {
                        throw new IllegalArgumentException("Required field is missing: " + destinationPath);
                    }

                    String onMissing = config.getOnMissing() != null ? config.getOnMissing().toLowerCase() : "null";
                    switch (onMissing) {
                        case "error":
                            throw new IllegalArgumentException("Field is missing: " + destinationPath);
                        case "omit":
                            // Do not add key
                            continue;
                        case "null":
                        default:
                            projected.put(destinationPath, null);
                            break;
                    }
                } else {
                    projected.put(destinationPath, finalValue);
                }
            }
        }

        // Add overall confidence if enabled
        if (config.isIncludeConfidence()) {
            projected.put("overall_confidence", profile.getOverallConfidence());
        }

        // Add provenance block if enabled
        if (config.isIncludeProvenance()) {
            List<Map<String, Object>> provenanceList = new ArrayList<>();
            if (profile.getFieldProvenances() != null) {
                for (var prov : profile.getFieldProvenances()) {
                    Map<String, Object> provMap = new LinkedHashMap<>();
                    provMap.put("field", prov.getFieldName());
                    if (prov.getProvenance() != null) {
                        provMap.put("source", prov.getProvenance().getSourceName());
                        provMap.put("method", prov.getProvenance().getExtractionMethod() != null ? prov.getProvenance().getExtractionMethod().name() : null);
                    } else {
                        provMap.put("source", null);
                        provMap.put("method", null);
                    }
                    provenanceList.add(provMap);
                }
            }
            projected.put("provenance", provenanceList);
        }

        return projected;
    }

    private JsonNode evaluatePath(JsonNode root, String path) {
        if (path == null || path.trim().isEmpty()) {
            return null;
        }

        // Standardize common snake_case/camelCase queries
        String normalizedPath = path
                .replace("full_name", "fullName")
                .replace("years_experience", "yearsOfExperience")
                .replace("overall_confidence", "overallConfidence");

        return evaluateNormalizedPath(root, normalizedPath);
    }

    private JsonNode evaluateNormalizedPath(JsonNode node, String path) {
        String[] parts = path.split("\\.");
        JsonNode current = node;

        for (int i = 0; i < parts.length; i++) {
            if (current == null || current.isMissingNode() || current.isNull()) {
                return null;
            }

            String part = parts[i];

            if (part.contains("[")) {
                String fieldName = part.substring(0, part.indexOf("["));
                String indexStr = part.substring(part.indexOf("[") + 1, part.indexOf("]"));

                if (!fieldName.isEmpty()) {
                    current = getFieldCaseInsensitive(current, fieldName);
                }

                if (current == null || !current.isArray()) {
                    return null;
                }

                if ("*".equals(indexStr)) {
                    if (i == parts.length - 1) {
                        return current;
                    } else {
                        // Gather remaining sub-path
                        StringBuilder subPathBuilder = new StringBuilder();
                        for (int j = i + 1; j < parts.length; j++) {
                            if (subPathBuilder.length() > 0) subPathBuilder.append(".");
                            subPathBuilder.append(parts[j]);
                        }
                        String subPath = subPathBuilder.toString();

                        ArrayNode resultArray = JsonNodeFactory.instance.arrayNode();
                        for (JsonNode item : current) {
                            JsonNode subResult = evaluateNormalizedPath(item, subPath);
                            if (subResult != null && !subResult.isMissingNode() && !subResult.isNull()) {
                                resultArray.add(extractValue(subResult));
                            }
                        }
                        return resultArray;
                    }
                } else {
                    try {
                        int index = Integer.parseInt(indexStr);
                        if (index >= 0 && index < current.size()) {
                            current = current.get(index);
                        } else {
                            return null;
                        }
                    } catch (NumberFormatException e) {
                        return null;
                    }
                }
            } else {
                current = getFieldCaseInsensitive(current, part);
            }
        }

        return current;
    }

    private JsonNode getFieldCaseInsensitive(JsonNode node, String fieldName) {
        if (node == null || !node.isObject()) return null;
        JsonNode val = node.get(fieldName);
        if (val != null) return val;

        Iterator<String> fieldNames = node.fieldNames();
        while (fieldNames.hasNext()) {
            String name = fieldNames.next();
            if (name.equalsIgnoreCase(fieldName)) {
                return node.get(name);
            }
        }

        // Smart translation fallback to map generic fields like "name" to internal properties
        if ("name".equalsIgnoreCase(fieldName)) {
            if (node.has("skillName")) return node.get("skillName");
            if (node.has("email")) return node.get("email");
            if (node.has("phone")) return node.get("phone");
            if (node.has("url")) return node.get("url");
        }
        return null;
    }

    private JsonNode extractValue(JsonNode node) {
        if (node == null) return null;
        if (node.isObject()) {
            if (node.has("email")) return node.get("email");
            if (node.has("phone")) return node.get("phone");
            if (node.has("url")) return node.get("url");
            if (node.has("skillName")) return node.get("skillName");
            if (node.has("name")) return node.get("name");
        }
        return node;
    }

    private Object processValue(JsonNode node, FieldProjection field) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }

        String type = field.getType() != null ? field.getType().toLowerCase() : "string";
        String normalize = field.getNormalize();

        if (type.equals("string[]") || node.isArray()) {
            List<String> list = new ArrayList<>();
            if (node.isArray()) {
                for (JsonNode item : node) {
                    JsonNode extracted = extractValue(item);
                    String val = extracted != null ? extracted.asText() : null;
                    if (val != null) {
                        list.add(applyNormalization(val, normalize));
                    }
                }
            } else {
                JsonNode extracted = extractValue(node);
                String val = extracted != null ? extracted.asText() : null;
                if (val != null) {
                    list.add(applyNormalization(val, normalize));
                }
            }
            return list;
        } else if (type.equals("number")) {
            JsonNode extracted = extractValue(node);
            return extracted != null ? extracted.asDouble() : null;
        } else if (type.equals("boolean")) {
            JsonNode extracted = extractValue(node);
            return extracted != null ? extracted.asBoolean() : null;
        } else {
            // Default to string
            JsonNode extracted = extractValue(node);
            String val = extracted != null ? extracted.asText() : null;
            return applyNormalization(val, normalize);
        }
    }

    private String applyNormalization(String val, String normalize) {
        if (val == null || normalize == null) {
            return val;
        }

        switch (normalize.toUpperCase()) {
            case "E164":
                return normalizationEngine.normalizePhone(val);
            case "CANONICAL":
                // Standardize skills or strings using title casing and normalization
                return normalizationEngine.normalizeSkill(val);
            default:
                return val;
        }
    }

    private boolean isValueEmpty(Object val) {
        if (val instanceof String) {
            return ((String) val).trim().isEmpty();
        } else if (val instanceof Collection) {
            return ((Collection<?>) val).isEmpty();
        }
        return false;
    }
}
