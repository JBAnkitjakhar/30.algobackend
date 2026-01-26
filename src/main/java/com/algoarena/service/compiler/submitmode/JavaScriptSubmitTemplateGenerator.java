// src/main/java/com/algoarena/service/compiler/submitmode/JavaScriptSubmitTemplateGenerator.java
package com.algoarena.service.compiler.submitmode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class JavaScriptSubmitTemplateGenerator {

    private static final Logger logger = LoggerFactory.getLogger(JavaScriptSubmitTemplateGenerator.class);

    /**
     * For submit mode, admin provides complete template with all test cases
     * We just replace USER_CODE_PLACEHOLDER with user's solution
     */
    public String generateFromTemplate(String adminTemplate, String userCode) {
        logger.info("Generating JavaScript submit code from admin template...");
        logger.info("Admin template length: {} chars", adminTemplate.length());
        logger.info("User code length: {} chars", userCode.length());

        String finalCode = adminTemplate.replace("/*USER_CODE_PLACEHOLDER*/", userCode);

        logger.info("Final JavaScript submit code generated (length: {} chars)", finalCode.length());

        return finalCode;
    }
}