package io.famly.mapper.core.security;

/**
 * Represents a security issue found in an expression.
 */
public class SecurityIssue {
    private final String context;
    private final String expression;
    private final String severity;
    private final String description;
    private final String pattern;
    
    public SecurityIssue(String context, String expression, String severity, 
                        String description, String pattern) {
        this.context = context;
        this.expression = expression;
        this.severity = severity;
        this.description = description;
        this.pattern = pattern;
    }
    
    public String getContext() { return context; }
    public String getExpression() { return expression; }
    public String getSeverity() { return severity; }
    public String getDescription() { return description; }
    public String getPattern() { return pattern; }
    
    @Override
    public String toString() {
        return severity + ": " + context + " - " + description;
    }
}
