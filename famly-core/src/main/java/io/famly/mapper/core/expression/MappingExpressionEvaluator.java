package io.famly.mapper.core.expression;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.JexlExpression;
import org.apache.commons.jexl3.MapContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.famly.mapper.core.model.TransformationContext;

/**
 * Expression evaluator for FHIR mapping transformations.
 * Based on JEXL with custom utility functions.
 */
public class MappingExpressionEvaluator {
    
    private static final Logger logger = LoggerFactory.getLogger(MappingExpressionEvaluator.class);
    
    private final JexlEngine jexlEngine;
    private final Map<String, JexlExpression> expressionCache;
    private final boolean cacheEnabled;
    
    public MappingExpressionEvaluator() {
        this(createDefaultJexlEngine(), true);
    }
    
    public MappingExpressionEvaluator(JexlEngine jexlEngine) {
        this(jexlEngine, true);
    }
    
    public MappingExpressionEvaluator(JexlEngine jexlEngine, boolean cacheEnabled) {
        this.jexlEngine = jexlEngine;
        this.cacheEnabled = cacheEnabled;
        this.expressionCache = cacheEnabled ? new ConcurrentHashMap<>() : null;
    }
    
    /**
     * Create a default JEXL engine with recommended settings for mapping.
     */
    private static JexlEngine createDefaultJexlEngine() {
        Map<String, Object> namespaces = new HashMap<>();
        namespaces.put("fn", new TransformFunctions());
        
        // Use RESTRICTED permissions with allowed packages
        // This is safer than UNRESTRICTED while still being functional        
        String[] allowedPackages = {
                "io.famly.mapper.core.expression.*"
        };
        
        
        org.apache.commons.jexl3.introspection.JexlPermissions permissions = 
            org.apache.commons.jexl3.introspection.JexlPermissions.RESTRICTED.compose(allowedPackages);
        
        return new JexlBuilder()
                .cache(512)
                .strict(false)
                .silent(false)
                .safe(false)
                .permissions(permissions)
                .namespaces(namespaces)
                .create();
    }
    
    /**
     * Evaluate an expression with source data and transformation context.
     */
    public Object evaluate(String expression, 
                          Object value,
                          Map<String, Object> sourceData,
                          TransformationContext context) 
            throws ExpressionEvaluationException {
        
        if (expression == null || expression.trim().isEmpty()) {
            throw new ExpressionEvaluationException("Expression cannot be null or empty");
        }
        
//        System.out.println("=== MappingExpressionEvaluator.evaluate ===");
//        System.out.println("Expression: " + expression);
//        System.out.println("Value: " + value);
//        System.out.println("Value type: " + (value != null ? value.getClass().getName() : "null"));
        
        try {
            JexlContext jexlContext = createJexlContext(value, sourceData, context);
            
//            System.out.println("Context variables: " + 
//                ((MapContext)jexlContext).entrySet().stream()
//                    .map(e -> e.getKey() + "=" + e.getValue())
//                    .collect(java.util.stream.Collectors.joining(", ")));
            
            JexlExpression jexlExpr = getOrCompileExpression(expression);
            Object result = jexlExpr.evaluate(jexlContext);
            
            // Log null results
            if (result == null) {
                logger.warn("Expression evaluated to null: '{}' with value: {}", 
                    expression, value);
            }
            
//            System.out.println("Result: " + result);
//            System.out.println("Result type: " + (result != null ? result.getClass().getName() : "null"));
            
            logger.debug("Evaluated expression '{}' = {}", expression, result);
            
            return result;
            
        } catch (JexlException e) {
        	logger.error("Expression evaluation failed: '{}' - {}", expression, e.getMessage());
//            System.err.println("JexlException: " + e.getMessage());
//            e.printStackTrace();
            throw new ExpressionEvaluationException(
                expression, 
                "Failed to evaluate expression: " + e.getMessage(), 
                e
            );
        }
    }
    
    /**
     * Evaluate a boolean condition.
     */
    public boolean evaluateCondition(String expression,
                                    Map<String, Object> sourceData,
                                    TransformationContext context) 
            throws ExpressionEvaluationException {
        
//        System.out.println("=== Evaluating CONDITION ===");
//        System.out.println("Expression: " + expression);
//        System.out.println("Context organizationId: " + (context != null ? context.getOrganizationId() : "null"));
        
        Object result = evaluate(expression, null, sourceData, context);
        
//        System.out.println("Condition result: " + result);
        
        if (result == null) {
            return false;
        }
        
        if (result instanceof Boolean) {
            return (Boolean) result;
        }
        
        // Try to coerce to boolean
        if (result instanceof Number) {
            return ((Number) result).doubleValue() != 0.0;
        }
        
        if (result instanceof String) {
            String str = (String) result;
            return !str.isEmpty() && !str.equalsIgnoreCase("false");
        }
        
        return true;
    }
    
    /**
     * Check if an expression is valid.
     */
    public boolean isValid(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            return false;
        }
        
        try {
            jexlEngine.createExpression(expression);
            return true;
        } catch (JexlException e) {
            logger.debug("Invalid expression '{}': {}", expression, e.getMessage());
            return false;
        }
    }
    
    /**
     * Get or compile and cache an expression.
     */
    private JexlExpression getOrCompileExpression(String expression) throws JexlException {
        if (cacheEnabled) {
            return expressionCache.computeIfAbsent(expression, 
                expr -> jexlEngine.createExpression(expr));
        } else {
            return jexlEngine.createExpression(expression);
        }
    }
    
    /**
     * Create a JEXL context with all necessary variables.
     */
	private JexlContext createJexlContext(Object value, Map<String, Object> sourceData, TransformationContext context) {
		MapContext jexlContext = new MapContext();

//        System.out.println("Creating JEXL Context:");
//        System.out.println("  sourceData: " + sourceData);

		// Add all source data variables
		if (sourceData != null) {
			sourceData.forEach((key, val) -> {
				jexlContext.set(key, val);
//                System.out.println("  Set context['" + key + "'] = " + val);
			});
		}

		// Add the current value being transformed
		jexlContext.set("value", value);
//        System.out.println("  Set context['value'] = " + value);

		// Add transformation context 
		if (context != null) {
			
			Map<String, Object> map = new HashMap<>();
			jexlContext.set("$ctx", map);
//            System.out.println("  Set context['ctx'] = " + context);

			// Add context variables directly for easier access
			if (context.getOrganizationId() != null) {
//				jexlContext.set("organizationId", context.getOrganizationId());
				map.put("organizationId", context.getOrganizationId());
				// System.out.println(" Set context['organizationId'] = " +
				// context.getOrganizationId());
			} else {
				// System.out.println(" context.getOrganizationId() is NULL!");
			}

			if (context.getFacilityId() != null) {
//				jexlContext.set("facilityId", context.getFacilityId());
				map.put("facilityId", context.getFacilityId());
				// System.out.println(" Set context['facilityId'] = " +
				// context.getFacilityId());
			}

			if (context.getTenantId() != null) {
//				jexlContext.set("tenantId", context.getTenantId());
				map.put("tenantId", context.getTenantId());
				// System.out.println(" Set context['tenantId'] = "+ context.getTenantId());
			}

			// Add all custom variables
			if (context.getVariables() != null) {
				map.putAll(context.getVariables());
//				context.getVariables().forEach(jexlContext::set);
//			  System.out.println("  Added " + context.getVariables().size() + " custom variables"); 
			}
			
			if(context.getSettings() != null) {
				map.putAll(context.getSettings());
			}

		} else {
			System.out.println("  TransformationContext is NULL!");
		}

		// CRITICAL: Add fn as a regular variable for dot notation access
		jexlContext.set("fn", new TransformFunctions());

		return jexlContext;
	}
    
    /**
     * Clear the expression cache.
     */
    public void clearCache() {
        if (cacheEnabled && expressionCache != null) {
            expressionCache.clear();
            logger.debug("Expression cache cleared");
        }
    }
    
    /**
     * Get the size of the expression cache.
     */
    public int getCacheSize() {
        return cacheEnabled && expressionCache != null ? expressionCache.size() : 0;
    }
}
