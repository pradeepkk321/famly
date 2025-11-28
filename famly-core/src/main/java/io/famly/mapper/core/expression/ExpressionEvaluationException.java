package io.famly.mapper.core.expression;

public class ExpressionEvaluationException extends Exception {
    
    private final String expression;
    
    public ExpressionEvaluationException(String message) {
        super(message);
        this.expression = null;
    }
    
    public ExpressionEvaluationException(String message, Throwable cause) {
        super(message, cause);
        this.expression = null;
    }
    
    public ExpressionEvaluationException(String expression, String message) {
        super(message);
        this.expression = expression;
    }
    
    public ExpressionEvaluationException(String expression, String message, Throwable cause) {
        super(message, cause);
        this.expression = expression;
    }
    
    public String getExpression() {
        return expression;
    }
    
    @Override
    public String toString() {
        if (expression != null) {
            return "ExpressionEvaluationException{expression='" + expression + "', message='" + getMessage() + "'}";
        }
        return super.toString();
    }
}