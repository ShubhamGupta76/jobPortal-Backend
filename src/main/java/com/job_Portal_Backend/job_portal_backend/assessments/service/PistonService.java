package com.job_Portal_Backend.job_portal_backend.assessments.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class PistonService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${piston.api.url:http://localhost:2000}")
    private String pistonApiUrl;

    @SuppressWarnings("unchecked")
    public PistonExecutionResult execute(String language, String version, String code, String stdin) {
        Map<String, Object> payload = Map.of(
                "language", mapLanguage(language),
                "version", version == null || version.isBlank() ? "*" : version,
                "stdin", stdin == null ? "" : stdin,
                "files", List.of(Map.of("content", code == null ? "" : code))
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Map> response = restTemplate.exchange(
                pistonApiUrl + "/api/v2/execute",
                HttpMethod.POST,
                new HttpEntity<>(payload, headers),
                Map.class
        );

        Map<String, Object> body = response.getBody();
        Map<String, Object> run = body != null ? (Map<String, Object>) body.get("run") : null;

        PistonExecutionResult result = new PistonExecutionResult();
        if (run == null) {
            result.setStderr("Piston did not return a run result");
            result.setStatus("ERROR");
            return result;
        }

        result.setStdout(asString(run.get("stdout")));
        result.setStderr(asString(run.get("stderr")));
        result.setOutput(asString(run.get("output")));
        result.setCode(asInteger(run.get("code")));
        result.setStatus(asString(run.get("status")));
        result.setMessage(asString(run.get("message")));
        result.setCpuTimeMs(asLong(run.get("cpu_time")));
        result.setWallTimeMs(asLong(run.get("wall_time")));
        result.setMemoryBytes(asLong(run.get("memory")));
        return result;
    }

    private String mapLanguage(String language) {
        if (language == null) {
            return "python";
        }
        return switch (language.trim().toUpperCase()) {
            case "CPP", "C++" -> "c++";
            case "JAVASCRIPT", "JS", "NODE" -> "javascript";
            case "PYTHON", "PY" -> "python";
            case "JAVA" -> "java";
            case "C" -> "c";
            default -> language.toLowerCase();
        };
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Integer asInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return null;
    }

    private Long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return null;
    }
}
