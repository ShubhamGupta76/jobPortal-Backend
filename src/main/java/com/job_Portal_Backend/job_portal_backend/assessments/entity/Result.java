package com.job_Portal_Backend.job_portal_backend.assessments.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "results")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_session_id", nullable = false, unique = true)
    private TestSession testSession;

    @Column(nullable = false)
    private Double totalMarksObtained = 0.0;

    @Column(nullable = false)
    private Double maxMarks;

    @Column(nullable = false)
    private Double percentageScore = 0.0;

    @Column(nullable = false)
    private Boolean passed = false;

    @Column(nullable = false)
    private Integer correctAnswers = 0;

    @Column(nullable = false)
    private Integer totalQuestions = 0;

    @Column(columnDefinition = "TEXT")
    private String detailedAnalysis;

    private LocalDateTime resultPublishedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
}
