package com.candidate.transformer.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "candidates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Candidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name")
    private String fullName;

    private String headline;
    private String location;

    @Column(name = "years_of_experience")
    private Double yearsOfExperience;

    @Column(name = "overall_confidence")
    private Double overallConfidence;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CandidateEmail> emails = new ArrayList<>();

    @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CandidatePhone> phones = new ArrayList<>();

    @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CandidateLink> links = new ArrayList<>();

    @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CandidateSkill> skills = new ArrayList<>();

    @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CandidateExperience> experiences = new ArrayList<>();

    @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CandidateEducation> educations = new ArrayList<>();

    @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CandidateProject> projects = new ArrayList<>();

    @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<FieldProvenance> fieldProvenances = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Helper methods for bi-directional synchronization
    public void addEmail(CandidateEmail email) {
        emails.add(email);
        email.setCandidate(this);
    }

    public void addPhone(CandidatePhone phone) {
        phones.add(phone);
        phone.setCandidate(this);
    }

    public void addLink(CandidateLink link) {
        links.add(link);
        link.setCandidate(this);
    }

    public void addSkill(CandidateSkill skill) {
        skills.add(skill);
        skill.setCandidate(this);
    }

    public void addExperience(CandidateExperience exp) {
        experiences.add(exp);
        exp.setCandidate(this);
    }

    public void addEducation(CandidateEducation edu) {
        educations.add(edu);
        edu.setCandidate(this);
    }

    public void addProject(CandidateProject proj) {
        projects.add(proj);
        proj.setCandidate(this);
    }

    public void addFieldProvenance(FieldProvenance prov) {
        fieldProvenances.add(prov);
        prov.setCandidate(this);
    }
}
