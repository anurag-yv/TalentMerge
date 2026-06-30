# Technical Design: Multi-Source Candidate Data Transformer (TalentMerge)

This document outlines the architecture, pipeline stages, deduplication strategy, conflict-resolution policies, and dynamic schema projection layer of the **TalentMerge** data transformer platform.

---

## 1. System Pipeline Architecture
The transformation engine ingests multi-source profiles and executes a decoupled, transactional pipeline:
`Ingest & Load` $\rightarrow$ `Text Extraction` $\rightarrow$ `Sanitize & Normalize` $\rightarrow$ `Deduplication (Similarity Engine)` $\rightarrow$ `Conflict Resolution` $\rightarrow$ `Overall Confidence Calculation` $\rightarrow$ `Dynamic Schema Projection`

*   **Ingestion & Parsing:** Parallel upload processors ingest structured tabular data (`OpenCSV`) and unstructured resume PDF streams (`Apache PDFBox`).
*   **Normalized Baseline:** Raw attributes are cleaned immediately: names are title-cased and whitespace-trimmed; phone numbers are cleaned via regex to E.164 formats (e.g. `+11234567890`); skill tags are standardized via an alias dictionary mapping (e.g. `react.js` $\rightarrow$ `React`).

---

## 2. Match, Merge & Deduplication Policy
To resolve duplicate records across disparate sources, the engine employs a tiered matching algorithm:
*   **Tier 1 (Exact Match):** Short-circuits matches immediately on identical emails or phone numbers.
*   **Tier 2 (Fuzzy Match):** Computes **Weighted Jaro-Winkler string similarity** on candidate names. Profiles with similarity scores $\ge 0.85$ are flagged as matches.
*   **Transitive Deduplication:** Matches are clustered recursively. If raw Candidate A matches B, and B matches C, the engine merges all three into a single, unified canonical profile.

---

## 3. Configurable Conflict Resolution & Provenance
*   **Source Priority Weights:** Field conflicts are resolved using deterministic priorities:
    $$\text{PDF Resume (Priority Weight: 2)} > \text{Recruiter CSV Export (Priority Weight: 1)}$$
    If priorities are equal, the system resolves conflicts using the most recently extracted timestamp.
*   **Confidence Scoring:** The system computes extraction confidence dynamically (e.g. degrading field scores for regex fallback parsing vs. structured fields) and aggregates them into an overall profile confidence.
*   **Immutable Provenance Audits:** Every canonical field maintains an audit link tracing back to the source filename, extraction method, date, and field-level confidence score.

---

## 4. Runtime Configurable Projection Layer
To output custom schemas dynamically without database migrations or code rebuilds, the engine evaluates projection configurations at runtime:
*   **Path Mapping:** Evaluates expressions to map properties (e.g., `fullName` $\rightarrow$ `name`), indices (`emails[0]`), and wildcards (`skills[*].name`).
*   **Missing Fields Strategy:** Operates on the top-level client configuration:
    *   `null`: Standardizes absent properties as JSON `null`.
    *   `omit`: Filters out keys entirely from the payload.
    *   `error`: Returns an HTTP `400 Bad Request` validation listing missing fields if `"required": true` constraints fail.
*   **Metadata Toggle:** Selectively includes/strips provenance records and confidence weights.

---

## 5. Resiliency & Edge Cases Resolved
1.  **Robust Normalization:** Ingested telephone fields containing prose or extensions (e.g., `(123) 456-7890 ext. 4`) are processed via regex to extract valid digits.
2.  **Safe Wildcards:** If a wildcard mapping (e.g., `skills[*].name`) is requested for a candidate with no skills, the engine outputs an empty array `[]` instead of raising a NullPointerException.
3.  **H2/PostgreSQL Context Separation:** Employs JPA transaction boundaries to isolate staging tables from production records during merge iterations.
