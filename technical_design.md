# Technical Design: Multi-Source Candidate Data Transformer

This document outlines the high-level system design, pipeline mechanics, schema normalizations, conflict resolution rules, and edge-case behaviors of the **TalentMerge** transformer platform.

---

## 1. Data Pipeline Breakdown
The candidate transformation pipeline executes in a modular, decoupled flow:
`Detect & Load` $\rightarrow$ `Extract Raw` $\rightarrow$ `Normalize Formats` $\rightarrow$ `Match & Deduplicate` $\rightarrow$ `Resolve Conflicts` $\rightarrow$ `Compute Confidence` $\rightarrow$ `Project Output` $\rightarrow$ `Validate Schema`

1.  **Detect & Ingest**: Receives uploads (CSV spreadsheet rows or unstructured PDF resumes).
2.  **Extract Raw**: Parses files using dedicated processors (`OpenCSV` / `PDFBox`), storing unmerged records in a raw candidate table.
3.  **Normalize Formats**: Cleans name spacing, reformats phone numbers to `E.164`, and runs raw skill strings against a canonical dictionary.
4.  **Match & Deduplicate**: Computes Jaro-Winkler string similarity scores on candidate names alongside exact checks on emails/phones to isolate duplicates.
5.  **Resolve Conflicts**: Merges attributes from multiple raw sources using source priority rules (PDF overrides CSV) and computes an overall confidence weight.
6.  **Project & Validate**: Projects the canonical candidate records into a client's custom schema at runtime using a JSON configuration.

---

## 2. Canonical Output Schema & Normalizations
The canonical record acts as the single source of truth:
*   **Canonical Profile**: `id`, `fullName`, `headline`, `location`, `yearsOfExperience`, `overallConfidence`, and child lists of standardized emails, phones, URLs, skills, experiences, and educations.
*   **Normalized Formats**:
    *   *Names*: Stripped of leading/trailing spaces, multiple inner spaces collapsed, formatted to Title Case.
    *   *Phones*: Formatted to **E.164** format (e.g. `+11234567890`) via a cleaning regex pattern.
    *   *Skills*: Standardized using a lookup table (e.g. mapping `reactjs` or `react.js` to `React`).
    *   *Timelines*: Job experience and education dates standardized to ISO-8601 strings.

---

## 3. Merge & Conflict-Resolution Policy
*   **Match Keys**: Exact email match, exact phone match, or weighted Jaro-Winkler string similarity score $\ge 0.85$ on names.
*   **Conflict Resolution**: Staged prioritizations are applied. Higher fidelity sources are weighted over structured ones:
    $$\text{PDF Resume (Priority 2)} > \text{Recruiter CSV Export (Priority 1)}$$
    If priorities are equal, the system resolves conflicts using the most recently extracted record timestamp.
*   **Confidence Weights**: Calculated by aggregating individual field extraction scores (degraded if parsed using fallback text regex search rather than structured grids).

---

## 4. Runtime Custom-Output Configuration
To support dynamic client schemas, the **Projection Engine** interprets custom configurations at runtime:
*   **Path Mapping**: Resolves paths like `fullName` to custom keys (e.g. `name`) and handles array indices (`emails[0]`) or wildcards (`skills[*].name`).
*   **Missing Values**: Handled using one of three strategies:
    *   `null`: Inserts `null` if the property is absent.
    *   `omit`: Completely excludes the key from the output JSON.
    *   `error`: Throws a `SchemaProjectionException` (400 Bad Request) if a required attribute is missing.
*   **Metadata Stripping**: Dynamically filters out `provenance` and `confidence` fields if toggled off in the config.

---

## 5. Edge Cases & Scope Omissions

### Edge Cases Handled
1.  **Malformed Phone Formatting**: Input phones with extensions or text (e.g. `123-456-7890 ext 45`) are cleaned by regex, extracting digits to construct valid E.164 outputs.
2.  **Wildcard Inconsistencies**: If a wildcard path is requested (e.g., `skills[*].name`) but the candidate has an empty skills list, the engine outputs an empty array `[]` instead of raising a null-pointer crash.
3.  **Transitive Merge Chains**: If Raw A matches Raw B (by phone) and Raw B matches Raw C (by email), the system groups them recursively during pipeline execution to merge all three into a single canonical record.
4.  **Schema Validation Failures**: When a required custom field is missing and `on_missing: "error"` is set, the engine halts and throws a validation response listing the missing paths.

### Deliberate Omissions Under Time Pressure
1.  **Multi-lingual PDF Parsing**: Optical Character Recognition (OCR) and NLP parsing for foreign language resumes are omitted.
2.  **Distributed Pipeline Locks**: Concurrency issues during large batch runs are handled via serializable DB transactions rather than Redis distributed locking.
3.  **Dynamic OpenAPI Document Generation**: The server does not auto-generate updated API schema definitions on-the-fly for custom projections.
