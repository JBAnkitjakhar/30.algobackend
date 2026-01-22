// src/main/java/com/algoarena/dto/compiler/runmode/RunTestCaseInput.java
package com.algoarena.dto.compiler.runmode;

import java.util.Map;

public class RunTestCaseInput {
    
    private Map<String, Object> input; // { "grid": [[1,0,1]], "target": 5 }

    // Constructors
    public RunTestCaseInput() {}

    public RunTestCaseInput(Map<String, Object> input) {
        this.input = input;
    }

    // Getters and Setters
    public Map<String, Object> getInput() {
        return input;
    }

    public void setInput(Map<String, Object> input) {
        this.input = input;
    }
}