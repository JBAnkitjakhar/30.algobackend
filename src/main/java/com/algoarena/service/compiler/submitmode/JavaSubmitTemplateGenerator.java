// src/main/java/com/algoarena/service/compiler/submitmode/JavaSubmitTemplateGenerator.java
package com.algoarena.service.compiler.submitmode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class JavaSubmitTemplateGenerator {

    private static final Logger logger = LoggerFactory.getLogger(JavaSubmitTemplateGenerator.class);

    /**
     * Generate from admin template (used when admin template is available in DB)
     */
    public String generateFromTemplate(String adminTemplate, String userCode) {
        logger.info("Generating Java submit code from admin template...");
        logger.info("Admin template length: {} chars", adminTemplate.length());
        logger.info("User code length: {} chars", userCode.length());

        String finalCode = adminTemplate.replace("/*USER_CODE_PLACEHOLDER*/", userCode);

        logger.info("Final Java submit code generated (length: {} chars)", finalCode.length());

        return finalCode;
    }
}