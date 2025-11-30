package io.famly.mapper.core.cli;

import java.io.File;
import java.util.Scanner;

import ca.uhn.fhir.context.FhirContext;
import io.famly.mapper.core.excel.ExcelMappingConverter;
import io.famly.mapper.core.loader.MappingLoader;
import io.famly.mapper.core.validation.ValidationResult;

/**
 * Command-line interface for Excel/JSON conversion
 * 
 * Usage:
 *   java -jar fhir-mapper-cli.jar convert --json-to-excel patient-mapping.json patient-mapping.xlsx
 *   java -jar fhir-mapper-cli.jar convert --excel-to-json patient-mapping.xlsx patient-mapping.json
 *   java -jar fhir-mapper-cli.jar batch --json-to-excel ./mappings/resources ./excel-mappings
 *   java -jar fhir-mapper-cli.jar validate ./mappings
 *   java -jar fhir-mapper-cli.jar interactive
 */
public class ExcelConverterCLI {
    
    private static final ExcelMappingConverter converter = new ExcelMappingConverter();
    
    public static void main(String[] args) {
        if (args.length == 0) {
            printHelp();
            return;
        }
        
        String command = args[0];
        
        try {
            switch (command) {
                case "convert":
                    handleConvert(args);
                    break;
                    
                case "batch":
                    handleBatch(args);
                    break;
                    
                case "validate":
                    handleValidate(args);
                    break;
                    
                case "interactive":
                    handleInteractive();
                    break;
                    
                case "template":
                    handleTemplate(args);
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
     * Handle single file conversion
     */
    private static void handleConvert(String[] args) throws Exception {
        if (args.length < 4) {
            System.err.println("Usage: convert --json-to-excel input.json output.xlsx");
            System.err.println("   or: convert --excel-to-json input.xlsx output.json");
            System.exit(1);
        }
        
        String direction = args[1];
        String inputFile = args[2];
        String outputFile = args[3];
        
        if (!new File(inputFile).exists()) {
            System.err.println("Input file not found: " + inputFile);
            System.exit(1);
        }
        
        System.out.println("Converting: " + inputFile + " -> " + outputFile);
        
        if ("--json-to-excel".equals(direction)) {
            converter.jsonToExcel(inputFile, outputFile);
            System.out.println("✓ Successfully converted JSON to Excel");
        } else if ("--excel-to-json".equals(direction)) {
            converter.excelToJson(inputFile, outputFile);
            System.out.println("✓ Successfully converted Excel to JSON");
        } else {
            System.err.println("Invalid direction: " + direction);
            System.exit(1);
        }
    }
    
    /**
     * Handle batch directory conversion
     */
    private static void handleBatch(String[] args) throws Exception {
        if (args.length < 4) {
            System.err.println("Usage: batch --json-to-excel input-dir output-dir");
            System.err.println("   or: batch --excel-to-json input-dir output-dir");
            System.exit(1);
        }
        
        String direction = args[1];
        String inputDir = args[2];
        String outputDir = args[3];
        
        if (!new File(inputDir).exists()) {
            System.err.println("Input directory not found: " + inputDir);
            System.exit(1);
        }
        
        System.out.println("Batch converting directory: " + inputDir);
        
        if ("--json-to-excel".equals(direction)) {
            converter.convertDirectoryToExcel(inputDir, outputDir);
            System.out.println("✓ Batch conversion completed");
        } else if ("--excel-to-json".equals(direction)) {
            converter.convertDirectoryToJson(inputDir, outputDir);
            System.out.println("✓ Batch conversion completed");
        } else {
            System.err.println("Invalid direction: " + direction);
            System.exit(1);
        }
    }
    
    /**
     * Validate mappings in a directory
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
     * Interactive mode
     */
    private static void handleInteractive() throws Exception {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("=================================");
        System.out.println("FHIR Mapper - Interactive Mode");
        System.out.println("=================================");
        System.out.println();
        
        while (true) {
            System.out.println("Options:");
            System.out.println("  1. Convert JSON to Excel");
            System.out.println("  2. Convert Excel to JSON");
            System.out.println("  3. Validate mappings");
            System.out.println("  4. Create template Excel");
            System.out.println("  5. Handle Batch Conversion");
            System.out.println("  6. Exit");
            System.out.print("\nChoice: ");
            
            int choice = scanner.nextInt();
            scanner.nextLine(); // Consume newline
            
            switch (choice) {
                case 1:
                    System.out.print("Enter JSON file path: ");
                    String jsonInput = scanner.nextLine();
                    System.out.print("Enter Excel output path: ");
                    String excelOutput = scanner.nextLine();
                    converter.jsonToExcel(jsonInput, excelOutput);
                    System.out.println("✓ Conversion complete\n");
                    break;
                    
                case 2:
                    System.out.print("Enter Excel file path: ");
                    String excelInput = scanner.nextLine();
                    System.out.print("Enter JSON output path: ");
                    String jsonOutput = scanner.nextLine();
                    converter.excelToJson(excelInput, jsonOutput);
                    System.out.println("✓ Conversion complete\n");
                    break;
                    
                case 3:
                    System.out.print("Enter mappings directory: ");
                    String mappingsDir = scanner.nextLine();
                    handleValidate(new String[]{"validate", mappingsDir});
                    break;
                    
                case 4:
                    System.out.print("Enter template file path: ");
                    String templatePath = scanner.nextLine();
                    createTemplate(templatePath);
                    System.out.println("✓ Template created\n");
                    break;
                case 5:
                	System.out.print("Enter conversion direction (J2E: --json-to-excel or E2J: --excel-to-json): ");
					String direction = scanner.nextLine();
					if(direction.equals("J2E")) {
						direction="--json-to-excel";
					} else if(direction.equals("E2J")) {
						direction="--excel-to-json";
					} else {
						System.out.println("Invalid direction choice\n");
						break;
					}
					
					System.out.print("Enter input directory: ");
					String inputDir = scanner.nextLine();
					System.out.print("Enter output directory: ");
					String outputDir = scanner.nextLine();
					handleBatch(new String[]{"batch", direction, inputDir, outputDir});
					break;
                case 6:
                    System.out.println("Goodbye!");
                    scanner.close();
                    return;
                    
                default:
                    System.out.println("Invalid choice\n");
            }
        }
    }
    
    /**
     * Create template Excel file
     */
    private static void handleTemplate(String[] args) throws Exception {
        String outputPath = args.length > 1 ? args[1] : "mapping-template.xlsx";
        createTemplate(outputPath);
        System.out.println("✓ Template created: " + outputPath);
    }
    
    private static void createTemplate(String outputPath) throws Exception {
        ExcelTemplateGenerator generator = new ExcelTemplateGenerator();
        generator.generateTemplate(outputPath);
    }
    
    /**
     * Print help message
     */
    private static void printHelp() {
        System.out.println("FHIR Mapper - Excel/JSON Converter");
        System.out.println();
        System.out.println("Usage:");
        System.out.println("  Single file conversion:");
        System.out.println("    java -jar fhir-mapper-cli.jar convert --json-to-excel <input.json> <output.xlsx>");
        System.out.println("    java -jar fhir-mapper-cli.jar convert --excel-to-json <input.xlsx> <output.json>");
        System.out.println();
        System.out.println("  Batch directory conversion:");
        System.out.println("    java -jar fhir-mapper-cli.jar batch --json-to-excel <input-dir> <output-dir>");
        System.out.println("    java -jar fhir-mapper-cli.jar batch --excel-to-json <input-dir> <output-dir>");
        System.out.println();
        System.out.println("  Validation:");
        System.out.println("    java -jar fhir-mapper-cli.jar validate <mappings-directory>");
        System.out.println();
        System.out.println("  Create template:");
        System.out.println("    java -jar fhir-mapper-cli.jar template [output-file.xlsx]");
        System.out.println();
        System.out.println("  Interactive mode:");
        System.out.println("    java -jar fhir-mapper-cli.jar interactive");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  # Convert single file");
        System.out.println("  java -jar fhir-mapper-cli.jar convert --json-to-excel patient.json patient.xlsx");
        System.out.println();
        System.out.println("  # Convert entire directory");
        System.out.println("  java -jar fhir-mapper-cli.jar batch --json-to-excel ./mappings/resources ./excel-mappings");
        System.out.println();
        System.out.println("  # Validate all mappings");
        System.out.println("  java -jar fhir-mapper-cli.jar validate ./mappings");
    }
}