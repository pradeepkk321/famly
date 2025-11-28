package com.fhir.mapper.cli;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import com.fhir.mapper.excel.MultiMappingExcelConverter;
import com.fhir.mapper.loader.MappingLoader;
import com.fhir.mapper.validation.ValidationResult;

import ca.uhn.fhir.context.FhirContext;

/**
 * CLI for multi-mapping Excel workbooks
 * 
 * Commands:
 *   excel-to-json    Convert Excel workbook (multiple mappings) to JSON files
 *   json-to-excel    Convert JSON files to single Excel workbook
 *   validate         Validate mappings
 */
public class MultiMappingCLI {
    
    private static final MultiMappingExcelConverter converter = new MultiMappingExcelConverter();
    
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
        
        List<String> createdFiles = converter.excelToJsonFiles(inputFile, outputDir);
        
        System.out.println();
        System.out.println("✓ Successfully converted " + createdFiles.size() + " mappings");
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
            
            converter.jsonDirectoryToExcel(jsonDir, outputFile);
            
        } else {
            // Multiple files mode
            List<String> jsonFiles = Arrays.asList(args).subList(1, args.length - 1);
            String outputFile = lastArg;
            
            System.out.println("Converting " + jsonFiles.size() + " JSON files to Excel workbook...");
            System.out.println("Output: " + outputFile);
            System.out.println();
            
            converter.jsonFilesToExcel(jsonFiles, outputFile);
        }
        
        System.out.println("✓ Successfully created Excel workbook");
    }
    
    /**
     * Validate mappings
     */
    private static void handleValidate(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: validate <mappings-directory>");
            System.exit(1);
        }
        
        String mappingsDir = args[1];
        
        if (!new File(mappingsDir).exists()) {
            System.err.println("Mappings directory not found: " + mappingsDir);
            System.exit(1);
        }
        
        System.out.println("Validating mappings in: " + mappingsDir);
        System.out.println();
        
        MappingLoader loader = new MappingLoader(mappingsDir, false, FhirContext.forR4());
        ValidationResult result = loader.validateOnly();
        
        if (result.isValid()) {
            System.out.println("✓ All mappings are valid!");
            result.printWarnings();
        } else {
            System.err.println("✗ Validation failed:");
            System.err.println();
            
            for (var error : result.getErrors()) {
                System.err.println("  [ERROR] " + error.getContext());
                System.err.println("          " + error.getMessage());
                System.err.println();
            }
            
            result.printWarnings();
            System.exit(1);
        }
    }
    
    /**
     * Print help
     */
    private static void printHelp() {
        System.out.println("FHIR Mapper - Multi-Mapping Excel CLI");
        System.out.println();
        System.out.println("Usage:");
        System.out.println("  Excel to JSON files:");
        System.out.println("    java -jar fhir-mapper-cli.jar excel-to-json <workbook.xlsx> <output-dir>");
        System.out.println();
        System.out.println("  JSON files to Excel workbook:");
        System.out.println("    java -jar fhir-mapper-cli.jar json-to-excel <json-dir> <output.xlsx>");
        System.out.println("    java -jar fhir-mapper-cli.jar json-to-excel <file1.json> <file2.json> ... <output.xlsx>");
        System.out.println();
        System.out.println("  Validate mappings:");
        System.out.println("    java -jar fhir-mapper-cli.jar validate <mappings-directory>");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  # Convert Excel workbook to JSON files");
        System.out.println("  java -jar fhir-mapper-cli.jar excel-to-json mappings.xlsx ./mappings/resources");
        System.out.println();
        System.out.println("  # Convert JSON directory to Excel workbook");
        System.out.println("  java -jar fhir-mapper-cli.jar json-to-excel ./mappings/resources mappings.xlsx");
        System.out.println();
        System.out.println("  # Convert specific JSON files to Excel workbook");
        System.out.println("  java -jar fhir-mapper-cli.jar json-to-excel patient.json encounter.json all-mappings.xlsx");
        System.out.println();
        System.out.println("  # Validate mappings");
        System.out.println("  java -jar fhir-mapper-cli.jar validate ./mappings");
    }
}