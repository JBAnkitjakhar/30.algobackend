// src/main/java/com/algoarena/service/compiler/submitmode/JavaSubmitTemplateGenerator.java
package com.algoarena.service.compiler.submitmode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class JavaSubmitTemplateGenerator {

    private static final Logger logger = LoggerFactory.getLogger(JavaSubmitTemplateGenerator.class);

    /**
     * For submit mode, admin provides complete template with all test cases
     * We just replace USER_CODE_PLACEHOLDER with user's solution
     * The testcases and methodName parameters are kept for consistency but not used
     */
    public String generateSubmitTemplate(
            String userCode,
            java.util.List<com.algoarena.model.Question.Testcase> testcases,
            String methodName) {
        
        logger.info("Generating Java submit code (placeholder replacement mode)...");
        logger.info("User code length: {} chars", userCode.length());

        // For now, just return the user code wrapped in Solution class
        // This will be replaced once we fetch admin template from DB
        String template = "class Solution {\n" + userCode + "\n}";
        
        logger.info("Generated code length: {} chars", template.length());
        
        return template;
    }

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