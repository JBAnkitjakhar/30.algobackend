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
    private CppTemplateGenerator cppTemplateGenerator;

    @Autowired
    private PythonTemplateGenerator pythonTemplateGenerator;

    @Autowired
    private JavaScriptTemplateGenerator javaScriptTemplateGenerator;

    @Autowired
    private PistonService pistonService;

    @Autowired
    private RunOutputParser runOutputParser;

    /**
     * Execute user code in run mode
     */
    public RunCodeResponse executeRunMode(String questionId, RunCodeRequest request) {
        // logger.info("========== RUN MODE EXECUTION START ==========");
        // logger.info("Question ID: {}", questionId);
        // logger.info("Language: {}", request.getLanguage());
        // logger.info("Test cases count: {}", request.getTestCases().size());
        
        // 1. Fetch question from DB
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found with id: " + questionId));

        // Validate methodName exists
        if (question.getMethodName() == null || question.getMethodName().trim().isEmpty()) {
            throw new RuntimeException("Method name not configured for this question. Please contact admin.");
        }

        // 2. Get correct solution for the language
        Map<String, String> correctSolutions = question.getCorrectSolution();
        if (correctSolutions == null || !correctSolutions.containsKey(request.getLanguage())) {
            throw new RuntimeException("Correct solution not available for language: " + request.getLanguage());
        }

        String correctSolution = correctSolutions.get(request.getLanguage());
        // logger.info("Correct solution retrieved (length: {} chars)", correctSolution.length());
        // logger.info("Method name: {}", question.getMethodName());

        // 3. Generate complete code based on language
        String completeCode = generateCompleteCode(
            request.getLanguage(),
            correctSolution,
            request.getCode(),
            request.getTestCases(),
            question.getMethodName()
        );

        // logger.info("========== GENERATED COMPLETE CODE START ==========");
        // logger.info("Language: {}", request.getLanguage());
        // logger.info("Code length: {} characters", completeCode.length());
        // logger.info("\n{}", completeCode);
        // logger.info("========== GENERATED COMPLETE CODE END ==========");

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
     * Generate complete code based on language
     */
    private String generateCompleteCode(
            String language,
            String correctSolution,
            String userCode,
            List<com.algoarena.dto.compiler.runmode.RunTestCaseInput> testCases,
            String methodName) {

        switch (language.toLowerCase()) {
            case "java":
                return javaTemplateGenerator.generateRunTemplate(
                    correctSolution, userCode, testCases, methodName);
            
            case "cpp":
                return cppTemplateGenerator.generateRunTemplate(
                    correctSolution, userCode, testCases, methodName);
            
            case "python":
                return pythonTemplateGenerator.generateRunTemplate(
                    correctSolution, userCode, testCases, methodName);
            
            case "javascript":
                return javaScriptTemplateGenerator.generateRunTemplate(
                    correctSolution, userCode, testCases, methodName);
            
            default:
                throw new IllegalArgumentException("Unsupported language: " + language);
        }
    }

    /**
     * Execute code using Piston API
     */
    private ExecutionResponse executeCode(String language, String code) {
        ExecutionRequest executionRequest = new ExecutionRequest();
        
        // Map our language names to Piston language names
        String pistonLanguage = mapLanguageToPiston(language);
        executionRequest.setLanguage(pistonLanguage);
        executionRequest.setVersion("*"); // Use latest version
        executionRequest.setCode(code);

        // Execute with Piston
        return pistonService.executeCode(executionRequest);
    }

    /**
     * Map our language names to Piston API language names
     */
    private String mapLanguageToPiston(String language) {
        switch (language.toLowerCase()) {
            case "java":
                return "java";
            case "cpp":
                return "cpp";
            case "python":
                return "python";
            case "javascript":
                return "javascript";
            default:
                return language;
        }
    }

    /**
     * Build RunCodeResponse from Piston execution result
     */
    private RunCodeResponse buildRunCodeResponse(ExecutionResponse executionResponse, int totalTestCases) {
        RunCodeResponse response = new RunCodeResponse();

        // Check for compilation errors (method 1: compile object with non-zero exit code)
        if (executionResponse.getCompile() != null && 
            executionResponse.getCompile().getCode() != 0) {
            
            String compileError = executionResponse.getCompile().getStderr() != null 
                ? executionResponse.getCompile().getStderr() 
                : executionResponse.getCompile().getOutput();
            
            return handleCompileError(compileError, totalTestCases);
        }

        // Parse execution output
        ExecutionResponse.RunResult runResult = executionResponse.getRun();
        
        // Check for compilation errors (method 2: stderr in run result)
        if (runResult != null && runResult.getStderr() != null && 
            !runResult.getStderr().isEmpty() &&
            runOutputParser.isCompileError(runResult.getStderr())) {
            
            return handleCompileError(runResult.getStderr(), totalTestCases);
        }
        
        String stdout = runResult != null ? runResult.getStdout() : "";
        
        // Parse test case results from stdout
        List<RunTestCaseResult> testCaseResults = runOutputParser.parseOutput(stdout, totalTestCases);

        // Calculate metrics and determine verdict
        RunCodeResponse.ExecutionMetrics metrics = calculateMetrics(testCaseResults, runResult, totalTestCases);
        String verdict = determineVerdict(testCaseResults);

        // Build response
        response.setSuccess(verdict.equals("ACCEPTED"));
        response.setVerdict(verdict);
        response.setMessage(generateMessage(verdict, testCaseResults));
        response.setTestCaseResults(testCaseResults);
        response.setMetrics(metrics);

        return response;
    }

    /**
     * Handle compilation error response
     */
    private RunCodeResponse handleCompileError(String errorMessage, int totalTestCases) {
        RunCodeResponse response = new RunCodeResponse();
        response.setSuccess(false);
        response.setVerdict("WRONG_ANSWER");
        response.setMessage("Compilation failed");
        
        // Create single FAIL result for first test case with compile error
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

    /**
     * Calculate execution metrics
     */
    private RunCodeResponse.ExecutionMetrics calculateMetrics(
            List<RunTestCaseResult> testCaseResults,
            ExecutionResponse.RunResult runResult,
            int totalTestCases) {

        RunCodeResponse.ExecutionMetrics metrics = new RunCodeResponse.ExecutionMetrics();

        // Count test case statuses
        int passed = 0;
        int failed = 0;
        int tle = 0;

        for (RunTestCaseResult result : testCaseResults) {
            switch (result.getStatus()) {
                case "PASS":
                    passed++;
                    break;
                case "FAIL":
                    failed++;
                    break;
                case "TLE":
                    tle++;
                    break;
            }
        }

        metrics.setTotalTestCases(totalTestCases);
        metrics.setPassedTestCases(passed);
        metrics.setFailedTestCases(failed);
        metrics.setTleTestCases(tle);

        // Extract memory from Piston
        if (runResult != null && runResult.getMemory() != null) {
            metrics.setMemoryUsedMb(runResult.getMemory() / (1024.0 * 1024.0));
        }

        return metrics;
    }

    /**
     * Determine overall verdict based on test case results
     */
    private String determineVerdict(List<RunTestCaseResult> testCaseResults) {
        boolean hasFailure = false;
        boolean hasTLE = false;

        for (RunTestCaseResult result : testCaseResults) {
            if (result.getStatus().equals("FAIL")) {
                hasFailure = true;
            } else if (result.getStatus().equals("TLE")) {
                hasTLE = true;
            }
        }

        // Priority: TLE > FAIL > ACCEPTED
        if (hasTLE) {
            return "TLE";
        } else if (hasFailure) {
            return "WRONG_ANSWER";
        } else {
            return "ACCEPTED";
        }
    }

    /**
     * Generate user-friendly message
     */
    private String generateMessage(String verdict, List<RunTestCaseResult> testCaseResults) {
        switch (verdict) {
            case "ACCEPTED":
                return "All test cases passed!";
            
            case "WRONG_ANSWER":
                // Find first failed test case
                for (RunTestCaseResult result : testCaseResults) {
                    if (result.getStatus().equals("FAIL")) {
                        return "Wrong answer on test case " + result.getId();
                    }
                }
                return "Wrong answer";
            
            case "TLE":
                // Find first TLE test case
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

    /**
     * Create empty metrics for compile error cases
     */
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