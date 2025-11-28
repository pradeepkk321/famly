package io.famly.mapper.core.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.famly.mapper.core.model.*;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Security validator for mapping expressions.
 * Validates all expressions at startup to prevent malicious code execution.
 */
public class MappingSecurityValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(MappingSecurityValidator.class);
    
    // Forbidden patterns that indicate security risks
    private static final List<ForbiddenPattern> FORBIDDEN_PATTERNS = Arrays.asList(
        // System and Runtime
        new ForbiddenPattern("System\\.", "HIGH", "Access to System class"),
        new ForbiddenPattern("Runtime\\.getRuntime", "CRITICAL", "Runtime execution"),
        new ForbiddenPattern("ProcessBuilder", "CRITICAL", "Process creation"),
        new ForbiddenPattern("\\.exec\\(", "CRITICAL", "Process execution"),
        new ForbiddenPattern("\\.exit\\(", "CRITICAL", "System exit"),
        
        // Class loading and reflection
        new ForbiddenPattern("Class\\.forName", "HIGH", "Dynamic class loading"),
        new ForbiddenPattern("ClassLoader", "HIGH", "Class loader access"),
        new ForbiddenPattern("\\.newInstance\\(", "HIGH", "Reflection instantiation"),
        new ForbiddenPattern("Method\\.invoke", "HIGH", "Reflection method invocation"),
        new ForbiddenPattern("Constructor\\.newInstance", "HIGH", "Reflection constructor"),
        
        // File I/O
        new ForbiddenPattern("java\\.io\\.File", "MEDIUM", "File system access"),
        new ForbiddenPattern("java\\.nio\\.file", "MEDIUM", "NIO file access"),
        new ForbiddenPattern("FileInputStream", "MEDIUM", "File reading"),
        new ForbiddenPattern("FileOutputStream", "MEDIUM", "File writing"),
        new ForbiddenPattern("FileReader", "MEDIUM", "File reading"),
        new ForbiddenPattern("FileWriter", "MEDIUM", "File writing"),
        new ForbiddenPattern("RandomAccessFile", "MEDIUM", "Random file access"),
        
        // Network I/O
        new ForbiddenPattern("java\\.net\\.Socket", "HIGH", "Network socket access"),
        new ForbiddenPattern("java\\.net\\.URL", "MEDIUM", "URL connection"),
        new ForbiddenPattern("URLConnection", "MEDIUM", "URL connection"),
        new ForbiddenPattern("HttpURLConnection", "MEDIUM", "HTTP connection"),
        new ForbiddenPattern("ServerSocket", "HIGH", "Server socket"),
        
        // Database access
        new ForbiddenPattern("java\\.sql\\.", "HIGH", "SQL database access"),
        new ForbiddenPattern("javax\\.sql\\.", "HIGH", "SQL database access"),
        new ForbiddenPattern("DriverManager", "HIGH", "JDBC driver access"),
        
        // JNDI (injection risks)
        new ForbiddenPattern("javax\\.naming\\.", "HIGH", "JNDI access"),
        new ForbiddenPattern("InitialContext", "HIGH", "JNDI context"),
        
        // Threading (potential DoS)
        new ForbiddenPattern("Thread\\.sleep", "LOW", "Thread sleep"),
        new ForbiddenPattern("new Thread\\(", "MEDIUM", "Thread creation"),
        
        // Scripting engines (code injection)
        new ForbiddenPattern("ScriptEngine", "CRITICAL", "Script engine access"),
        new ForbiddenPattern("javax\\.script", "CRITICAL", "Scripting API")
    );
    
    /**
     * Validate entire mapping registry for security issues.
     */
    public SecurityValidationResult validateRegistry(MappingRegistry registry) {
        SecurityValidationResult result = new SecurityValidationResult();
        
        logger.info("Starting security validation of mapping registry");
        
        for (ResourceMapping mapping : registry.getResourceMappings()) {
            validateResourceMapping(mapping, result);
        }
        
        logger.info("Security validation complete: {} issues found", result.getIssueCount());
        
        return result;
    }
    
    /**
     * Validate a single resource mapping.
     */
    public void validateResourceMapping(ResourceMapping mapping, SecurityValidationResult result) {
        String mappingContext = "Mapping: " + mapping.getId();
        
        for (FieldMapping field : mapping.getFieldMappings()) {
            String fieldContext = mappingContext + ", Field: " + field.getId();
            
            // Validate condition expression
            if (field.getCondition() != null) {
                validateExpression(field.getCondition(), fieldContext + " (condition)", result);
            }
            
            // Validate transform expression
            if (field.getTransformExpression() != null) {
                validateExpression(field.getTransformExpression(), fieldContext + " (transform)", result);
            }
        }
    }
    
    /**
     * Validate a single expression for security issues.
     */
    private void validateExpression(String expression, String context, SecurityValidationResult result) {
        if (expression == null || expression.trim().isEmpty()) {
            return;
        }
        
        for (ForbiddenPattern pattern : FORBIDDEN_PATTERNS) {
            if (pattern.matches(expression)) {
                result.addIssue(new SecurityIssue(
                    context,
                    expression,
                    pattern.getSeverity(),
                    pattern.getDescription(),
                    pattern.getPattern()
                ));
                
                logger.warn("Security issue found in {}: {} - {}", 
                    context, pattern.getDescription(), expression);
            }
        }
    }
    
    /**
     * Represents a forbidden pattern to check for.
     */
    static class ForbiddenPattern {
        private final Pattern pattern;
        private final String severity;
        private final String description;
        private final String patternString;
        
        public ForbiddenPattern(String regex, String severity, String description) {
            this.pattern = Pattern.compile(regex);
            this.severity = severity;
            this.description = description;
            this.patternString = regex;
        }
        
        public boolean matches(String expression) {
            return pattern.matcher(expression).find();
        }
        
        public String getSeverity() { return severity; }
        public String getDescription() { return description; }
        public String getPattern() { return patternString; }
    }
}