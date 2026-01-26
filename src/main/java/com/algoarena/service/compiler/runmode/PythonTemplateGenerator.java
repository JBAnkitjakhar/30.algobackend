// src/main/java/com/algoarena/service/compiler/runmode/PythonTemplateGenerator.java
package com.algoarena.service.compiler.runmode;

import com.algoarena.dto.compiler.runmode.RunTestCaseInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PythonTemplateGenerator {

    private static final Logger logger = LoggerFactory.getLogger(PythonTemplateGenerator.class);

    public String generateFromTemplate(
            String adminTemplate,
            String userCode,
            List<RunTestCaseInput> testCases) {

        logger.info("Generating Python code from template...");
        logger.info("Number of test cases: {}", testCases.size());

        String testCaseTemplate = extractBetween(
                adminTemplate,
                "#TEST_CASE_TEMPLATE_START",
                "#TEST_CASE_TEMPLATE_END"
        );

        logger.info("Extracted test case template (length: {} chars)", testCaseTemplate.length());

        StringBuilder allTestCases = new StringBuilder();

        for (int i = 0; i < testCases.size(); i++) {
            logger.info("Processing test case {}: input = {}", i + 1, testCases.get(i).getInput());

            String filledBlock = testCaseTemplate;
            List<Object> inputs = testCases.get(i).getInput();

            for (int j = 0; j < inputs.size(); j++) {
                String placeholder = "{{INPUT_" + j + "}}";
                String pythonLiteral = convertToPythonLiteral(inputs.get(j));

                logger.info("Replacing {} with: {}", placeholder, pythonLiteral);

                filledBlock = filledBlock.replace(placeholder, pythonLiteral);
            }

            allTestCases.append(filledBlock).append("\n");
        }

        String finalCode = adminTemplate
                .replace(
                        "#TEST_CASE_TEMPLATE_START" + testCaseTemplate + "#TEST_CASE_TEMPLATE_END",
                        allTestCases.toString()
                )
                .replace("#USER_CODE_PLACEHOLDER", userCode);

        logger.info("Final Python code generated (length: {} chars)", finalCode.length());

        return finalCode;
    }

    private String convertToPythonLiteral(Object value) {
        if (value == null) {
            return "None";
        }

        if (value instanceof Number) {
            return String.valueOf(value);
        }

        if (value instanceof Boolean) {
            return (Boolean) value ? "True" : "False";
        }

        if (value instanceof String) {
            String str = (String) value;
            return "\"" + escapeString(str) + "\"";
        }

        if (value instanceof List) {
            List<?> list = (List<?>) value;

            if (list.isEmpty()) {
                return "[]";
            }

            int depth = getListDepth(list);

            if (depth == 1) {
                return "[" + convertListToString(list) + "]";
            } else if (depth == 2) {
                return "[" + list.stream()
                        .map(inner -> "[" + convertListToString((List<?>) inner) + "]")
                        .collect(Collectors.joining(", ")) + "]";
            } else {
                return "[" + list.stream()
                        .map(this::convertToPythonLiteral)
                        .collect(Collectors.joining(", ")) + "]";
            }
        }

        return String.valueOf(value);
    }

    private int getListDepth(List<?> list) {
        if (list.isEmpty()) return 1;
        Object first = list.get(0);
        if (first instanceof List) {
            return 1 + getListDepth((List<?>) first);
        }
        return 1;
    }

    private String convertListToString(List<?> list) {
        return list.stream()
                .map(item -> {
                    if (item == null) return "None";
                    if (item instanceof Boolean) return (Boolean) item ? "True" : "False";
                    if (item instanceof String) {
                        return "\"" + escapeString((String) item) + "\"";
                    }
                    return String.valueOf(item);
                })
                .collect(Collectors.joining(", "));
    }

    private String escapeString(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String extractBetween(String text, String start, String end) {
        int startIdx = text.indexOf(start);
        int endIdx = text.indexOf(end);

        if (startIdx == -1 || endIdx == -1) {
            throw new RuntimeException("Invalid template: missing markers " + start + " or " + end);
        }

        return text.substring(startIdx + start.length(), endIdx);
    }
}