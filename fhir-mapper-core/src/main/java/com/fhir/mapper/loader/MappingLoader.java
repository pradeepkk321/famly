package com.fhir.mapper.loader;

import java.io.File;
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
import com.fhir.mapper.excel.MultiMappingExcelConverter;
import com.fhir.mapper.model.CodeLookupTable;
import com.fhir.mapper.model.MappingRegistry;
import com.fhir.mapper.model.ResourceMapping;
import com.fhir.mapper.security.MappingSecurityValidator;
import com.fhir.mapper.security.SecurityValidationResult;
import com.fhir.mapper.validation.MappingValidator;
import com.fhir.mapper.validation.ValidationError;
import com.fhir.mapper.validation.ValidationResult;

import ca.uhn.fhir.context.FhirContext;

public class MappingLoader {
    private final ObjectMapper objectMapper;
    private final MultiMappingExcelConverter multiConverter;
    private final String basePath;
    private final MappingValidator validator;
    private final boolean strictValidation;
    private final boolean enableExcelSupport;
    private final FhirContext fhirContext;
    
    private final String lookupsDir;
    private final String jsonDir;
    private final String excelDir;
    private final String excelGeneratedDir;
    
    private int jsonFilesLoaded = 0;
    private int excelFilesLoaded = 0;
    private int excelWorkbooksLoaded = 0;

    public MappingLoader(String basePath) {
        this(basePath, true, FhirContext.forR4(), true);
    }

    public MappingLoader(String basePath, boolean strictValidation) {
        this(basePath, strictValidation, FhirContext.forR4(), true);
    }

    public MappingLoader(String basePath, boolean strictValidation, FhirContext fhirContext) {
        this(basePath, strictValidation, fhirContext, true);
    }
    
    public MappingLoader(String basePath, boolean strictValidation, 
                        FhirContext fhirContext, boolean enableExcelSupport) {
        this.objectMapper = new ObjectMapper();
        this.multiConverter = new MultiMappingExcelConverter();
        this.basePath = basePath;
        this.strictValidation = strictValidation;
        this.fhirContext = fhirContext;
        this.validator = new MappingValidator(fhirContext);
        this.enableExcelSupport = enableExcelSupport;
        
        this.lookupsDir = basePath + "/lookups";
        this.jsonDir = basePath + "/json";
        this.excelDir = basePath + "/excel";
        this.excelGeneratedDir = basePath + "/excel-generated";
    }

    public MappingRegistry loadAll() throws IOException {
        MappingRegistry registry = new MappingRegistry();
        
        jsonFilesLoaded = 0;
        excelFilesLoaded = 0;
        excelWorkbooksLoaded = 0;
        
        registry.setFhirVersion(fhirContext.getVersion().getVersion().getFhirVersionString());
        
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║  FHIR Mapper - Loading Mappings                            ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("Base path: " + basePath);
        System.out.println("FHIR Version: " + registry.getFhirVersion());
        System.out.println("Excel Support: " + (enableExcelSupport ? "Enabled" : "Disabled"));
        System.out.println();
        
        if (enableExcelSupport) {
            cleanupExcelGeneratedDirectory();
        }
        
        System.out.println("Loading lookup tables...");
        Map<String, CodeLookupTable> lookups = loadLookupTables();
        registry.setLookupTables(lookups);
        System.out.println("✓ Loaded " + lookups.size() + " lookup tables");
        System.out.println();
        
        System.out.println("Loading resource mappings...");
        List<ResourceMapping> mappings = loadResourceMappings();
        registry.setResourceMappings(mappings);
        System.out.println("✓ Loaded " + mappings.size() + " resource mappings");
        System.out.println("  - " + jsonFilesLoaded + " from JSON directory");
        if (enableExcelSupport) {
            System.out.println("  - " + excelFilesLoaded + " from " + excelWorkbooksLoaded + " Excel workbook(s)");
        }
        System.out.println();
        
        System.out.println("Validating mappings using HAPI FHIR structure definitions...");
        ValidationResult result = validator.validateRegistry(registry);
        
        result.printWarnings();
        
        if (strictValidation) {
            result.throwIfInvalid();
        } else if (!result.isValid()) {
            System.err.println("⚠ Validation errors found but continuing (strict mode disabled):");
            for (ValidationError error : result.getErrors()) {
                System.err.println("  [ERROR] " + error.getContext() + ": " + error.getMessage());
            }
        }
        
        System.out.println("Running security validation...");
        MappingSecurityValidator securityValidator = new MappingSecurityValidator();
        SecurityValidationResult securityResult = securityValidator.validateRegistry(registry);
        
        if (securityResult.hasIssues()) {
            securityResult.printReport();
            securityResult.throwIfCritical();
        } else {
            System.out.println("✓ Security validation passed - no issues found");
        }
        
        System.out.println();
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║  Mapping registry loaded successfully                      ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
        
        return registry;
    }

    private Map<String, CodeLookupTable> loadLookupTables() throws IOException {
        Path lookupsPath = Paths.get(lookupsDir);
        if (!Files.exists(lookupsPath)) {
            System.out.println("  ℹ No lookups directory found, skipping lookup tables");
            return Collections.emptyMap();
        }

        Map<String, CodeLookupTable> lookups = new HashMap<>();
        
        List<Path> lookupFiles = Files.walk(lookupsPath, 1)
            .filter(p -> p.toString().endsWith(".json"))
            .collect(Collectors.toList());

        for (Path file : lookupFiles) {
            try {
                CodeLookupTable lookup = objectMapper.readValue(file.toFile(), CodeLookupTable.class);
                lookups.put(lookup.getId(), lookup);
                System.out.println("  ✓ " + lookup.getId() + " (" + file.getFileName() + ")");
            } catch (Exception e) {
                throw new IOException("Failed to load lookup from " + file + ": " + e.getMessage(), e);
            }
        }

        return lookups;
    }

    private List<ResourceMapping> loadResourceMappings() throws IOException {
        List<ResourceMapping> mappings = new ArrayList<>();
        Map<String, String> seenIds = new HashMap<>();
        
        Path jsonPath = Paths.get(jsonDir);
        if (Files.exists(jsonPath)) {
            List<Path> jsonFiles = Files.walk(jsonPath, 1)
                .filter(p -> p.toString().endsWith(".json"))
                .collect(Collectors.toList());

            for (Path file : jsonFiles) {
                try {
                    ResourceMapping mapping = objectMapper.readValue(file.toFile(), ResourceMapping.class);
                    
                    if (seenIds.containsKey(mapping.getId())) {
                        throw new IOException(
                            "Duplicate mapping ID '" + mapping.getId() + "' found in: " +
                            seenIds.get(mapping.getId()) + " and json/" + file.getFileName()
                        );
                    }
                    
                    mappings.add(mapping);
                    seenIds.put(mapping.getId(), "json/" + file.getFileName());
                    jsonFilesLoaded++;
                    
                    System.out.println("  ✓ " + mapping.getId() + 
                        " [" + mapping.getDirection() + "] (JSON: " + file.getFileName() + ")");
                } catch (Exception e) {
                    String errorMsg = "Failed to load mapping from " + file + ": " + e.getMessage();
                    if (strictValidation) {
                        throw new IOException(errorMsg, e);
                    } else {
                        System.err.println("  ✗ " + errorMsg);
                    }
                }
            }
        }
        
        if (enableExcelSupport) {
            Path excelPath = Paths.get(excelDir);
            if (Files.exists(excelPath)) {
                File generatedDir = new File(excelGeneratedDir);
                if (!generatedDir.exists()) {
                    generatedDir.mkdirs();
                }
                
                List<Path> excelFiles = Files.walk(excelPath, 1)
                    .filter(p -> p.toString().endsWith(".xlsx") || p.toString().endsWith(".xls"))
                    .collect(Collectors.toList());

                for (Path file : excelFiles) {
                    try {
                        System.out.println("  📊 Converting Excel workbook: " + file.getFileName() + "...");
                        
                        List<ResourceMapping> excelMappings = 
                            multiConverter.excelToResourceMappings(file.toString());
                        
                        excelWorkbooksLoaded++;
                        
                        for (ResourceMapping mapping : excelMappings) {
                            if (seenIds.containsKey(mapping.getId())) {
                                throw new IOException(
                                    "Duplicate mapping ID '" + mapping.getId() + "' found in: " +
                                    seenIds.get(mapping.getId()) + " and excel/" + file.getFileName() +
                                    " (sheet: " + mapping.getName() + ")"
                                );
                            }
                            
                            String jsonFilename = mapping.getId() + ".json";
                            String jsonFilePath = excelGeneratedDir + "/" + jsonFilename;
                            
                            objectMapper.writerWithDefaultPrettyPrinter()
                                .writeValue(new File(jsonFilePath), mapping);
                            
                            mappings.add(mapping);
                            seenIds.put(mapping.getId(), "excel/" + file.getFileName());
                            excelFilesLoaded++;
                            
                            System.out.println("    ✓ " + mapping.getId() + 
                                " [" + mapping.getDirection() + "] (Sheet: " + mapping.getName() + ")");
                            System.out.println("      → Generated: excel-generated/" + jsonFilename);
                        }
                        
                    } catch (Exception e) {
                        String errorMsg = "Failed to load Excel workbook " + file + ": " + e.getMessage();
                        if (strictValidation) {
                            throw new IOException(errorMsg, e);
                        } else {
                            System.err.println("  ✗ " + errorMsg);
                            System.err.println("    Skipping this workbook and continuing...");
                        }
                    }
                }
            }
        }

        return mappings;
    }

    public ResourceMapping loadResourceMapping(String filename) throws IOException {
        Path jsonFile = Paths.get(jsonDir, filename);
        if (Files.exists(jsonFile)) {
            return objectMapper.readValue(jsonFile.toFile(), ResourceMapping.class);
        }
        
        if (enableExcelSupport) {
            Path excelFile = Paths.get(excelDir, filename);
            if (Files.exists(excelFile)) {
                List<ResourceMapping> mappings = multiConverter.excelToResourceMappings(excelFile.toString());
                if (mappings.isEmpty()) {
                    throw new IOException("No mappings found in Excel file: " + filename);
                }
                return mappings.get(0);
            }
        }
        
        throw new IOException("Mapping file not found: " + filename);
    }

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

    public ValidationResult validateOnly() throws IOException {
        MappingRegistry tempRegistry = new MappingRegistry();
        tempRegistry.setLookupTables(loadLookupTables());
        tempRegistry.setResourceMappings(loadResourceMappings());
        return validator.validateRegistry(tempRegistry);
    }
    
    private void cleanupExcelGeneratedDirectory() {
        File generatedDir = new File(excelGeneratedDir);
        
        if (generatedDir.exists() && generatedDir.isDirectory()) {
            System.out.println("Cleaning up excel-generated directory...");
            File[] files = generatedDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().endsWith(".json")) {
                        file.delete();
                    }
                }
            }
            System.out.println("✓ Cleaned up excel-generated directory");
            System.out.println();
        } else {
            generatedDir.mkdirs();
        }
    }
    
    public LoadStatistics getLoadStatistics() {
        return new LoadStatistics(jsonFilesLoaded, excelFilesLoaded, excelWorkbooksLoaded);
    }
    
    public boolean isExcelSupportEnabled() {
        return enableExcelSupport;
    }
    
    public static class LoadStatistics {
        private final int jsonFilesLoaded;
        private final int excelFilesLoaded;
        private final int excelWorkbooksLoaded;
        
        public LoadStatistics(int jsonFilesLoaded, int excelFilesLoaded, int excelWorkbooksLoaded) {
            this.jsonFilesLoaded = jsonFilesLoaded;
            this.excelFilesLoaded = excelFilesLoaded;
            this.excelWorkbooksLoaded = excelWorkbooksLoaded;
        }
        
        public int getJsonFilesLoaded() {
            return jsonFilesLoaded;
        }
        
        public int getExcelFilesLoaded() {
            return excelFilesLoaded;
        }
        
        public int getExcelWorkbooksLoaded() {
            return excelWorkbooksLoaded;
        }
        
        public int getTotalMappingsLoaded() {
            return jsonFilesLoaded + excelFilesLoaded;
        }
        
        @Override
        public String toString() {
            return String.format("LoadStatistics{totalMappings=%d, json=%d, excelMappings=%d, excelWorkbooks=%d}", 
                getTotalMappingsLoaded(), jsonFilesLoaded, excelFilesLoaded, excelWorkbooksLoaded);
        }
    }
}