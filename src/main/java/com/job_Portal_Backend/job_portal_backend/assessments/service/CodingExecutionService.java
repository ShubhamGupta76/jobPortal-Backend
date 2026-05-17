package com.job_Portal_Backend.job_portal_backend.assessments.service;

import com.job_Portal_Backend.job_portal_backend.assessments.dto.CodeExecutionRequest;
import com.job_Portal_Backend.job_portal_backend.assessments.dto.CodeExecutionResponse;
import com.job_Portal_Backend.job_portal_backend.assessments.dto.TestCaseResultDto;
import com.job_Portal_Backend.job_portal_backend.assessments.entity.Question;
import com.job_Portal_Backend.job_portal_backend.assessments.repository.QuestionRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CodingExecutionService {

    private final PistonService pistonService;
    private final BoilerplateService boilerplateService;
    private final TestCaseParserService testCaseParserService;
    private final QuestionRepository questionRepository;

    public CodingExecutionService(
            PistonService pistonService,
            BoilerplateService boilerplateService,
            TestCaseParserService testCaseParserService,
            QuestionRepository questionRepository) {
        this.pistonService = pistonService;
        this.boilerplateService = boilerplateService;
        this.testCaseParserService = testCaseParserService;
        this.questionRepository = questionRepository;
    }

    public CodeExecutionResponse execute(CodeExecutionRequest request) {
        Question question = request.getQuestionId() != null
                ? questionRepository.findById(request.getQuestionId()).orElse(null)
                : null;
        boolean hidden = Boolean.TRUE.equals(request.getRunHiddenTests());
        List<CodingTestCase> cases = question != null
                ? testCaseParserService.getCases(question, hidden)
                : testCaseParserService.parse(request.getInput());

        if (cases.isEmpty()) {
            cases = List.of(new CodingTestCase(request.getInput(), null));
        }

        String executableCode = boilerplateService.buildExecutableCode(question, request.getCode());
        String language = request.getLanguage();
        if ((language == null || language.isBlank()) && question != null && question.getProgrammingLanguage() != null) {
            language = question.getProgrammingLanguage().name();
        }

        List<TestCaseResultDto> results = new ArrayList<>();
        int passed = 0;
        long totalRuntime = 0;
        long maxMemory = 0;

        for (int i = 0; i < cases.size(); i++) {
            CodingTestCase testCase = cases.get(i);
            PistonExecutionResult pistonResult = pistonService.execute(language, "*", executableCode, testCase.getInput());
            String actual = pistonResult.getStdout() != null ? pistonResult.getStdout() : "";
            boolean hasExpected = testCase.getExpectedOutput() != null && !testCase.getExpectedOutput().isBlank();
            boolean casePassed = hasExpected && normalize(actual).equals(normalize(testCase.getExpectedOutput()))
                    && (pistonResult.getCode() == null || pistonResult.getCode() == 0)
                    && (pistonResult.getStatus() == null || pistonResult.getStatus().isBlank());

            if (casePassed) {
                passed++;
            }
            totalRuntime += pistonResult.getWallTimeMs() != null ? pistonResult.getWallTimeMs() : 0;
            maxMemory = Math.max(maxMemory, pistonResult.getMemoryBytes() != null ? pistonResult.getMemoryBytes() : 0);

            results.add(new TestCaseResultDto(
                    i + 1,
                    testCase.getInput(),
                    testCase.getExpectedOutput(),
                    actual,
                    pistonResult.getStderr(),
                    casePassed,
                    pistonResult.getCode(),
                    pistonResult.getStatus(),
                    pistonResult.getWallTimeMs(),
                    pistonResult.getMemoryBytes()
            ));
        }

        CodeExecutionResponse response = new CodeExecutionResponse();
        response.setTestCases(results);
        response.setPassedCount(passed);
        response.setTotalCount(results.size());
        response.setScorePercentage(results.isEmpty() ? 0.0 : (passed * 100.0) / results.size());
        response.setExecutionTime(totalRuntime);
        response.setMemoryUsed(maxMemory);
        response.setSuccess(results.stream().allMatch(TestCaseResultDto::getPassed));
        response.setOutput(results.isEmpty() ? "" : results.get(results.size() - 1).getActualOutput());
        response.setError(results.stream()
                .map(TestCaseResultDto::getStderr)
                .filter(stderr -> stderr != null && !stderr.isBlank())
                .findFirst()
                .orElse(null));
        return response;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }
}
