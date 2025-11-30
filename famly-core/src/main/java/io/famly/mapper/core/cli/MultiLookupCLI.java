package io.famly.mapper.core.cli;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.famly.mapper.core.excel.ExcelLookupConverter;
import io.famly.mapper.core.excel.LookupJsonToExcelConverter;
import io.famly.mapper.core.model.CodeLookupTable;

/**
 * CLI for multi-lookup Excel workbooks
 * 
 * Commands:
 *   excel-to-json    Convert Excel workbook (multiple lookups) to JSON files
 *   json-to-excel    Convert JSON files to single Excel workbook
 *   validate         Validate lookups
 */
public class MultiLookupCLI {
    
    private static final ExcelLookupConverter excelConverter = new ExcelLookupConverter();
    private static final LookupJsonToExcelConverter jsonToExcelConverter = new LookupJsonToExcelConverter();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    public static void main(String[] args) {
        if (args.length == 0) {
            printHelp();
            return;
        }
        
        String command = args[0];
        
        try {
            switch (command) {
                case "excel-to-json":
                    handleExcelToJson(args);
                    break;
                    
                case "json-to-excel":
                    handleJsonToExcel(args);
                    break;
                    
                case "validate":
                    handleValidate(args);
                    break;
                    
                default:
                    System.err.println("Unknown command: " + command);
                    printHelp();
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    /**
     * Convert Excel workbook to multiple JSON files
     */
    private static void handleExcelToJson(String[] args) throws Exception {
        if (args.length < 3) {
            System.err.println("Usage: excel-to-json <input.xlsx> <output-directory>");
            System.exit(1);
        }
        
        String inputFile = args[1];
        String outputDir = args[2];
        
        if (!new File(inputFile).exists()) {
            System.err.println("Input file not found: " + inputFile);
            System.exit(1);
        }
        
        System.out.println("Converting Excel workbook to JSON files...");
        System.out.println("Input: " + inputFile);
        System.out.println("Output directory: " + outputDir);
        System.out.println();
        
        // Create output directory if it doesn't exist
        File outputDirFile = new File(outputDir);
        if (!outputDirFile.exists()) {
            outputDirFile.mkdirs();
        }
        
        List<CodeLookupTable> lookups = excelConverter.excelToLookupTables(inputFile);
        
        int createdCount = 0;
        for (CodeLookupTable lookup : lookups) {
            String lookupId = lookup.getId() != null ? lookup.getId() : "unknown";
            String jsonFileName = outputDir + File.separator + lookupId + ".json";
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(jsonFileName), lookup);
            System.out.println("  ✓ Created: " + jsonFileName);
            createdCount++;
        }
        
        System.out.println();
        System.out.println("✓ Successfully converted " + createdCount + " lookups");
    }
    
    /**
     * Convert JSON files to single Excel workbook
     */
    private static void handleJsonToExcel(String[] args) throws Exception {
        if (args.length < 3) {
            System.err.println("Usage: json-to-excel <json-directory> <output.xlsx>");
            System.err.println("   or: json-to-excel <file1.json> <file2.json> ... <output.xlsx>");
            System.exit(1);
        }
        
        String lastArg = args[args.length - 1];
        
        // Check if first argument is directory or file
        File firstArg = new File(args[1]);
        
        if (firstArg.isDirectory()) {
            // Directory mode
            String jsonDir = args[1];
            String outputFile = args[2];
            
            System.out.println("Converting JSON files from directory to Excel workbook...");
            System.out.println("Input directory: " + jsonDir);
            System.out.println("Output: " + outputFile);
            System.out.println();
            
            jsonToExcelConverter.convertDirectoryToExcel(jsonDir, outputFile);
            
        } else {
            // Multiple files mode
            List<String> jsonFiles = Arrays.asList(args).subList(1, args.length - 1);
            String outputFile = lastArg;
            
            System.out.println("Converting " + jsonFiles.size() + " JSON files to Excel workbook...");
            System.out.println("Output: " + outputFile);
            System.out.println();
            
            List<CodeLookupTable> lookups = new java.util.ArrayList<>();
            for (String jsonFile : jsonFiles) {
                if (!new File(jsonFile).exists()) {
                    System.err.println("Warning: File not found: " + jsonFile);
                    continue;
                }
                CodeLookupTable lookup = objectMapper.readValue(new File(jsonFile), CodeLookupTable.class);
                lookups.add(lookup);
                System.out.println("  ✓ Loaded: " + jsonFile);
            }
            
            System.out.println();
            jsonToExcelConverter.lookupsToExcel(lookups, outputFile);
        }
        
        System.out.println("✓ Successfully created Excel workbook");
    }
    
    /**
     * Validate lookups
     */
    private static void handleValidate(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: validate <lookups-directory>");
            System.exit(1);
        }
        
        String lookupsDir = args[1];
        
        if (!new File(lookupsDir).exists()) {
            System.err.println("Lookups directory not found: " + lookupsDir);
            System.exit(1);
        }
        
        System.out.println("Validating lookups in: " + lookupsDir);
        System.out.println();
        
        File dir = new File(lookupsDir);
        File[] jsonFiles = dir.listFiles((d, name) -> name.endsWith(".json"));
        File[] excelFiles = dir.listFiles((d, name) -> name.endsWith(".xlsx") || name.endsWith(".xls"));
        
        int validCount = 0;
        int invalidCount = 0;
        int totalMappings = 0;
        
        // Validate JSON files
        if (jsonFiles != null && jsonFiles.length > 0) {
            for (File jsonFile : jsonFiles) {
                try {
                    CodeLookupTable lookup = objectMapper.readValue(jsonFile, CodeLookupTable.class);
                    
                    // Validate lookup
                    if (!validateLookup(lookup)) {
                        invalidCount++;
                        continue;
                    }
                    
                    validCount++;
                    if (lookup.getMappings() != null) {
                        totalMappings += lookup.getMappings().size();
                    }
                    System.out.println("  ✓ " + lookup.getId() + " (" + 
                        (lookup.getMappings() != null ? lookup.getMappings().size() : 0) + " mappings)");
                    
                } catch (Exception e) {
                    invalidCount++;
                    System.err.println("  ✗ " + jsonFile.getName() + " - " + e.getMessage());
                }
            }
        }
        
        // Validate Excel files
        if (excelFiles != null && excelFiles.length > 0) {
            for (File excelFile : excelFiles) {
                try {
                    List<CodeLookupTable> lookups = excelConverter.excelToLookupTables(excelFile.getAbsolutePath());
                    
                    for (CodeLookupTable lookup : lookups) {
                        if (!validateLookup(lookup)) {
                            invalidCount++;
                            continue;
                        }
                        
                        validCount++;
                        if (lookup.getMappings() != null) {
                            totalMappings += lookup.getMappings().size();
                        }
                        System.out.println("  ✓ " + lookup.getId() + " (" + 
                            (lookup.getMappings() != null ? lookup.getMappings().size() : 0) + " mappings)");
                    }
                } catch (Exception e) {
                    invalidCount++;
                    System.err.println("  ✗ " + excelFile.getName() + " - " + e.getMessage());
                }
            }
        }
        
        if ((jsonFiles == null || jsonFiles.length == 0) && (excelFiles == null || excelFiles.length == 0)) {
            System.err.println("No lookup files (JSON or Excel) found in: " + lookupsDir);
            System.exit(1);
        }
        
        System.out.println();
        System.out.println("Validation Summary:");
        System.out.println("  Total lookups: " + (validCount + invalidCount));
        System.out.println("  Valid: " + validCount);
        System.out.println("  Invalid: " + invalidCount);
        System.out.println("  Total mappings: " + totalMappings);
        
        if (invalidCount > 0) {
            System.exit(1);
        } else {
            System.out.println();
            System.out.println("✓ All lookups are valid!");
        }
    }
    
    /**
     * Validate a single lookup table
     */
    private static boolean validateLookup(CodeLookupTable lookup) {
        StringBuilder errors = new StringBuilder();
        
        // Check ID
        if (lookup.getId() == null || lookup.getId().trim().isEmpty()) {
            errors.append("Lookup ID is required; ");
        } else if (!lookup.getId().matches("^[a-zA-Z0-9_-]+$")) {
            errors.append("Lookup ID must contain only alphanumeric characters, hyphens, and underscores; ");
        }
        
        // Check for at least one mapping
        if (lookup.getMappings() == null || lookup.getMappings().isEmpty()) {
            errors.append("Lookup must have at least one mapping; ");
        }
        
        // Check each mapping
        if (lookup.getMappings() != null) {
            for (int i = 0; i < lookup.getMappings().size(); i++) {
                var mapping = lookup.getMappings().get(i);
                if (mapping.getSourceCode() == null || mapping.getSourceCode().trim().isEmpty()) {
                    errors.append("Mapping " + (i + 1) + ": sourceCode is required; ");
                }
                if (mapping.getTargetCode() == null || mapping.getTargetCode().trim().isEmpty()) {
                    errors.append("Mapping " + (i + 1) + ": targetCode is required; ");
                }
            }
        }
        
        if (errors.length() > 0) {
            System.err.println("  ✗ " + lookup.getId() + " - " + errors.toString());
            return false;
        }
        
        return true;
    }
    
    /**
     * Print help
     */
    private static void printHelp() {
        System.out.println("FHIR Mapper - Multi-Lookup Excel CLI");
        System.out.println();
        System.out.println("Usage:");
        System.out.println("  Excel to JSON files:");
        System.out.println("    java -jar fhir-mapper-cli.jar excel-to-json <workbook.xlsx> <output-dir>");
        System.out.println();
        System.out.println("  JSON files to Excel workbook:");
        System.out.println("    java -jar fhir-mapper-cli.jar json-to-excel <json-dir> <output.xlsx>");
        System.out.println("    java -jar fhir-mapper-cli.jar json-to-excel <file1.json> <file2.json> ... <output.xlsx>");
        System.out.println();
        System.out.println("  Validate lookups:");
        System.out.println("    java -jar fhir-mapper-cli.jar validate <lookups-directory>");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  # Convert Excel workbook to JSON files");
        System.out.println("  java -jar fhir-mapper-cli.jar excel-to-json lookups.xlsx ./lookups/resources");
        System.out.println();
        System.out.println("  # Convert JSON directory to Excel workbook");
        System.out.println("  java -jar fhir-mapper-cli.jar json-to-excel ./lookups/resources lookups.xlsx");
        System.out.println();
        System.out.println("  # Convert specific JSON files to Excel workbook");
        System.out.println("  java -jar fhir-mapper-cli.jar json-to-excel gender.json status.json all-lookups.xlsx");
        System.out.println();
        System.out.println("  # Validate lookups");
        System.out.println("  java -jar fhir-mapper-cli.jar validate ./lookups");
    }
}
