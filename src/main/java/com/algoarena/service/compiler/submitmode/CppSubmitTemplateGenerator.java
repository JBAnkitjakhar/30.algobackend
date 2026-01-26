// src/main/java/com/algoarena/service/compiler/submitmode/CppSubmitTemplateGenerator.java
package com.algoarena.service.compiler.submitmode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CppSubmitTemplateGenerator {

    private static final Logger logger = LoggerFactory.getLogger(CppSubmitTemplateGenerator.class);

    /**
     * For submit mode, admin provides complete template with all test cases
     * We just replace USER_CODE_PLACEHOLDER with user's solution
     */
    public String generateFromTemplate(String adminTemplate, String userCode) {
        logger.info("Generating C++ submit code from admin template...");
        logger.info("Admin template length: {} chars", adminTemplate.length());
        logger.info("User code length: {} chars", userCode.length());

        String finalCode = adminTemplate.replace("/*USER_CODE_PLACEHOLDER*/", userCode);

        logger.info("Final C++ submit code generated (length: {} chars)", finalCode.length());

        return finalCode;
    }
}