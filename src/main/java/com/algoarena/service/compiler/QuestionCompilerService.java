// src/main/java/com/algoarena/service/compiler/QuestionCompilerService.java
package com.algoarena.service.compiler;

import com.algoarena.dto.compiler.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QuestionCompilerService {

    @Autowired
    private PistonService pistonService;

    @Autowired
    private OutputParserService outputParserService;

    @Autowired
    private QueueService queueService;

    public QuestionExecutionResponse executeQuestion(QuestionExecutionRequest request, String userId) {
        try {
            queueService.tryAcquire(userId);

            try {
                return executeCodeInternal(request);
            } finally {
                queueService.release(userId);
            }

        } catch (IllegalStateException e) {
            QuestionExecutionResponse response = new QuestionExecutionResponse();
            response.setSuccess(false);
            response.setMessage(e.getMessage());
            return response;
            
        } catch (InterruptedException e) {
            QuestionExecutionResponse response = new QuestionExecutionResponse();
            response.setSuccess(false);
            response.setMessage("Request interrupted");
            return response;
        }
    }

    private QuestionExecutionResponse executeCodeInternal(QuestionExecutionRequest request) {
        QuestionExecutionResponse response = new QuestionExecutionResponse();

        try {
            String version = request.getVersion();
            if (version == null || version.isEmpty()) {
                version = pistonService.getLanguageVersion(request.getLanguage());
            }

            // Just execute the code as-is - frontend sends complete code
            ExecutionRequest executionRequest = new ExecutionRequest(
                request.getLanguage(),
                version,
                request.getCode()
            );

            ExecutionResponse pistonResponse = pistonService.executeCode(executionRequest);

            if (pistonResponse.hasCompileError()) {
                response.setSuccess(false);
                response.setMessage("Compilation failed");
                response.setError(outputParserService.extractCompileError(
                    pistonResponse.getCompile().getStderr()
                ));
                return response;
            }

            if (pistonResponse.hasRuntimeError() && 
                (pistonResponse.getRun().getStdout() == null || 
                 pistonResponse.getRun().getStdout().isEmpty())) {
                response.setSuccess(false);
                response.setMessage("Runtime error");
                response.setError(pistonResponse.getRun().getStderr());
                return response;
            }

            String stdout = pistonResponse.getRun().getStdout();
            List<TestCaseResult> testCaseResults = outputParserService.parseTestCaseResults(stdout);

            QuestionExecutionResponse.ExecutionMetrics metrics = new QuestionExecutionResponse.ExecutionMetrics();
            
            // Set test case counts
            metrics.setTotalTestCases(testCaseResults.size());
            metrics.setExecutedTestCases(testCaseResults.size());
            
            // Calculate MAX TIME among all test cases (like LeetCode)
            long maxTime = testCaseResults.stream()
                .filter(tc -> tc.getTimeMs() != null)
                .mapToLong(TestCaseResult::getTimeMs)
                .max()
                .orElse(0L);
            metrics.setMaxTimeMs(maxTime);
            
            // Get total memory from Piston (in bytes, convert to MB)
            if (pistonResponse.getRun().getMemory() != null) {
                long memoryBytes = pistonResponse.getRun().getMemory();
                double memoryMb = memoryBytes / (1024.0 * 1024.0);
                // Round to 2 decimal places
                memoryMb = Math.round(memoryMb * 100.0) / 100.0;
                metrics.setTotalMemoryMb(memoryMb);
            }

            response.setSuccess(true);
            response.setMessage("Execution completed");
            response.setTestCaseResults(testCaseResults);
            response.setMetrics(metrics);

            return response;

        } catch (Exception e) {
            response.setSuccess(false);
            response.setMessage("Execution failed");
            response.setError(e.getMessage());
            return response;
        }
    }

    public QueueService.QueueStatus getQueueStatus() {
        return queueService.getQueueStatus();
    }
}