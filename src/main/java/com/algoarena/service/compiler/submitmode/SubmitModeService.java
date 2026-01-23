// src/main/java/com/algoarena/service/compiler/submitmode/SubmitModeService.java
package com.algoarena.service.compiler.submitmode;

import com.algoarena.dto.compiler.ExecutionRequest;
import com.algoarena.dto.compiler.ExecutionResponse;
import com.algoarena.dto.compiler.submitmode.SubmitCodeRequest;
import com.algoarena.dto.compiler.submitmode.SubmitCodeResponse;
import com.algoarena.dto.compiler.submitmode.SubmitTestCaseResult;
import com.algoarena.dto.dsa.ApproachDetailDTO;
import com.algoarena.model.Question;
import com.algoarena.model.User;
import com.algoarena.model.UserApproaches.ApproachStatus;
import com.algoarena.repository.QuestionRepository;
import com.algoarena.service.compiler.PistonService;
import com.algoarena.service.dsa.ApproachService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SubmitModeService {

    private static final Logger logger = LoggerFactory.getLogger(SubmitModeService.class);

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private JavaSubmitTemplateGenerator javaSubmitTemplateGenerator;

    // ✅ ADD NEW TEMPLATE GENERATORS
    @Autowired
    private CppSubmitTemplateGenerator cppSubmitTemplateGenerator;

    @Autowired
    private PythonSubmitTemplateGenerator pythonSubmitTemplateGenerator;

    @Autowired
    private JavaScriptSubmitTemplateGenerator javaScriptSubmitTemplateGenerator;

    @Autowired
    private PistonService pistonService;

    @Autowired
    private SubmitOutputParser submitOutputParser;

    @Autowired
    private ApproachService approachService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public SubmitCodeResponse executeSubmitMode(String questionId, SubmitCodeRequest request, User user) {
        logger.info("========== SUBMIT MODE EXECUTION START ==========");
        logger.info("Question ID: {}", questionId);
        logger.info("Language: {}", request.getLanguage());
        logger.info("User: {}", user.getUsername());
        
        // 1. Fetch question
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found with id: " + questionId));

        if (question.getMethodName() == null || question.getMethodName().trim().isEmpty()) {
            throw new RuntimeException("Method name not configured for this question");
        }

        if (question.getTestcases() == null || question.getTestcases().isEmpty()) {
            throw new RuntimeException("No test cases found for this question");
        }

        logger.info("Method name: {}", question.getMethodName());
        logger.info("Total testcases: {}", question.getTestcases().size());

        // 2. Generate complete code
        String completeCode = generateCompleteCode(
            request.getLanguage(),
            request.getCode(),
            question.getTestcases(),
            question.getMethodName()
        );

        logger.info("========== GENERATED COMPLETE CODE START ==========");
        logger.info("\n{}", completeCode);
        logger.info("========== GENERATED COMPLETE CODE END ==========");

        // 3. Execute via Piston
        ExecutionResponse executionResponse = executeCode(request.getLanguage(), completeCode);

        logger.info("========== PISTON EXECUTION RESPONSE ==========");
        logger.info("Language: {}", executionResponse.getLanguage());
        logger.info("Version: {}", executionResponse.getVersion());
        
        if (executionResponse.getCompile() != null) {
            logger.info("Compile stdout: {}", executionResponse.getCompile().getStdout());
            logger.info("Compile stderr: {}", executionResponse.getCompile().getStderr());
            logger.info("Compile code: {}", executionResponse.getCompile().getCode());
        }
        
        if (executionResponse.getRun() != null) {
            logger.info("Run stdout length: {}", 
                executionResponse.getRun().getStdout() != null ? executionResponse.getRun().getStdout().length() : 0);
            logger.info("Run stdout: \n{}", executionResponse.getRun().getStdout());
            logger.info("Run stderr: {}", executionResponse.getRun().getStderr());
            logger.info("Run code: {}", executionResponse.getRun().getCode());
            logger.info("Run wall_time: {}", executionResponse.getRun().getWallTime());
            logger.info("Run memory: {}", executionResponse.getRun().getMemory());
        }
        logger.info("========== PISTON EXECUTION RESPONSE END ==========");

        // 4. Parse results
        SubmitCodeResponse response = buildSubmitCodeResponse(
            executionResponse, 
            question.getTestcases().size(),
            question.getTestcases()
        );

        logger.info("========== SUBMIT MODE RESULT ==========");
        logger.info("Success: {}", response.isSuccess());
        logger.info("Verdict: {}", response.getVerdict());
        logger.info("Passed: {}/{}", response.getPassedTestCases(), response.getTotalTestCases());
        logger.info("Runtime: {} ms", response.getMetrics() != null ? response.getMetrics().getRuntime() : 0);
        logger.info("Memory: {} MB", response.getMetrics() != null ? response.getMetrics().getMemory() : 0);

        // 5. Create approach
        try {
            String approachId = createApproach(
                user,
                questionId,
                request.getCode(),
                request.getLanguage(),
                response,
                executionResponse
            );
            response.setApproachId(approachId);
            logger.info("Approach created: {}", approachId);
        } catch (Exception e) {
            logger.error("Failed to create approach: {}", e.getMessage(), e);
        }

        logger.info("========== SUBMIT MODE EXECUTION END ==========");
        return response;
    }

    // ✅ UPDATED: Now supports all 4 languages
    private String generateCompleteCode(
            String language,
            String userCode,
            List<Question.Testcase> testcases,
            String methodName) {

        switch (language.toLowerCase()) {
            case "java":
                return javaSubmitTemplateGenerator.generateSubmitTemplate(
                    userCode, testcases, methodName);
            
            case "cpp":
                return cppSubmitTemplateGenerator.generateSubmitTemplate(
                    userCode, testcases, methodName);
            
            case "python":
                return pythonSubmitTemplateGenerator.generateSubmitTemplate(
                    userCode, testcases, methodName);
            
            case "javascript":
                return javaScriptSubmitTemplateGenerator.generateSubmitTemplate(
                    userCode, testcases, methodName);
            
            default:
                throw new IllegalArgumentException("Unsupported language: " + language);
        }
    }

    private ExecutionResponse executeCode(String language, String code) {
        ExecutionRequest executionRequest = new ExecutionRequest();
        executionRequest.setLanguage(mapLanguageToPiston(language));
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

    private SubmitCodeResponse buildSubmitCodeResponse(
            ExecutionResponse executionResponse, 
            int totalTestCases,
            List<Question.Testcase> testcases) {

        SubmitCodeResponse response = new SubmitCodeResponse();

        // Check compilation errors
        if (executionResponse.getCompile() != null && 
            executionResponse.getCompile().getCode() != 0) {
            
            String compileError = executionResponse.getCompile().getStderr() != null 
                ? executionResponse.getCompile().getStderr() 
                : executionResponse.getCompile().getOutput();
            
            return handleCompileError(compileError, totalTestCases, testcases);
        }

        ExecutionResponse.RunResult runResult = executionResponse.getRun();
        
        // Check stderr for compilation errors
        if (runResult != null && runResult.getStderr() != null && 
            !runResult.getStderr().isEmpty() &&
            submitOutputParser.isCompileError(runResult.getStderr())) {
            
            return handleCompileError(runResult.getStderr(), totalTestCases, testcases);
        }
        
        String stdout = runResult != null ? runResult.getStdout() : "";
        
        // Parse test results
        List<SubmitTestCaseResult> testResults = submitOutputParser.parseOutput(stdout, totalTestCases);

        logger.info("========== PARSED TEST RESULTS ==========");
        for (SubmitTestCaseResult result : testResults) {
            logger.info("TC {}: Status={}, Expected={}, User={}, Time={} ms, Error={}", 
                result.getId(), 
                result.getStatus(), 
                result.getExpectedOutput(), 
                result.getUserOutput(),
                result.getExecutionTime(),
                result.getError());
        }
        logger.info("========== PARSED TEST RESULTS END ==========");

        // Calculate metrics
        int passedCount = (int) testResults.stream().filter(r -> "PASS".equals(r.getStatus())).count();
        int failedCount = (int) testResults.stream().filter(r -> "FAIL".equals(r.getStatus())).count();
        int tleCount = (int) testResults.stream().filter(r -> "TLE".equals(r.getStatus())).count();

        // Determine verdict
        String verdict;
        if (tleCount > 0) {
            verdict = "TLE";
        } else if (failedCount > 0) {
            verdict = "WRONG_ANSWER";
        } else {
            verdict = "ACCEPTED";
        }

        // Build response
        response.setSuccess(verdict.equals("ACCEPTED"));
        response.setVerdict(verdict);
        response.setPassedTestCases(passedCount);
        response.setTotalTestCases(totalTestCases);

        // Set metrics with MAX execution time
        SubmitCodeResponse.ExecutionMetrics metrics = new SubmitCodeResponse.ExecutionMetrics();
        
        Long maxRuntime = testResults.stream()
            .map(SubmitTestCaseResult::getExecutionTime)
            .filter(time -> time != null && time > 0)
            .max(Long::compareTo)
            .orElse(0L);
        
        logger.info("Max runtime calculated: {} ms", maxRuntime);
        
        metrics.setRuntime(maxRuntime);
        
        if (runResult != null && runResult.getMemory() != null) {
            metrics.setMemory(runResult.getMemory() / (1024.0 * 1024.0));
        }
        
        response.setMetrics(metrics);

        // Set first failure
        if (!verdict.equals("ACCEPTED")) {
            SubmitTestCaseResult firstFail = testResults.stream()
                .filter(r -> !"PASS".equals(r.getStatus()))
                .findFirst()
                .orElse(null);
            
            if (firstFail != null) {
                Question.Testcase originalTestcase = testcases.stream()
                    .filter(tc -> tc.getId() == firstFail.getId())
                    .findFirst()
                    .orElse(null);
                
                response.setFirstFailure(buildFirstFailureDetail(firstFail, originalTestcase));
            }
        }

        response.setMessage(generateMessage(verdict, passedCount, totalTestCases, response.getFirstFailure()));

        return response;
    }

    private SubmitCodeResponse handleCompileError(String errorMessage, int totalTestCases, 
                                                    List<Question.Testcase> testcases) {
        SubmitCodeResponse response = new SubmitCodeResponse();
        response.setSuccess(false);
        response.setVerdict("WRONG_ANSWER");
        response.setMessage("Compilation failed");
        response.setPassedTestCases(0);
        response.setTotalTestCases(totalTestCases);
        
        SubmitCodeResponse.FirstFailureDetail failure = new SubmitCodeResponse.FirstFailureDetail();
        failure.setTestCaseId(1);
        
        if (!testcases.isEmpty()) {
            Question.Testcase firstTestcase = testcases.get(0);
            try {
                failure.setInput(objectMapper.writeValueAsString(firstTestcase.getInput()));
                failure.setExpectedOutput(String.valueOf(firstTestcase.getExpectedOutput()));
            } catch (Exception e) {
                failure.setInput("{}");
            }
        }
        
        failure.setUserOutput(null);
        failure.setError(submitOutputParser.extractCompileError(errorMessage));
        
        response.setFirstFailure(failure);
        response.setMetrics(new SubmitCodeResponse.ExecutionMetrics(0L, 0.0));
        
        return response;
    }

    private SubmitCodeResponse.FirstFailureDetail buildFirstFailureDetail(
            SubmitTestCaseResult failResult, 
            Question.Testcase originalTestcase) {
        
        SubmitCodeResponse.FirstFailureDetail detail = new SubmitCodeResponse.FirstFailureDetail();
        detail.setTestCaseId(failResult.getId());
        detail.setExpectedOutput(failResult.getExpectedOutput());
        detail.setUserOutput(failResult.getUserOutput());
        detail.setError(failResult.getError());
        
        if (originalTestcase != null) {
            try {
                detail.setInput(objectMapper.writeValueAsString(originalTestcase.getInput()));
            } catch (Exception e) {
                detail.setInput("{}");
            }
        }
        
        return detail;
    }

    private String generateMessage(String verdict, int passed, int total, 
                                     SubmitCodeResponse.FirstFailureDetail firstFailure) {
        switch (verdict) {
            case "ACCEPTED":
                return String.format("Accepted! All %d/%d test cases passed. Great job!", passed, total);
            
            case "WRONG_ANSWER":
                if (firstFailure != null && firstFailure.getError() != null) {
                    return "Compilation failed";
                }
                return String.format("Wrong answer on test case %d", 
                    firstFailure != null ? firstFailure.getTestCaseId() : 1);
            
            case "TLE":
                return String.format("Time limit exceeded on test case %d", 
                    firstFailure != null ? firstFailure.getTestCaseId() : 1);
            
            default:
                return "Execution completed";
        }
    }

    private String createApproach(
            User user,
            String questionId,
            String code,
            String language,
            SubmitCodeResponse response,
            ExecutionResponse executionResponse) {

        ApproachDetailDTO approachDTO = new ApproachDetailDTO();
        approachDTO.setQuestionId(questionId);
        approachDTO.setCodeContent(code);
        approachDTO.setCodeLanguage(language);
        approachDTO.setTextContent(null);

        switch (response.getVerdict()) {
            case "ACCEPTED":
                approachDTO.setStatus(ApproachStatus.ACCEPTED);
                if (response.getMetrics() != null) {
                    approachDTO.setRuntime(response.getMetrics().getRuntime());
                    approachDTO.setMemory(response.getMetrics().getMemory() != null 
                        ? response.getMetrics().getMemory().longValue() : null);
                }
                break;
            
            case "WRONG_ANSWER":
                approachDTO.setStatus(ApproachStatus.WRONG_ANSWER);
                if (response.getFirstFailure() != null) {
                    ApproachDetailDTO.TestcaseFailureDTO wrongTC = new ApproachDetailDTO.TestcaseFailureDTO();
                    wrongTC.setInput(response.getFirstFailure().getInput());
                    wrongTC.setExpectedOutput(response.getFirstFailure().getExpectedOutput());
                    wrongTC.setUserOutput(response.getFirstFailure().getUserOutput());
                    if (response.getFirstFailure().getError() != null) {
                        wrongTC.setUserOutput(response.getFirstFailure().getError());
                    }
                    approachDTO.setWrongTestcase(wrongTC);
                }
                break;
            
            case "TLE":
                approachDTO.setStatus(ApproachStatus.TIME_LIMIT_EXCEEDED);
                if (response.getFirstFailure() != null) {
                    ApproachDetailDTO.TestcaseFailureDTO tleTC = new ApproachDetailDTO.TestcaseFailureDTO();
                    tleTC.setInput(response.getFirstFailure().getInput());
                    tleTC.setExpectedOutput(response.getFirstFailure().getExpectedOutput());
                    tleTC.setUserOutput(null);
                    approachDTO.setTleTestcase(tleTC);
                }
                break;
        }

        ApproachDetailDTO created = approachService.createApproach(
            user.getId(), 
            questionId, 
            approachDTO, 
            user
        );

        return created.getId();
    }
}