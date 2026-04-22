package com.job_Portal_Backend.job_portal_backend.assessments.repository;

import com.job_Portal_Backend.job_portal_backend.assessments.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByAssessmentIdOrderBySequenceNumber(Long assessmentId);

    List<Question> findByAssessmentId(Long assessmentId);
}
