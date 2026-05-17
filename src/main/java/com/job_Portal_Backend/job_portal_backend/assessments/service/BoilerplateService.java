package com.job_Portal_Backend.job_portal_backend.assessments.service;

import com.job_Portal_Backend.job_portal_backend.assessments.entity.Question;
import org.springframework.stereotype.Service;

@Service
public class BoilerplateService {

    public String buildExecutableCode(Question question, String userCode) {
        String code = userCode == null ? "" : userCode;
        String wrapper = question != null ? question.getHiddenWrapperCode() : null;

        if (wrapper != null && !wrapper.isBlank()) {
            return wrapper
                    .replace("{{USER_CODE}}", code)
                    .replace("${USER_CODE}", code);
        }

        return code;
    }
}
