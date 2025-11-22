package com.fhir.mapper.security;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Result of security validation.
 */
public class SecurityValidationResult {
    private final List<SecurityIssue> issues = new ArrayList<>();
    
    public void addIssue(SecurityIssue issue) {
        issues.add(issue);
    }
    
    public List<SecurityIssue> getIssues() {
        return issues;
    }
    
    public boolean hasIssues() {
        return !issues.isEmpty();
    }
    
    public int getIssueCount() {
        return issues.size();
    }
    
    public boolean hasCriticalIssues() {
        return issues.stream().anyMatch(i -> "CRITICAL".equals(i.getSeverity()));
    }
    
    public boolean hasHighSeverityIssues() {
        return issues.stream().anyMatch(i -> "HIGH".equals(i.getSeverity()) || "CRITICAL".equals(i.getSeverity()));
    }
    
    public List<SecurityIssue> getCriticalIssues() {
        return issues.stream()
            .filter(i -> "CRITICAL".equals(i.getSeverity()))
            .collect(java.util.stream.Collectors.toList());
    }
    
    public void throwIfCritical() throws SecurityException {
        if (hasCriticalIssues()) {
            StringBuilder sb = new StringBuilder("CRITICAL security issues found:\n");
            for (SecurityIssue issue : getCriticalIssues()) {
                sb.append("  - ").append(issue.getContext()).append(": ")
                  .append(issue.getDescription()).append("\n");
            }
            throw new SecurityException(sb.toString());
        }
    }
    
    public void printReport() {
        if (!hasIssues()) {
            System.out.println("✓ No security issues found");
            return;
        }
        
        System.out.println("\n=== Security Validation Report ===");
        
        Map<String, List<SecurityIssue>> bySeverity = new HashMap<>();
        for (SecurityIssue issue : issues) {
            bySeverity.computeIfAbsent(issue.getSeverity(), k -> new ArrayList<>()).add(issue);
        }
        
        for (String severity : Arrays.asList("CRITICAL", "HIGH", "MEDIUM", "LOW")) {
            List<SecurityIssue> severityIssues = bySeverity.get(severity);
            if (severityIssues != null && !severityIssues.isEmpty()) {
                System.out.println("\n" + severity + " (" + severityIssues.size() + "):");
                for (SecurityIssue issue : severityIssues) {
                    System.out.println("  - " + issue.getContext());
                    System.out.println("    " + issue.getDescription());
                    System.out.println("    Expression: " + issue.getExpression());
                }
            }
        }
        
        System.out.println("\nTotal issues: " + getIssueCount());
    }
}