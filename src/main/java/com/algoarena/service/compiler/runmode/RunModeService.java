// src/main/java/com/algoarena/service/compiler/runmode/RunModeService.java
package com.algoarena.service.compiler.runmode;

import com.algoarena.dto.compiler.ExecutionRequest;
import com.algoarena.dto.compiler.ExecutionResponse;
import com.algoarena.dto.compiler.runmode.RunCodeRequest;
import com.algoarena.dto.compiler.runmode.RunCodeResponse;
import com.algoarena.dto.compiler.runmode.RunTestCaseResult;
import com.algoarena.model.Question;
import com.algoarena.repository.QuestionRepository;
import com.algoarena.service.compiler.PistonService;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class RunModeService {

    // private static final Logger logger = LoggerFactory.getLogger(RunModeService.class);

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private JavaTemplateGenerator javaTemplateGenerator;

    @Autowired
    private PythonTemplateGenerator pythonTemplateGenerator;

    @Autowired
    private CppTemplateGenerator cppTemplateGenerator;

    @Autowired
    private JavaScriptTemplateGenerator javaScriptTemplateGenerator;

    @Autowired
    private PistonService pistonService;

    @Autowired
    private RunOutputParser runOutputParser;

    /**
     * Execute user code in run mode using template-based approach
     */
    public RunCodeResponse executeRunMode(String questionId, RunCodeRequest request) {

        // logger.info("========== RUN MODE EXECUTION START ==========");
        // logger.info("Question ID: {}", questionId);
        // logger.info("Language: {}", request.getLanguage());
        // logger.info("Test cases count: {}", request.getTestCases().size());

        // 1. Fetch question from DB
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found with id: " + questionId));

        // 2. Get run template for the language
        Map<String, String> runTemplates = question.getRunTemplate();
        if (runTemplates == null || !runTemplates.containsKey(request.getLanguage())) {
            throw new RuntimeException("Template not available for language: " + request.getLanguage());
        }

        String adminTemplate = runTemplates.get(request.getLanguage());

        // logger.info("Admin template length: {} characters", adminTemplate.length());

        // 3. Generate executable code from template using language-specific generator
        String completeCode = generateCompleteCode(
                request.getLanguage(),
                adminTemplate,
                request.getCode(),
                request.getTestCases()
        );

        // logger.info("========== GENERATED COMPLETE CODE START ==========");
        // logger.info("\n{}", completeCode);
        // logger.info("========== GENERATED COMPLETE CODE END ==========");
        // logger.info("Generated code length: {} characters", completeCode.length());

        // 4. Execute code using Piston
        ExecutionResponse executionResponse = executeCode(request.getLanguage(), completeCode);

        // logger.info("========== PISTON EXECUTION RESPONSE ==========");
        // logger.info("Language: {}", executionResponse.getLanguage());
        // logger.info("Version: {}", executionResponse.getVersion());

        // if (executionResponse.getCompile() != null) {
        //     logger.info("Compile stdout: {}", executionResponse.getCompile().getStdout());
        //     logger.info("Compile stderr: {}", executionResponse.getCompile().getStderr());
        //     logger.info("Compile code: {}", executionResponse.getCompile().getCode());
        // }

        // if (executionResponse.getRun() != null) {
        //     logger.info("Run stdout: {}", executionResponse.getRun().getStdout());
        //     logger.info("Run stderr: {}", executionResponse.getRun().getStderr());
        //     logger.info("Run code: {}", executionResponse.getRun().getCode());
        // }
        // logger.info("========== PISTON EXECUTION RESPONSE END ==========");

        // 5. Parse output and build response
        RunCodeResponse response = buildRunCodeResponse(executionResponse, request.getTestCases().size());

        // logger.info("========== RUN MODE EXECUTION END ==========");
        // logger.info("Success: {}", response.isSuccess());
        // logger.info("Verdict: {}", response.getVerdict());

        return response;
    }

    /**
     * Generate complete code using language-specific generator
     */
    private String generateCompleteCode(
            String language,
            String adminTemplate,
            String userCode,
            List<com.algoarena.dto.compiler.runmode.RunTestCaseInput> testCases) {

        switch (language.toLowerCase()) {
            case "java":
                return javaTemplateGenerator.generateFromTemplate(adminTemplate, userCode, testCases);
            
            case "python":
                return pythonTemplateGenerator.generateFromTemplate(adminTemplate, userCode, testCases);
            
            case "cpp":
            case "c++":
                return cppTemplateGenerator.generateFromTemplate(adminTemplate, userCode, testCases);
            
            case "javascript":
                return javaScriptTemplateGenerator.generateFromTemplate(adminTemplate, userCode, testCases);
            
            default:
                throw new IllegalArgumentException("Unsupported language: " + language);
        }
    }

    /**
     * Execute code using Piston API
     */
    private ExecutionResponse executeCode(String language, String code) {
        ExecutionRequest executionRequest = new ExecutionRequest();

        String pistonLanguage = mapLanguageToPiston(language);
        executionRequest.setLanguage(pistonLanguage);
        executionRequest.setVersion("*");
        executionRequest.setCode(code);

        return pistonService.executeCode(executionRequest);
    }

    private String mapLanguageToPiston(String language) {
        switch (language.toLowerCase()) {
            case "java": return "java";
            case "cpp": return "cpp";
            case "python": return "python";
            case "javascript": return "javascript";
            default: return language;
        }
    }

    /**
     * Build RunCodeResponse from Piston execution result
     */
    private RunCodeResponse buildRunCodeResponse(ExecutionResponse executionResponse, int totalTestCases) {
        RunCodeResponse response = new RunCodeResponse();

        // Check for compilation errors
        if (executionResponse.getCompile() != null &&
                executionResponse.getCompile().getCode() != 0) {

            String compileError = executionResponse.getCompile().getStderr() != null
                    ? executionResponse.getCompile().getStderr()
                    : executionResponse.getCompile().getOutput();

            return handleCompileError(compileError, totalTestCases);
        }

        ExecutionResponse.RunResult runResult = executionResponse.getRun();

        if (runResult != null && runResult.getStderr() != null &&
                !runResult.getStderr().isEmpty() &&
                runOutputParser.isCompileError(runResult.getStderr())) {

            return handleCompileError(runResult.getStderr(), totalTestCases);
        }

        String stdout = runResult != null ? runResult.getStdout() : "";

        // logger.info("Parsing stdout: {}", stdout);

        List<RunTestCaseResult> testCaseResults = runOutputParser.parseOutput(stdout, totalTestCases);

        // logger.info("Parsed {} test case results", testCaseResults.size());
        // for (RunTestCaseResult result : testCaseResults) {
        //     logger.info("Test Case {}: status={}, expected={}, actual={}",
        //             result.getId(), result.getStatus(), result.getExpectedOutput(), result.getUserOutput());
        // }

        RunCodeResponse.ExecutionMetrics metrics = calculateMetrics(testCaseResults, runResult, totalTestCases);
        String verdict = determineVerdict(testCaseResults);

        response.setSuccess(verdict.equals("ACCEPTED"));
        response.setVerdict(verdict);
        response.setMessage(generateMessage(verdict, testCaseResults));
        response.setTestCaseResults(testCaseResults);
        response.setMetrics(metrics);

        return response;
    }

    private RunCodeResponse handleCompileError(String errorMessage, int totalTestCases) {
        RunCodeResponse response = new RunCodeResponse();
        response.setSuccess(false);
        response.setVerdict("WRONG_ANSWER");
        response.setMessage("Compilation failed");

        List<RunTestCaseResult> results = new ArrayList<>();
        RunTestCaseResult failResult = new RunTestCaseResult();
        failResult.setId(1);
        failResult.setStatus("FAIL");
        failResult.setError(runOutputParser.extractCompileError(errorMessage));
        results.add(failResult);

        response.setTestCaseResults(results);
        response.setMetrics(createEmptyMetrics(totalTestCases));

        return response;
    }

    private RunCodeResponse.ExecutionMetrics calculateMetrics(
            List<RunTestCaseResult> testCaseResults,
            ExecutionResponse.RunResult runResult,
            int totalTestCases) {

        RunCodeResponse.ExecutionMetrics metrics = new RunCodeResponse.ExecutionMetrics();

        int passed = 0, failed = 0, tle = 0;

        for (RunTestCaseResult result : testCaseResults) {
            switch (result.getStatus()) {
                case "PASS": passed++; break;
                case "FAIL": failed++; break;
                case "TLE": tle++; break;
            }
        }

        metrics.setTotalTestCases(totalTestCases);
        metrics.setPassedTestCases(passed);
        metrics.setFailedTestCases(failed);
        metrics.setTleTestCases(tle);

        if (runResult != null && runResult.getMemory() != null) {
            metrics.setMemoryUsedMb(runResult.getMemory() / (1024.0 * 1024.0));
        }

        return metrics;
    }

    private String determineVerdict(List<RunTestCaseResult> testCaseResults) {
        boolean hasFailure = false;
        boolean hasTLE = false;

        for (RunTestCaseResult result : testCaseResults) {
            if (result.getStatus().equals("FAIL")) hasFailure = true;
            else if (result.getStatus().equals("TLE")) hasTLE = true;
        }

        if (hasTLE) return "TLE";
        else if (hasFailure) return "WRONG_ANSWER";
        else return "ACCEPTED";
    }

    private String generateMessage(String verdict, List<RunTestCaseResult> testCaseResults) {
        switch (verdict) {
            case "ACCEPTED":
                return "All test cases passed!";
            case "WRONG_ANSWER":
                for (RunTestCaseResult result : testCaseResults) {
                    if (result.getStatus().equals("FAIL")) {
                        return "Wrong answer on test case " + result.getId();
                    }
                }
                return "Wrong answer";
            case "TLE":
                for (RunTestCaseResult result : testCaseResults) {
                    if (result.getStatus().equals("TLE")) {
                        return "Time limit exceeded on test case " + result.getId();
                    }
                }
                return "Time limit exceeded";
            default:
                return "Execution completed";
        }
    }

    private RunCodeResponse.ExecutionMetrics createEmptyMetrics(int totalTestCases) {
        RunCodeResponse.ExecutionMetrics metrics = new RunCodeResponse.ExecutionMetrics();
        metrics.setTotalTestCases(totalTestCases);
        metrics.setPassedTestCases(0);
        metrics.setFailedTestCases(1);
        metrics.setTleTestCases(0);
        metrics.setMemoryUsedMb(0.0);
        return metrics;
    }
}