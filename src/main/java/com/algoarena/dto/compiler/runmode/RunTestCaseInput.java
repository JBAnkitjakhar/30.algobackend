// src/main/java/com/algoarena/dto/compiler/runmode/RunTestCaseInput.java
package com.algoarena.dto.compiler.runmode;

import java.util.List;

public class RunTestCaseInput {

    // Changed from Map to List - frontend sends ordered array
    private List<Object> input; // [[1,2,3,4,5]] for binary tree

    // Constructors
    public RunTestCaseInput() {}

    public RunTestCaseInput(List<Object> input) {
        this.input = input;
    }

    // Getters and Setters
    public List<Object> getInput() {
        return input;
    }

    public void setInput(List<Object> input) {
        this.input = input;
    }
}