package com.job_Portal_Backend.job_portal_backend.assessments.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.job_Portal_Backend.job_portal_backend.assessments.entity.Question;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TestCaseParserService {

    private final ObjectMapper objectMapper;

    public TestCaseParserService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<CodingTestCase> getCases(Question question, boolean hidden) {
        String source = hidden ? question.getHiddenTestCases() : question.getSampleTestCases();
        if ((source == null || source.isBlank()) && !hidden) {
            source = question.getTestCases();
        }

        List<CodingTestCase> parsed = parse(source);
        if (parsed.isEmpty() && !hidden && question.getExpectedOutput() != null) {
            parsed.add(new CodingTestCase(question.getTestCases(), question.getExpectedOutput()));
        }
        return parsed;
    }

    public List<CodingTestCase> parse(String raw) {
        List<CodingTestCase> cases = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return cases;
        }

        String trimmed = raw.trim();
        if (trimmed.startsWith("[")) {
            try {
                return objectMapper.readValue(trimmed, new TypeReference<List<CodingTestCase>>() {});
            } catch (Exception ignored) {
                // Fall back to the plain-text parser.
            }
        }

        String[] blocks = trimmed.split("(?m)^---\\s*$");
        for (String block : blocks) {
            CodingTestCase testCase = parseBlock(block.trim());
            if (testCase.getInput() != null || testCase.getExpectedOutput() != null) {
                cases.add(testCase);
            }
        }
        return cases;
    }

    private CodingTestCase parseBlock(String block) {
        String input = "";
        String expected = "";
        String[] lines = block.split("\\R");
        String section = "input";
        StringBuilder inputBuilder = new StringBuilder();
        StringBuilder expectedBuilder = new StringBuilder();

        for (String line : lines) {
            String lower = line.trim().toLowerCase();
            if (lower.startsWith("input:")) {
                section = "input";
                append(inputBuilder, line.substring(line.indexOf(':') + 1).trim());
                continue;
            }
            if (lower.startsWith("expected:") || lower.startsWith("expected output:")) {
                section = "expected";
                append(expectedBuilder, line.substring(line.indexOf(':') + 1).trim());
                continue;
            }
            if ("expected".equals(section)) {
                append(expectedBuilder, line);
            } else {
                append(inputBuilder, line);
            }
        }

        input = inputBuilder.toString();
        expected = expectedBuilder.toString();
        return new CodingTestCase(input, expected);
    }

    private void append(StringBuilder builder, String value) {
        if (value == null) {
            return;
        }
        if (builder.length() > 0) {
            builder.append(System.lineSeparator());
        }
        builder.append(value);
    }
}
