package com.fhir.mapper.examples;

import java.util.List;

import org.hl7.fhir.r4.model.Patient;

import com.fhir.mapper.engine.TransformationEngine;
import com.fhir.mapper.excel.MultiMappingExcelConverter;
import com.fhir.mapper.loader.MappingLoader;
import com.fhir.mapper.model.MappingRegistry;
import com.fhir.mapper.model.ResourceMapping;
import com.fhir.mapper.model.TransformationContext;

public class MultiMappingExcelExample {

    public static void main(String[] args) throws Exception {
        
        example1_DirectoryStructure();
        example2_AutoLoad();
        example3_DuplicateIdError();
    }

    private static void example1_DirectoryStructure() throws Exception {
        System.out.println("=== Example 1: Directory Structure ===\n");
        
        System.out.println("Expected directory structure:");
        System.out.println("  /mappings/lookups       - Lookup tables");
        System.out.println("  /mappings/json          - Manual JSON mappings");
        System.out.println("  /mappings/excel         - Excel workbooks ONLY");
        System.out.println("  /mappings/excel-generated - Auto-generated (cleaned on load)");
        System.out.println();
    }

    private static void example2_AutoLoad() throws Exception {
        System.out.println("=== Example 2: Auto-Load Process ===\n");
        
        MappingLoader loader = new MappingLoader("./mappings");
        MappingRegistry registry = loader.loadAll();
        
        TransformationEngine engine = new TransformationEngine(registry);
        TransformationContext context = createContext();
        
        ResourceMapping mapping = registry.findById("patient-json-to-fhir-v1");
        
        if (mapping != null) {
            String json = "{\"patientId\":\"P123\",\"firstName\":\"John\",\"lastName\":\"Doe\",\"gender\":\"M\"}";
            Patient patient = engine.jsonToFhirResource(json, mapping, context, Patient.class);
            
            System.out.println("✓ Transformed patient: " + patient.getIdentifierFirstRep().getValue());
        }
        System.out.println();
    }

    private static void example3_DuplicateIdError() throws Exception {
        System.out.println("=== Example 3: Duplicate ID Handling ===\n");
        
        System.out.println("Duplicate ID behavior:");
        System.out.println("  - Same ID in /json and /excel → ERROR");
        System.out.println("  - Same ID in different Excel workbooks → ERROR");
        System.out.println("  - Same ID in different sheets of same workbook → ERROR");
        System.out.println();
        System.out.println("All IDs must be unique across:");
        System.out.println("  - JSON mappings in /json directory");
        System.out.println("  - Excel mappings in /excel directory");
        System.out.println();
    }
    
    private static void exampleJsonToExcel() throws Exception {
        MultiMappingExcelConverter converter = new MultiMappingExcelConverter();
        
        converter.jsonDirectoryToExcel(
            "./mappings/json",
            "./mappings/excel/all.xlsx"
        );
        
        System.out.println("✓ Created Excel workbook from JSON directory");
    }
    
    private static void exampleExcelToJson() throws Exception {
        MultiMappingExcelConverter converter = new MultiMappingExcelConverter();
        
        List<String> createdFiles = converter.excelToJsonFiles(
            "./mappings/excel/all-mappings.xlsx",
            "./mappings/json"
        );
        
        System.out.println("✓ Created " + createdFiles.size() + " JSON files from Excel");
    }
    
    private static TransformationContext createContext() {
        TransformationContext context = new TransformationContext();
        context.setOrganizationId("org-123");
        context.getSettings().put("identifierSystem", "urn:oid:2.16.840.1.113883.4.1");
        return context;
    }
}