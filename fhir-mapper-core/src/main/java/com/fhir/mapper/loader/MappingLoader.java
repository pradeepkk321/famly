package com.fhir.mapper.loader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fhir.mapper.model.CodeLookupTable;
import com.fhir.mapper.model.MappingRegistry;
import com.fhir.mapper.model.ResourceMapping;
import com.fhir.mapper.security.MappingSecurityValidator;
import com.fhir.mapper.security.SecurityValidationResult;
import com.fhir.mapper.validation.MappingValidator;
import com.fhir.mapper.validation.ValidationError;
import com.fhir.mapper.validation.ValidationResult;

import ca.uhn.fhir.context.FhirContext;

/**
 * Loads mappings and lookup tables from JSON files with validation
 * 
 * Expected directory structure:
 * /mappings
 *   /lookups
 *     gender-lookup.json
 *     encounter-status-lookup.json
 *   /resources
 *     patient-json-to-fhir.json
 *     patient-fhir-to-json.json
 *     encounter-json-to-fhir.json
 */
public class MappingLoader {
    private final ObjectMapper objectMapper;
    private final String basePath;
    private final MappingValidator validator;
    private final boolean strictValidation;
    private final FhirContext fhirContext;

    public MappingLoader(String basePath) {
        this(basePath, true, FhirContext.forR4());
    }

    public MappingLoader(String basePath, boolean strictValidation) {
        this(basePath, strictValidation, FhirContext.forR4());
    }

    public MappingLoader(String basePath, boolean strictValidation, FhirContext fhirContext) {
        this.objectMapper = new ObjectMapper();
        this.basePath = basePath;
        this.strictValidation = strictValidation;
        this.fhirContext = fhirContext;
        this.validator = new MappingValidator(fhirContext);
    }

    /**
     * Load all mappings and lookups into registry with validation
     */
    public MappingRegistry loadAll() throws IOException {
        MappingRegistry registry = new MappingRegistry();
        
        // Set FHIR version from context
        registry.setFhirVersion(fhirContext.getVersion().getVersion().getFhirVersionString());
        
        System.out.println("Loading mappings from: " + basePath);
        System.out.println("FHIR Version: " + registry.getFhirVersion());
        
        // Load lookup tables first
        Map<String, CodeLookupTable> lookups = loadLookupTables();
        registry.setLookupTables(lookups);
        System.out.println("Loaded " + lookups.size() + " lookup tables");
        
        // Load resource mappings
        List<ResourceMapping> mappings = loadResourceMappings();
        registry.setResourceMappings(mappings);
        System.out.println("Loaded " + mappings.size() + " resource mappings");
        
        // Validate entire registry
        System.out.println("Validating mappings using HAPI FHIR structure definitions...");
        ValidationResult result = validator.validateRegistry(registry);
        
        // Print warnings
        result.printWarnings();
        
        // Throw exception if validation fails and strict mode enabled
        if (strictValidation) {
            result.throwIfInvalid();
        } else if (!result.isValid()) {
            System.err.println("Validation errors found but continuing (strict mode disabled):");
            for (ValidationError error : result.getErrors()) {
                System.err.println("  [ERROR] " + error.getContext() + ": " + error.getMessage());
            }
        }
        
        // SECURITY VALIDATION - Always run regardless of strict mode
        System.out.println("Running security validation...");
        MappingSecurityValidator securityValidator = 
            new MappingSecurityValidator();
        SecurityValidationResult securityResult = 
            securityValidator.validateRegistry(registry);
        
        if (securityResult.hasIssues()) {
            securityResult.printReport();
            // Always fail on CRITICAL security issues
            securityResult.throwIfCritical();
        } else {
            System.out.println("✓ Security validation passed - no issues found");
        }
        
        System.out.println("Mapping registry loaded successfully");
        return registry;
    }

    /**
     * Load all lookup tables from lookups directory
     */
    private Map<String, CodeLookupTable> loadLookupTables() throws IOException {
        Path lookupsDir = Paths.get(basePath, "lookups");
        if (!Files.exists(lookupsDir)) {
            System.out.println("No lookups directory found, skipping lookup tables");
            return Collections.emptyMap();
        }

        Map<String, CodeLookupTable> lookups = new HashMap<>();
        
        List<Path> lookupFiles = Files.walk(lookupsDir, 1)
            .filter(p -> p.toString().endsWith(".json"))
            .collect(Collectors.toList());

        for (Path file : lookupFiles) {
            try {
                CodeLookupTable lookup = objectMapper.readValue(file.toFile(), CodeLookupTable.class);
                lookups.put(lookup.getId(), lookup);
                System.out.println("  Loaded lookup: " + lookup.getId() + " (" + file.getFileName() + ")");
            } catch (Exception e) {
                throw new IOException("Failed to load lookup from " + file + ": " + e.getMessage(), e);
            }
        }

        return lookups;
    }

    /**
     * Load all resource mappings from resources directory
     */
    private List<ResourceMapping> loadResourceMappings() throws IOException {
        Path resourcesDir = Paths.get(basePath, "resources");
        if (!Files.exists(resourcesDir)) {
            throw new IOException("Resources directory not found: " + resourcesDir);
        }

        List<ResourceMapping> mappings = new ArrayList<>();
        
        List<Path> mappingFiles = Files.walk(resourcesDir, 1)
            .filter(p -> p.toString().endsWith(".json"))
            .collect(Collectors.toList());

        for (Path file : mappingFiles) {
            try {
                ResourceMapping mapping = objectMapper.readValue(file.toFile(), ResourceMapping.class);
                mappings.add(mapping);
                System.out.println("  Loaded mapping: " + mapping.getId() + 
                    " [" + mapping.getDirection() + "] (" + file.getFileName() + ")");
            } catch (Exception e) {
                throw new IOException("Failed to load mapping from " + file + ": " + e.getMessage(), e);
            }
        }

        return mappings;
    }

    /**
     * Load single lookup table
     */
    public CodeLookupTable loadLookupTable(String filename) throws IOException {
        Path file = Paths.get(basePath, "lookups", filename);
        return objectMapper.readValue(file.toFile(), CodeLookupTable.class);
    }

    /**
     * Load single resource mapping
     */
    public ResourceMapping loadResourceMapping(String filename) throws IOException {
        Path file = Paths.get(basePath, "resources", filename);
        return objectMapper.readValue(file.toFile(), ResourceMapping.class);
    }

    /**
     * Hot reload - reload all mappings with validation
     */
    public void reload(MappingRegistry registry) throws IOException {
        Map<String, CodeLookupTable> lookups = loadLookupTables();
        registry.setLookupTables(lookups);
        
        List<ResourceMapping> mappings = loadResourceMappings();
        registry.setResourceMappings(mappings);
        
        ValidationResult result = validator.validateRegistry(registry);
        result.printWarnings();
        
        if (strictValidation) {
            result.throwIfInvalid();
        }
    }

    /**
     * Validate without loading (dry-run)
     */
    public ValidationResult validateOnly() throws IOException {
        MappingRegistry tempRegistry = new MappingRegistry();
        tempRegistry.setLookupTables(loadLookupTables());
        tempRegistry.setResourceMappings(loadResourceMappings());
        return validator.validateRegistry(tempRegistry);
    }
}

/**
 * Example file structures
 */

// File: mappings/lookups/gender-lookup.json
/*
{
  "id": "gender-lookup",
  "name": "Gender Code Mapping",
  "sourceSystem": "internal",
  "targetSystem": "http://hl7.org/fhir/administrative-gender",
  "mappings": [
    {"sourceCode": "M", "targetCode": "male", "display": "Male"},
    {"sourceCode": "F", "targetCode": "female", "display": "Female"}
  ]
}
*/

// File: mappings/resources/patient-mapping.json
/*
{
  "id": "patient-mapping-v1",
  "name": "Patient Mapping",
  "version": "1.0.0",
  "sourceType": "PatientDTO",
  "targetResourceType": "Patient",
  "bidirectional": true,
  "fieldMappings": [
    {
      "id": "patient-identifier",
      "sourcePath": "patientId",
      "targetPath": "identifier[0].value",
      "dataType": "string",
      "required": true
    },
    {
      "id": "patient-identifier-system",
      "sourcePath": null,
      "targetPath": "identifier[0].system",
      "dataType": "string",
      "defaultValue": "$ctx.identifierSystem",
      "required": true
    },
    {
      "id": "patient-gender",
      "sourcePath": "gender",
      "targetPath": "gender",
      "dataType": "code",
      "lookupTable": "gender-lookup",
      "required": true
    }
  ]
}
*/