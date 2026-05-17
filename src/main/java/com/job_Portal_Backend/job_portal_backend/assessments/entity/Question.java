package com.job_Portal_Backend.job_portal_backend.assessments.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;

@Entity
@Table(name = "questions")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assessment_id", nullable = false)
    private Assessment assessment;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuestionType type;

    @Column(nullable = false)
    private Integer marks;

    @Column(nullable = false)
    private Integer sequenceNumber;

    @Enumerated(EnumType.STRING)
    private DifficultyLevel difficulty = DifficultyLevel.MEDIUM;

    // For MCQ
    @Column(columnDefinition = "TEXT")
    private String option1;

    @Column(columnDefinition = "TEXT")
    private String option2;

    @Column(columnDefinition = "TEXT")
    private String option3;

    @Column(columnDefinition = "TEXT")
    private String option4;

    @Column(columnDefinition = "TEXT")
    private String correctAnswer;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    // For Coding
    @Column(columnDefinition = "TEXT")
    private String codeTemplate;

    @Enumerated(EnumType.STRING)
    private ProgrammingLanguage programmingLanguage;

    @Column(columnDefinition = "TEXT")
    private String testCases;

    @Column(columnDefinition = "TEXT")
    private String expectedOutput;

    @Column(columnDefinition = "TEXT")
    private String sampleTestCases;

    @Column(columnDefinition = "TEXT")
    private String hiddenTestCases;

    @Column(columnDefinition = "TEXT")
    private String functionSignature;

    @Column(columnDefinition = "TEXT")
    private String hiddenWrapperCode;

    @Column(columnDefinition = "TEXT")
    private String constraintsText;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public enum QuestionType {
        MCQ, CODING, DESCRIPTIVE
    }

    public enum DifficultyLevel {
        EASY, MEDIUM, HARD
    }

    public enum ProgrammingLanguage {
        JAVA, PYTHON, JAVASCRIPT, CPP, C, CSHARP, GO, RUST, SQL
    }
}
