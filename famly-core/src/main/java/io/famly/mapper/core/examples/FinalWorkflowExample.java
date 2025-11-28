package io.famly.mapper.core.examples;

import org.hl7.fhir.r4.model.Patient;

import io.famly.mapper.core.engine.TransformationEngine;
import io.famly.mapper.core.loader.MappingLoader;
import io.famly.mapper.core.model.MappingRegistry;
import io.famly.mapper.core.model.ResourceMapping;
import io.famly.mapper.core.model.TransformationContext;

/**
 * Complete workflow demonstrating all features:
 * 
 * 1. Excel workbook with multiple mappings
 * 2. Auto-loading with JSON file creation
 * 3. ID-based override
 * 4. Validation with duplicate name warnings
 * 5. Usage by ID
 */
public class FinalWorkflowExample {

    public static void main(String[] args) throws Exception {
        
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║  FHIR Mapper - Complete Workflow Example                   ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
        System.out.println();
        
        demonstrateCompleteWorkflow();
    }
    
    private static void demonstrateCompleteWorkflow() throws Exception {
        
        /*
         * SCENARIO:
         * =========
         * 
         * Directory structure BEFORE:
         * /mappings
         *   /resources
         *     all-mappings.xlsx    ← Excel workbook with 3 mappings
         * 
         * Excel workbook "all-mappings.xlsx":
         *   Sheet "Config":
         *     version = 1.0.0
         *   
         *   Sheet "Patient - JSON to FHIR":
         *     ID: patient-json-to-fhir-v1
         *     Direction: JSON_TO_FHIR
         *     Source: PatientDTO
         *     Target: Patient
         *     (10 field mappings)
         *   
         *   Sheet "Encounter - JSON to FHIR":
         *     ID: encounter-json-to-fhir-v1
         *     Direction: JSON_TO_FHIR
         *     Source: EncounterDTO
         *     Target: Encounter
         *     (8 field mappings)
         *   
         *   Sheet "Observation - JSON to FHIR":
         *     ID: observation-json-to-fhir-v1
         *     Direction: JSON_TO_FHIR
         *     Source: ObservationDTO
         *     Target: Observation
         *     (12 field mappings)
         */
        
        // STEP 1: Load mappings
        // Excel will be auto-converted and JSON files created
        System.out.println("STEP 1: Loading mappings from Excel workbook");
        System.out.println("─────────────────────────────────────────────");
        
        MappingLoader loader = new MappingLoader("./mappings");
        MappingRegistry registry = loader.loadAll();
        
        System.out.println();
        
        /*
         * WHAT HAPPENED:
         * ==============
         * 
         * Directory structure AFTER:
         * /mappings
         *   /resources
         *     all-mappings.xlsx                      ← Original Excel
         *     patient-json-to-fhir-v1.json          ← AUTO-CREATED
         *     encounter-json-to-fhir-v1.json        ← AUTO-CREATED
         *     observation-json-to-fhir-v1.json      ← AUTO-CREATED
         * 
         * Console output:
         *   📊 Converting Excel workbook: all-mappings.xlsx...
         *     ✓ patient-json-to-fhir-v1 [JSON_TO_FHIR] (Sheet: Patient - JSON to FHIR)
         *       → Saved to: patient-json-to-fhir-v1.json
         *     ✓ encounter-json-to-fhir-v1 [JSON_TO_FHIR] (Sheet: Encounter - JSON to FHIR)
         *       → Saved to: encounter-json-to-fhir-v1.json
         *     ✓ observation-json-to-fhir-v1 [JSON_TO_FHIR] (Sheet: Observation - JSON to FHIR)
         *       → Saved to: observation-json-to-fhir-v1.json
         */
        
        // STEP 2: Get mapping by ID (always use ID, not name)
        System.out.println("STEP 2: Getting mappings by ID");
        System.out.println("───────────────────────────────");
        
        ResourceMapping patientMapping = registry.findById("patient-json-to-fhir-v1");
        ResourceMapping encounterMapping = registry.findById("encounter-json-to-fhir-v1");
        ResourceMapping observationMapping = registry.findById("observation-json-to-fhir-v1");
        
        System.out.println("✓ Found patient mapping: " + patientMapping.getId());
        System.out.println("✓ Found encounter mapping: " + encounterMapping.getId());
        System.out.println("✓ Found observation mapping: " + observationMapping.getId());
        System.out.println();
        
        // STEP 3: Use mappings for transformation
        System.out.println("STEP 3: Using mappings for transformation");
        System.out.println("─────────────────────────────────────────");
        
        TransformationEngine engine = new TransformationEngine(registry);
        TransformationContext context = createContext();
        
        // Transform patient
        String patientJson = "{\"patientId\":\"P123\",\"firstName\":\"John\",\"lastName\":\"Doe\",\"gender\":\"M\"}";
        Patient patient = engine.jsonToFhirResource(patientJson, patientMapping, context, Patient.class);
        
        System.out.println("✓ Transformed patient: " + patient.getIdentifierFirstRep().getValue());
        System.out.println("  Name: " + patient.getNameFirstRep().getNameAsSingleString());
        System.out.println();
        
        // STEP 4: Demonstrate override behavior
        System.out.println("STEP 4: Demonstrating ID-based override");
        System.out.println("───────────────────────────────────────");
        System.out.println("Scenario: Excel workbook has updated patient mapping with same ID");
        System.out.println();
        
        /*
         * SCENARIO:
         * =========
         * 
         * 1. Business analyst edits all-mappings.xlsx:
         *    - Updates "Patient - JSON to FHIR" sheet
         *    - ID stays the same: patient-json-to-fhir-v1
         *    - Adds new field mappings
         * 
         * 2. Developer loads mappings again:
         *    MappingLoader loader = new MappingLoader("./mappings");
         *    MappingRegistry registry = loader.loadAll();
         * 
         * 3. What happens:
         *    - Excel is converted
         *    - JSON file patient-json-to-fhir-v1.json is OVERRIDDEN
         *    - Console shows: [OVERRIDDEN]
         *    - New mapping replaces old one (by ID)
         * 
         * Console output:
         *   📊 Converting Excel workbook: all-mappings.xlsx...
         *     ✓ patient-json-to-fhir-v1 [JSON_TO_FHIR] (Sheet: Patient - JSON to FHIR) [OVERRIDDEN]
         *       → Saved to: patient-json-to-fhir-v1.json
         */
        
        System.out.println("✓ Mappings with same ID are automatically overridden");
        System.out.println("✓ JSON files are updated with latest Excel content");
        System.out.println();
        
        // STEP 5: Show validation behavior
        System.out.println("STEP 5: Validation behavior");
        System.out.println("───────────────────────────");
        
        /*
         * VALIDATION RULES:
         * =================
         * 
         * 1. Duplicate IDs → ERROR (not allowed)
         *    If two mappings have ID "patient-json-to-fhir-v1":
         *    [ERROR] Duplicate mapping ID 'patient-json-to-fhir-v1' found in 2 mappings
         * 
         * 2. Duplicate Names → WARNING (allowed but warned)
         *    If two mappings have name "Patient Mapping":
         *    [WARN] Duplicate mapping name 'Patient Mapping' found in 2 mappings.
         *           Consider using unique names for better clarity.
         * 
         * 3. Empty cells → No warnings
         *    Blank Excel cells are treated as null, not empty strings
         */
        
        System.out.println("Validation rules:");
        System.out.println("  ✓ Duplicate IDs → ERROR (not allowed)");
        System.out.println("  ✓ Duplicate Names → WARNING (allowed)");
        System.out.println("  ✓ Empty cells → Treated as null (no warnings)");
        System.out.println();
        
        // STEP 6: Show statistics
        System.out.println("STEP 6: Load statistics");
        System.out.println("───────────────────────");
        
        MappingLoader.LoadStatistics stats = loader.getLoadStatistics();
        System.out.println("Total mappings loaded: " + stats.getTotalMappingsLoaded());
        System.out.println("  - JSON files: " + stats.getJsonFilesLoaded());
        System.out.println("  - Excel mappings: " + stats.getExcelFilesLoaded());
        System.out.println("  - Excel workbooks: " + stats.getExcelWorkbooksLoaded());
        System.out.println();
        
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║  Workflow complete!                                        ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
    }
    
    private static TransformationContext createContext() {
        TransformationContext context = new TransformationContext();
        context.setOrganizationId("org-123");
        context.getSettings().put("identifierSystem", "urn:oid:2.16.840.1.113883.4.1");
        return context;
    }
}

/**
 * Summary of Key Features
 */
class FeatureSummary {
    /*
     * KEY FEATURES:
     * =============
     * 
     * 1. EXCEL TO JSON AUTO-CONVERSION
     *    - Excel workbook automatically converted during load
     *    - JSON files created in same directory
     *    - One Excel workbook → Multiple JSON files
     * 
     * 2. ID-BASED OVERRIDE
     *    - Mappings identified by ID (read from meta row in Excel)
     *    - If ID exists, JSON file is overridden
     *    - Same ID in Excel always replaces existing JSON
     * 
     * 3. VALIDATION
     *    - Duplicate IDs → ERROR (prevents conflicts)
     *    - Duplicate Names → WARNING (allowed but warned)
     *    - Empty cells → null (no validation warnings)
     * 
     * 4. LOOKUP BY ID
     *    - Always use: registry.findById("mapping-id")
     *    - ID is stable and unique
     *    - Names can be duplicate (not recommended but allowed)
     * 
     * 5. EXCEL STRUCTURE
     *    Sheet "Config": Common metadata
     *    
     *    Each mapping sheet:
     *      Row 1: ID: patient-json-to-fhir-v1
     *      Row 2: Direction: JSON_TO_FHIR
     *      Row 3: Source Type: PatientDTO
     *      Row 4: Target Type: Patient
     *      Row 5: (blank)
     *      Row 6: Headers (id, sourcePath, targetPath, ...)
     *      Row 7+: Field mappings
     * 
     * 6. WORKFLOW
     *    Business Analyst:
     *      1. Edit Excel workbook
     *      2. Save (keep same IDs for updates)
     *      3. Commit Excel to version control
     *    
     *    Developer:
     *      1. Pull latest Excel
     *      2. Run: MappingLoader loader = new MappingLoader("./mappings");
     *      3. JSON files auto-created/updated
     *      4. Use: ResourceMapping m = registry.findById("mapping-id");
     *      5. Transform: engine.jsonToFhirResource(...)
     * 
     * 7. PRODUCTION DEPLOYMENT
     *    Option A: Deploy Excel + JSON (both)
     *      - Excel keeps being converted on startup
     *      - JSON files always in sync
     *    
     *    Option B: Deploy JSON only
     *      - Disable Excel support: new MappingLoader(..., false)
     *      - Faster startup (no Excel conversion)
     *      - Pre-convert Excel in CI/CD
     */
}

/**
 * Excel Sheet Structure Example
 */
class ExcelSheetStructureExample {
    /*
     * Sheet "Patient - JSON to FHIR":
     * 
     * ┌──────────────┬─────────────────────────────────┐
     * │ ID:          │ patient-json-to-fhir-v1         │ ← Row 1 (read by converter)
     * ├──────────────┼─────────────────────────────────┤
     * │ Direction:   │ JSON_TO_FHIR                    │ ← Row 2
     * ├──────────────┼─────────────────────────────────┤
     * │ Source Type: │ PatientDTO                      │ ← Row 3
     * ├──────────────┼─────────────────────────────────┤
     * │ Target Type: │ Patient                         │ ← Row 4
     * ├──────────────┼─────────────────────────────────┤
     * │              │                                 │ ← Row 5 (blank)
     * ├──────────────┼──────────┬──────────────┬───────┤
     * │ id           │sourcePath│targetPath    │...    │ ← Row 6 (header)
     * ├──────────────┼──────────┼──────────────┼───────┤
     * │ patient-id   │patientId │identifier... │...    │ ← Row 7+ (data)
     * │ patient-name │firstName │name[0].gi... │...    │
     * │ patient-dob  │birthDate │birthDate     │...    │
     * └──────────────┴──────────┴──────────────┴───────┘
     * 
     * IMPORTANT:
     * - ID in row 1 is used to identify mapping
     * - Same ID = override behavior
     * - Empty cells become null (not empty string)
     * - Direction must be exact: JSON_TO_FHIR or FHIR_TO_JSON
     */
}