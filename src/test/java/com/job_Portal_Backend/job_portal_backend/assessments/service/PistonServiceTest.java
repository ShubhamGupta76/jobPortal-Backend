package com.job_Portal_Backend.job_portal_backend.assessments.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * PISTON_API_URL is optional in production (see application-prod.yaml) so a missing/unconfigured
 * Piston deployment never crashes app startup. This locks in the resulting requirement: a
 * code-execution request must fail gracefully (503, clear message) rather than attempting a call
 * to a blank/malformed URL or silently reporting success.
 */
class PistonServiceTest {

    private final PistonService pistonService = new PistonService();

    @Test
    void blankPistonUrlFailsGracefullyInsteadOfAttemptingAnHttpCall() {
        ReflectionTestUtils.setField(pistonService, "pistonApiUrl", "");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> pistonService.execute("python", "*", "print('hi')", ""));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatusCode());
        assertEquals("Code execution service is not configured", ex.getReason());
    }

    @Test
    void nullPistonUrlFailsGracefully() {
        ReflectionTestUtils.setField(pistonService, "pistonApiUrl", null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> pistonService.execute("python", "*", "print('hi')", ""));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatusCode());
    }
}
