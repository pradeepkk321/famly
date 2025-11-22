package com.fhir.mapper.model;

/**
 * Individual code mapping entry
 */
public class CodeMapping {
    private String sourceCode;
    private String targetCode;
    private String display;

    public CodeMapping() {}

    public CodeMapping(String sourceCode, String targetCode, String display) {
        this.sourceCode = sourceCode;
        this.targetCode = targetCode;
        this.display = display;
    }

    public String getSourceCode() { return sourceCode; }
    public void setSourceCode(String sourceCode) { this.sourceCode = sourceCode; }

    public String getTargetCode() { return targetCode; }
    public void setTargetCode(String targetCode) { this.targetCode = targetCode; }

    public String getDisplay() { return display; }
    public void setDisplay(String display) { this.display = display; }
    
    @Override
    public String toString() {
        return "CodeMapping{" +
            "sourceCode='" + sourceCode + '\'' +
            ", targetCode='" + targetCode + '\'' +
            ", display='" + display + '\'' +
            '}';
    }
}
