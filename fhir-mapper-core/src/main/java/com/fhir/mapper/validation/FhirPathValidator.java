package com.fhir.mapper.validation;

import java.util.List;

import ca.uhn.fhir.context.BaseRuntimeChildDefinition;
import ca.uhn.fhir.context.BaseRuntimeElementDefinition;
import ca.uhn.fhir.context.RuntimeChildResourceBlockDefinition;
import ca.uhn.fhir.context.RuntimeCompositeDatatypeDefinition;
import ca.uhn.fhir.context.RuntimePrimitiveDatatypeDefinition;
import ca.uhn.fhir.context.RuntimeResourceBlockDefinition;
import ca.uhn.fhir.context.RuntimeResourceDefinition;

/**
 * Validates FHIR paths using HAPI FHIR structure definitions
 */
public class FhirPathValidator {
    private final ca.uhn.fhir.context.FhirContext fhirContext;
    
    public FhirPathValidator(ca.uhn.fhir.context.FhirContext fhirContext) {
        this.fhirContext = fhirContext;
    }
    
    /**
     * Check if resource type is valid
     */
    public boolean isValidResourceType(String resourceType) {
        try {
            fhirContext.getResourceDefinition(resourceType);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get expected FHIR type for a path
     * Returns null for nested paths that can't be validated
     */
    public String getExpectedType(String resourceType, String path) {
        try {
            // For nested paths, return null to skip type validation
            String normalizedPath = normalizePath(path);
            String[] parts = normalizedPath.split("\\.");
            
            if (parts.length > 1) {
                // Don't attempt to validate types for nested paths
                return null;
            }
            
            BaseRuntimeElementDefinition<?> elementDef = getElementDefinition(resourceType, path);
            if (elementDef != null) {
                return elementDef.getName();
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }
    
    /**
     * Validate FHIR path exists (without strict type checking for nested paths)
     */
    public ValidationResult validatePathExistsOld(String resourceType, String path) {
        ValidationResult result = new ValidationResult();
        
        if (!isValidResourceType(resourceType)) {
            result.addError("FHIRPath", "Unknown resource type: " + resourceType);
            return result;
        }
        
        try {
            RuntimeResourceDefinition resourceDef = 
                fhirContext.getResourceDefinition(resourceType);
            
            String normalizedPath = normalizePath(path);
            String[] parts = normalizedPath.split("\\.");
            
            // Just validate first-level field exists
            if (parts.length > 0) {
                String firstPart = parts[0];
                BaseRuntimeChildDefinition childDef = resourceDef.getChildByName(firstPart);
                
                if (childDef == null) {
                    result.addError("FHIRPath", 
                        "Field '" + firstPart + "' does not exist in " + resourceType);
                    return result;
                }
                
                // First level validated successfully
                // For nested paths (2+ levels), we don't validate further
                // This is acceptable as the transformation engine handles dynamic path creation
            }
            
        } catch (Exception e) {
            result.addWarning("FHIRPath", 
                "Could not fully validate path '" + path + "': " + e.getMessage());
        }
        
        return result;
    }
    
    public ValidationResult validatePathExists(String resourceType, String path) {
        ValidationResult result = new ValidationResult();
        
        if (!isValidResourceType(resourceType)) {
            result.addError("FHIRPath", "Unknown resource type: " + resourceType);
            return result;
        }
        
        try {
            RuntimeResourceDefinition resourceDef = 
                fhirContext.getResourceDefinition(resourceType);
            validatePathAgainstDefinition(resourceDef, path, result);
        } catch (Exception e) {
            result.addWarning("FHIRPath", 
                "Could not fully validate path '" + path + "': " + e.getMessage());
        }
        
        return result;
        
        
    }
    
    /**
     * Validate FHIR path for a resource using HAPI structure definitions
     * @deprecated Use validatePathExists for simpler validation
     */
    @Deprecated
    public ValidationResult validatePath(String resourceType, String path) {
        return validatePathExists(resourceType, path);
    }
    
    /**
     * Normalize path by removing array indices
     */
    private String normalizePath(String path) {
        // Remove array indices: name[0].given[0] -> name.given
        return path.replaceAll("\\[\\d+\\]", "");
    }
    
    /**
     * Validate path against HAPI structure definition
     */
    private void validatePathAgainstDefinition(
            RuntimeResourceDefinition resourceDef,
            String path, 
            ValidationResult result) {
        
    	String normalizedPath = normalizePath(path);
        String[] parts = normalizedPath.split("\\.");
        BaseRuntimeElementDefinition<?> currentDef = resourceDef;
        
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            
            if (currentDef instanceof RuntimeResourceDefinition) {
                RuntimeResourceDefinition resDef = (RuntimeResourceDefinition) currentDef;
                BaseRuntimeChildDefinition childDef = resDef.getChildByName(part);
                
                if (childDef == null) {
                    result.addError("FHIRPath", 
                        "Field '" + part + "' does not exist in " + resDef.getName());
                    return;
                }
                
                // Get the child element definition for next iteration
                if (i < parts.length - 1) {
                    currentDef = getChildElementDefinition(childDef, part);
                    handlePrimitives(resourceDef, path, result, parts, currentDef, i, part);
                }
                
            } else if (currentDef instanceof RuntimeCompositeDatatypeDefinition) {
                RuntimeCompositeDatatypeDefinition compositeDef = 
                    (RuntimeCompositeDatatypeDefinition) currentDef;
                
                BaseRuntimeChildDefinition childDef = compositeDef.getChildByName(part);
                
                if (childDef == null) {
                    result.addError("FHIRPath", 
                        "Field '" + part + "' does not exist in " + compositeDef.getName());
                    return;
                }
                
                // Get next level definition
                if (i < parts.length - 1) {
                    currentDef = getChildElementDefinition(childDef, part);
                    handlePrimitives(resourceDef, path, result, parts, currentDef, i, part);
                }
            } else if (currentDef instanceof RuntimeResourceBlockDefinition) {
            	RuntimeResourceBlockDefinition blockDef = (RuntimeResourceBlockDefinition) currentDef;
            	
            	BaseRuntimeChildDefinition childDef = blockDef.getChildByName(part);
            	
                if (childDef == null) {
                    result.addError("FHIRPath", 
                        "Field '" + part + "' does not exist in " + blockDef.getName());
                    return;
                }
                
                // Get next level definition
                if (i < parts.length - 1) {
                    currentDef = getChildElementDefinition(childDef, part);
                    handlePrimitives(resourceDef, path, result, parts, currentDef, i, part);
                }
            	
            } 
            else {
                // Primitive or other type - can't validate further
                if (i < parts.length - 1) {
                    result.addWarning("FHIRPath", 
                        "Cannot validate nested path beyond primitive/complex type at: " + part);
                }
                return;
            }
        }
    }

	private void handlePrimitives(RuntimeResourceDefinition resourceDef, String path, ValidationResult result,
			String[] parts, BaseRuntimeElementDefinition<?> currentDef, int i, String part) {
		if (currentDef == null) {
		    result.addWarning("FHIRPath",
		        "Cannot fully validate path beyond '" + part + "' - type information unavailable");
		    return;
		} else if (currentDef instanceof RuntimePrimitiveDatatypeDefinition) {
			RuntimePrimitiveDatatypeDefinition primitiveDef = (RuntimePrimitiveDatatypeDefinition) currentDef;
			
			result.addError("FHIRPath", "Field '" + parts[i+1] + "' is not allowed after '" + part + "(" + primitiveDef.getName() + ")" );
			return;
		}
	}
    
    /**
     * Get child element definition from a child definition
     */
    private BaseRuntimeElementDefinition<?> getChildElementDefinition(
            BaseRuntimeChildDefinition childDef, String childName) {
        
        try {
            // BaseRuntimeChildDefinition has a method to get child element definition
            // For primitive children, this returns the primitive type
            // For composite children, this returns the composite datatype definition
            
            // Try to get the element definition by the child name
            BaseRuntimeElementDefinition<?> elementDef = childDef.getChildByName(childName);
            if (elementDef != null) {
                return elementDef;
            }
            
            // For child elements like "communication", we need to check if it's a 
            // RuntimeChildResourceBlockDefinition which has specific handling
            if (childDef instanceof ca.uhn.fhir.context.RuntimeChildResourceBlockDefinition) {
                ca.uhn.fhir.context.RuntimeChildResourceBlockDefinition blockDef = 
                    (ca.uhn.fhir.context.RuntimeChildResourceBlockDefinition) childDef;
                // This returns the resource block definition (e.g., PatientCommunicationComponent)
                return blockDef.getChildByName(childName);
            }
            
            // For other types, try getting through valid child names
            List<String> validNames = (List<String>) childDef.getValidChildNames();
            if (validNames != null && !validNames.isEmpty()) {
                // Try the first valid child name
                String firstName = validNames.get(0);
                return childDef.getChildByName(firstName);
            }
            
        } catch (Exception e) {
            // Ignore and return null
        }
        
        return null;
    }
    
    /**
     * Get element definition for a path (useful for type checking)
     */
    public BaseRuntimeElementDefinition<?> getElementDefinition(
            String resourceType, String path) {
        try {
            RuntimeResourceDefinition resourceDef = 
                fhirContext.getResourceDefinition(resourceType);
            
            String normalizedPath = normalizePath(path);
            String[] parts = normalizedPath.split("\\.");
            
            BaseRuntimeElementDefinition<?> currentDef = resourceDef;
            
            for (int i = 0; i < parts.length; i++) {
                String part = parts[i];
                
                if (currentDef instanceof RuntimeResourceDefinition) {
                    RuntimeResourceDefinition resDef = (RuntimeResourceDefinition) currentDef;
                    BaseRuntimeChildDefinition childDef = resDef.getChildByName(part);
                    if (childDef != null) {
                        // For the last part, try to get the actual element type
                        if (i == parts.length - 1) {
                            currentDef = getChildElementDefinition(childDef, part);
                        } else {
                            currentDef = getChildElementDefinition(childDef, part);
                        }
                    } else {
                        return null;
                    }
                } else if (currentDef instanceof RuntimeCompositeDatatypeDefinition) {
                    RuntimeCompositeDatatypeDefinition compositeDef = 
                        (RuntimeCompositeDatatypeDefinition) currentDef;
                    BaseRuntimeChildDefinition childDef = compositeDef.getChildByName(part);
                    if (childDef != null) {
                        currentDef = getChildElementDefinition(childDef, part);
                    } else {
                        return null;
                    }
                } else {
                    // Can't traverse further
                    return currentDef;
                }
                
                if (currentDef == null) {
                    return null;
                }
            }
            
            return currentDef;
        } catch (Exception e) {
            return null;
        }
    }
}