package com.job_Portal_Backend.job_portal_backend.assessments.dto;

import com.job_Portal_Backend.job_portal_backend.assessments.entity.Question.DifficultyLevel;
import com.job_Portal_Backend.job_portal_backend.assessments.entity.Question.QuestionType;
import com.job_Portal_Backend.job_portal_backend.assessments.entity.Question.ProgrammingLanguage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionDto {
    private Long id;
    private Long assessmentId;
    private String title;
    private String description;
    private QuestionType type;
    private Integer marks;
    private Integer sequenceNumber;
    private DifficultyLevel difficulty;
    // MCQ
    private String option1;
    private String option2;
    private String option3;
    private String option4;
    private String correctAnswer;
    private String explanation;
    // Coding
    private String codeTemplate;
    private ProgrammingLanguage programmingLanguage;
    private String testCases;
    private String expectedOutput;
    private String sampleTestCases;
    private String hiddenTestCases;
    private String functionSignature;
    private String hiddenWrapperCode;
    private String constraintsText;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static QuestionDto fromQuestion(
            com.job_Portal_Backend.job_portal_backend.assessments.entity.Question question) {
        if (question == null)
            return null;
        QuestionDto dto = new QuestionDto();
        dto.setId(question.getId());
        if (question.getAssessment() != null) {
            dto.setAssessmentId(question.getAssessment().getId());
        }
        dto.setTitle(question.getTitle());
        dto.setDescription(question.getDescription());
        dto.setType(question.getType());
        dto.setMarks(question.getMarks());
        dto.setSequenceNumber(question.getSequenceNumber());
        dto.setDifficulty(question.getDifficulty());
        dto.setOption1(question.getOption1());
        dto.setOption2(question.getOption2());
        dto.setOption3(question.getOption3());
        dto.setOption4(question.getOption4());
        dto.setCorrectAnswer(question.getCorrectAnswer());
        dto.setExplanation(question.getExplanation());
        dto.setCodeTemplate(question.getCodeTemplate());
        dto.setProgrammingLanguage(question.getProgrammingLanguage());
        dto.setTestCases(question.getTestCases());
        dto.setExpectedOutput(question.getExpectedOutput());
        dto.setSampleTestCases(question.getSampleTestCases());
        dto.setHiddenTestCases(question.getHiddenTestCases());
        dto.setFunctionSignature(question.getFunctionSignature());
        dto.setHiddenWrapperCode(question.getHiddenWrapperCode());
        dto.setConstraintsText(question.getConstraintsText());
        dto.setCreatedAt(question.getCreatedAt());
        dto.setUpdatedAt(question.getUpdatedAt());
        return dto;
    }
}
