package com.fhir.mapper.expression;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Utility functions available in JEXL expressions for FHIR mapping.
 * Functions are accessed via 'fn' namespace: fn.uppercase(), fn.removeHyphens(), etc.
 */
public class TransformFunctions {
    
    // ====================
    // String Functions
    // ====================
    
    public String uppercase(String value) {
        return value != null ? value.toUpperCase() : null;
    }
    
    public String lowercase(String value) {
        return value != null ? value.toLowerCase() : null;
    }
    
    public String trim(String value) {
        return value != null ? value.trim() : null;
    }
    
    public String substring(String value, int start, int end) {
        return value != null ? value.substring(start, end) : null;
    }
    
    public String replace(String str, String target, String replacement) {
        System.out.println("replace called: str=" + str + ", target=" + target + ", replacement=" + replacement);
        if (str == null) return null;
        String result = str.replace(target, replacement);
        System.out.println("replace result: " + result);
        return result;
    }
    
    public String removeHyphens(String value) {
        System.out.println("removeHyphens called with: " + value);
        String result = value != null ? value.replace("-", "") : null;
        System.out.println("removeHyphens result: " + result);
        return result;
    }
    
    public String formatSSN(String value) {
        return value != null ? value.replaceAll("[^0-9]", "") : null;
    }
    
    public String concat(String... values) {
        if (values == null) return "";
        StringBuilder sb = new StringBuilder();
        for (String val : values) {
            if (val != null) {
                sb.append(val);
            }
        }
        return sb.toString();
    }
    
    public boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }
    
    public boolean isNotEmpty(String str) {
        return str != null && !str.isEmpty();
    }
    
    public boolean contains(String str, String substring) {
        return str != null && substring != null && str.contains(substring);
    }
    
    public boolean startsWith(String str, String prefix) {
        return str != null && prefix != null && str.startsWith(prefix);
    }
    
    public boolean endsWith(String str, String suffix) {
        return str != null && suffix != null && str.endsWith(suffix);
    }
    
    // ====================
    // Date/Time Functions
    // ====================
    
    public String formatDate(String date, String pattern) {
        if (date == null) return null;
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
            LocalDate localDate = LocalDate.parse(date);
            return formatter.format(localDate);
        } catch (Exception e) {
            return date;
        }
    }
    
    public String formatDateTime(Object dateTime, String pattern) {
        if (dateTime == null) return null;
        
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
            
            if (dateTime instanceof Instant) {
                return formatter.format(((Instant) dateTime).atZone(ZoneId.systemDefault()));
            } else if (dateTime instanceof LocalDateTime) {
                return formatter.format((LocalDateTime) dateTime);
            } else if (dateTime instanceof LocalDate) {
                return formatter.format((LocalDate) dateTime);
            } else if (dateTime instanceof String) {
                LocalDate date = LocalDate.parse((String) dateTime);
                return formatter.format(date);
            }
        } catch (Exception e) {
            return dateTime.toString();
        }
        
        return dateTime.toString();
    }
    
    public Instant now() {
        return Instant.now();
    }
    
    public LocalDate today() {
        return LocalDate.now();
    }
    
    // ====================
    // Type Conversion
    // ====================
    
    public Double toDouble(Object value) {
        if (value == null) return null;
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
    
    public Integer toInt(Object value) {
        if (value == null) return null;
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
    
    public Boolean toBoolean(Object value) {
        if (value == null) return null;
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return null;
    }
    
    // ====================
    // Utility Functions
    // ====================
    
    public Object defaultIfNull(Object value, Object defaultValue) {
        return value != null ? value : defaultValue;
    }
    
    public Object coalesce(Object... values) {
        if (values == null) return null;
        for (Object value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }
    
    public String uuid() {
        return UUID.randomUUID().toString();
    }
}