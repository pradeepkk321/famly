# FHIR Mapper Core

A declarative, Excel/JSON-driven transformation framework for converting between custom JSON/POJO formats and HL7 FHIR resources.

[![Maven](https://img.shields.io/badge/Maven-3.8+-blue.svg)](https://maven.apache.org/)
[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![HAPI FHIR](https://img.shields.io/badge/HAPI%20FHIR-6.10.0-green.svg)](https://hapifhir.io/)

## Overview

FHIR Mapper Core eliminates the need to write Java transformation code for each FHIR resource mapping. Instead, you define mappings in JSON or Excel configuration files with support for:

- **Bidirectional transformations** (JSON ↔ FHIR)
- **Excel-based configuration** for business users (mappings and lookup tables)
- **Expression-based transformations** using JEXL
- **Code lookups** for terminology mapping with multi-system support
- **Conditional field mapping** with context variables
- **Validation** against HAPI FHIR structure definitions
- **Security scanning** to prevent malicious expressions

## Quick Start

### Maven Dependency

```xml
<dependency>
    <groupId>com.fhir.mapper</groupId>
    <artifactId>fhir-mapper-core</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Basic Usage

```java
// 1. Load mappings from filesystem
MappingLoader loader = new MappingLoader("./mappings");
MappingRegistry registry = loader.loadAll();

// 2. Create transformation engine
TransformationEngine engine = new TransformationEngine(registry);

// 3. Setup context
TransformationContext context = new TransformationContext();
context.setOrganizationId("org-123");
context.getSettings().put("identifierSystem", "urn:oid:2.16.840.1.113883.4.1");

// 4. Get mapping
ResourceMapping mapping = registry.findById("patient-json-to-fhir-v1");

// 5. Transform!
String jsonInput = "{\"patientId\":\"P123\",\"firstName\":\"John\",\"lastName\":\"Doe\",\"gender\":\"M\"}";
Patient patient = engine.jsonToFhirResource(jsonInput, mapping, context, Patient.class);
```

## Excel Support

Business users can manage mappings and lookup tables using Excel without writing JSON.

### Multiple Workbooks Supported

**Use Case:** Different data sources, departments, or projects can have separate workbooks:
- `epic-mappings.xlsx` - Epic EMR mappings
- `cerner-mappings.xlsx` - Cerner EMR mappings
- `terminology.xlsx` - Standard terminologies
- `custom-codes.xlsx` - Organization-specific codes

All workbooks in the directory are automatically loaded.

### Mapping Workbooks
- Multiple workbooks supported
- Each workbook = Multiple resource mappings
- Each sheet = One mapping (e.g., "Patient - JSON to FHIR")
- Auto-converts to JSON on load

### Lookup Workbooks
- Multiple workbooks supported
- Each workbook = Multiple lookup tables
- Each sheet = One lookup table
- Supports per-mapping target systems

**Directory Structure:**
```
mappings/
├── lookups/              # JSON lookups (legacy)
├── lookups-excel/        # Excel lookup workbooks
│   ├── standard-terminology.xlsx   # FHIR standard codes
│   ├── epic-codes.xlsx             # Epic-specific codes
│   └── custom-codes.xlsx           # Organization codes
├── json/                 # Manual JSON mappings
├── excel/                # Excel mapping workbooks
│   ├── epic-mappings.xlsx          # Epic EMR mappings
│   ├── cerner-mappings.xlsx        # Cerner EMR mappings
│   └── lab-mappings.xlsx           # Lab system mappings
└── excel-generated/      # Auto-generated (cleaned on load)
```

**Excel Lookup Format:**
```
Sheet: "gender-lookup"

Row 1: ID:                     | gender-lookup
Row 2: Name:                   | Gender Code Mapping
Row 3: Source System:          | internal
Row 4: Default Target System:  | http://hl7.org/fhir/administrative-gender
Row 5: Bidirectional:          | false
Row 6: (blank)
Row 7: sourceCode | targetCode | targetSystem                              | display
Row 8: M          | male       |                                           | Male
Row 9: F          | female     |                                           | Female
Row 10: O         | other      | http://custom.org/special-gender-system   | Other
```

Note: `targetSystem` in row 8+ is optional. If blank, uses `Default Target System` from row 4.

**Excel Mapping Format:**
```
Sheet: "Patient - JSON to FHIR"

Row 1: ID:            | patient-json-to-fhir-v1
Row 2: Direction:     | JSON_TO_FHIR
Row 3: Source Type:   | PatientDTO
Row 4: Target Type:   | Patient
Row 5: (blank)
Row 6: id           | sourcePath | targetPath           | dataType | transformExpression        | condition              | required | defaultValue              | lookupTable    | description
Row 7: patient-id   | patientId  | identifier[0].value  | string   |                            |                        | TRUE     |                           |                | Patient MRN
Row 8: patient-sys  |            | identifier[0].system | uri      |                            |                        | TRUE     | $ctx.settings['mrnSystem']|                | MRN system
Row 9: patient-name | firstName  | name[0].given[0]     | string   |                            |                        | TRUE     |                           |                | First name
Row 10: patient-ssn | ssn        | identifier[1].value  | string   | fn.replace(value, '-', '') | ssn != null            | FALSE    |                           |                | SSN no dashes
Row 11: patient-gen | gender     | gender               | code     |                            |                        | TRUE     |                           | gender-lookup  | Gender code
```

**Multiple sheets = Multiple mappings** in one workbook.

**All Excel files are scanned:**
- `mappings/excel/*.xlsx` → Resource mappings
- `mappings/lookups-excel/*.xlsx` → Lookup tables
- IDs must be unique across **all** workbooks

## Features

### 1. Multiple Input/Output Formats

The engine supports 12 transformation methods covering all combinations:

**JSON → FHIR**
```java
// String to Map
Map<String, Object> fhirMap = engine.jsonToFhirMap(jsonString, mapping, context);

// String to HAPI Resource
Patient patient = engine.jsonToFhirResource(jsonString, mapping, context, Patient.class);

// POJO to HAPI Resource
PatientDTO dto = new PatientDTO();
Patient patient = engine.jsonToFhirResource(dto, mapping, context, Patient.class);

// Map to FHIR JSON String
String fhirJson = engine.jsonToFhirJson(dataMap, mapping, context);
```

**FHIR → JSON**
```java
// FHIR JSON to Map
Map<String, Object> jsonMap = engine.fhirToJsonMap(fhirJson, mapping, context);

// HAPI Resource to POJO
PatientDTO dto = engine.fhirToJsonObject(patient, mapping, context, PatientDTO.class);

// FHIR Map to JSON String
String json = engine.fhirToJsonString(fhirMap, mapping, context);
```

### 2. Declarative Mapping Configuration

Define mappings in JSON files instead of writing Java code:

```json
{
  "id": "patient-json-to-fhir-v1",
  "name": "Patient JSON to FHIR Mapping",
  "direction": "JSON_TO_FHIR",
  "sourceType": "PatientDTO",
  "targetType": "Patient",
  "fieldMappings": [
    {
      "id": "patient-identifier",
      "sourcePath": "patientId",
      "targetPath": "identifier[0].value",
      "dataType": "string",
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
```

### 3. Expression Language (JEXL)

Transform data using JEXL expressions with built-in functions:

```json
{
  "sourcePath": "ssn",
  "targetPath": "identifier[1].value",
  "transformExpression": "fn.replace(value, '-', '')",
  "condition": "ssn != null && ssn != ''"
}
```

**Built-in Functions:**

| Function | Description | Example |
|----------|-------------|---------|
| `fn.uppercase(str)` | Convert to uppercase | `"JOHN"` |
| `fn.lowercase(str)` | Convert to lowercase | `"john"` |
| `fn.replace(str, old, new)` | Replace substring | `fn.replace(value, '-', '')` |
| `fn.concat(str...)` | Concatenate strings | `fn.concat('Organization/', orgId)` |
| `fn.formatDate(date, pattern)` | Format date | `fn.formatDate(value, 'yyyy-MM-dd')` |
| `fn.toInt(value)` | Convert to integer | `fn.toInt("42")` |
| `fn.toBoolean(value)` | Convert to boolean | `fn.toBoolean("true")` |
| `fn.defaultIfNull(value, default)` | Fallback value | `fn.defaultIfNull(value, 'Unknown')` |

### 4. Context Variables

Access runtime context in expressions and default values:

```json
{
  "targetPath": "identifier[0].system",
  "defaultValue": "$ctx.settings['identifierSystem']"
},
{
  "targetPath": "managingOrganization.reference",
  "transformExpression": "fn.concat('Organization/', ctx.organizationId)",
  "condition": "ctx.organizationId != null"
}
```

**Context Object:**
```java
TransformationContext context = new TransformationContext();

// Built-in properties
context.setOrganizationId("org-123");
context.setFacilityId("facility-456");
context.setTenantId("tenant-789");

// Settings map
context.getSettings().put("identifierSystem", "urn:oid:2.16.840.1.113883.4.1");

// Custom variables
context.setVariable("customKey", "customValue");
```

### 5. Code Lookup Tables

Map between code systems using lookup tables:

**Definition:** `mappings/lookups/gender-lookup.json`
```json
{
  "id": "gender-lookup",
  "name": "Gender Code Mapping",
  "sourceSystem": "internal",
  "targetSystem": "http://hl7.org/fhir/administrative-gender",
  "bidirectional": false,
  "mappings": [
    {"sourceCode": "M", "targetCode": "male", "display": "Male"},
    {"sourceCode": "F", "targetCode": "female", "display": "Female"},
    {"sourceCode": "O", "targetCode": "other", "display": "Other"},
    {"sourceCode": "U", "targetCode": "unknown", "display": "Unknown"}
  ]
}
```

**Usage:**
```json
{
  "sourcePath": "gender",
  "targetPath": "gender",
  "lookupTable": "gender-lookup"
}
```

### 6. Conditional Mapping

Map fields based on conditions:

```json
{
  "id": "patient-ssn",
  "sourcePath": "ssn",
  "targetPath": "identifier[1].value",
  "condition": "ssn != null && ssn != ''",
  "transformExpression": "fn.replace(value, '-', '')"
}
```

### 7. Validation

**Startup Validation:**
- FHIR resource types exist
- FHIR paths are valid (using HAPI structure definitions)
- Data types match expected FHIR types
- Expressions are syntactically correct
- Lookup tables are defined

**Security Validation:**
- Scans for dangerous patterns (System calls, reflection, file I/O)
- Prevents malicious code execution
- Always fails on CRITICAL security issues

```java
MappingLoader loader = new MappingLoader("./mappings", true); // strict=true
MappingRegistry registry = loader.loadAll(); // Throws if validation fails
```
**Console Log: Load Mappings**
```
╔════════════════════════════════════════════════════════════╗
║  FHIR Mapper - Loading Mappings                            ║
╚════════════════════════════════════════════════════════════╝

Base path: ./mappings
FHIR Version: 4.0.1
Excel Support: Enabled

Cleaning up excel-generated directory...
✓ Cleaned up excel-generated directory

Loading lookup tables...
  ✓ address-use-lookup (JSON: address-use-lookup.json)
  ✓ birth-sex-lookup (JSON: birth-sex-lookup.json)
  ✓ ethnicity-text-lookup (JSON: ethnicity-text-lookup.json)
  ✓ gender-lookup-reverse (JSON: gender-lookup-reverse.json)
  ✓ gender-lookup (JSON: gender-lookup.json)
  ✓ language-lookup (JSON: language-lookup.json)
  ✓ marital-status-lookup (JSON: marital-status-lookup.json)
  ✓ race-text-lookup (JSON: race-text-lookup.json)
  ✓ telecom-use-lookup (JSON: telecom-use-lookup.json)
ERROR StatusLogger Log4j2 could not find a logging implementation. Please add log4j-core to the classpath. Using SimpleLogger to log to the console...
  ✓ address-use-lookup (Excel: all-lookups.xlsx)
  ✓ birth-sex-lookup (Excel: all-lookups.xlsx)
  ✓ ethnicity-text-lookup (Excel: all-lookups.xlsx)
  ✓ gender-lookup-reverse (Excel: all-lookups.xlsx)
  ✓ gender-lookup (Excel: all-lookups.xlsx)
  ✓ language-lookup (Excel: all-lookups.xlsx)
  ✓ marital-status-lookup (Excel: all-lookups.xlsx)
  ✓ race-text-lookup (Excel: all-lookups.xlsx)
  ✓ telecom-use-lookup (Excel: all-lookups.xlsx)
✓ Loaded 9 lookup tables

Loading resource mappings...
  ✓ patient-complex-json-to-fhir-v1 [JSON_TO_FHIR] (JSON: patient-complex-json-to-fhir.json)
  📊 Converting Excel workbook: all-mappings.xlsx...
    ✓ complex-patient-v1 [JSON_TO_FHIR] (Sheet: Patient-V1)
      → Generated: excel-generated/complex-patient-v1.json
    ✓ complex-patient-v2 [JSON_TO_FHIR] (Sheet: Patient-V2)
      → Generated: excel-generated/complex-patient-v2.json
    ✓ patient-fhir-to-json [FHIR_TO_JSON] (Sheet: Patient FHIR to JSON Mapping)
      → Generated: excel-generated/patient-fhir-to-json.json
    ✓ patient-json-to-fhir [JSON_TO_FHIR] (Sheet: Patient JSON to FHIR Mapping)
      → Generated: excel-generated/patient-json-to-fhir.json
✓ Loaded 5 resource mappings
  - 1 from JSON directory
  - 4 from 1 Excel workbook(s)

Validating mappings using HAPI FHIR structure definitions...
Validation warnings:
  [WARN] Mapping: patient-complex-json-to-fhir-v1, Field: patient-managing-org: Field has neither sourcePath nor defaultValue
  [WARN] Mapping: complex-patient-v1, Field: patient-managing-org: Field has neither sourcePath nor defaultValue
  [WARN] Mapping: complex-patient-v2, Field: patient-managing-org: Field has neither sourcePath nor defaultValue
  [WARN] Mapping: patient-json-to-fhir, Field: patient-managing-org: Field has neither sourcePath nor defaultValue
Running security validation...
✓ Security validation passed - no issues found

╔════════════════════════════════════════════════════════════╗
║  Mapping registry loaded successfully                      ║
╚════════════════════════════════════════════════════════════╝
```

### 8. Transformation Tracing

Optionally logs a lightweight trace of the transformation process, helping you understand how each field was mapped. When enabled, the engine generates a small JSON summary containing the trace ID, mapping used, overall status, and basic per-field results. More details are available in the [Transformation Tracing](https://github.com/pradeepkk321/fhir-mapper/wiki/Transformation-Tracing) section.

**Enable:**
```java
context.enableTracing();
```

**Use:**
```
TransformationTrace trace = context.getTrace();
System.out.println(trace.toString()); // JSON summary
```

**Example Output (trimmed):**
```
{
  "traceId": "abc-123",
  "mappingId": "patient-json-to-fhir",
  "success": true,
  "fieldTransformationTraces": [
    { "fieldId": "patient-id", "resultValue": "P123" }
  ]
}
```

## Project Structure

```
your-project/
├── mappings/
│   ├── lookups/
│   │   ├── gender-lookup.json
│   │   ├── marital-status-lookup.json
│   │   └── address-use-lookup.json
│   └── resources/
│       ├── patient-json-to-fhir.json
│       ├── patient-fhir-to-json.json
│       ├── encounter-json-to-fhir.json
│       └── observation-json-to-fhir.json
└── src/
    └── main/
        └── java/
            └── com/example/
                └── FhirTransformService.java
```

## Configuration Reference

### ResourceMapping

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | string | Yes | Unique mapping identifier |
| `name` | string | No | Human-readable name |
| `version` | string | No | Mapping version |
| `direction` | enum | Yes | `JSON_TO_FHIR` or `FHIR_TO_JSON` |
| `sourceType` | string | Yes | Source type name (DTO or FHIR resource) |
| `targetType` | string | Yes | Target type name (FHIR resource or DTO) |
| `fieldMappings` | array | Yes | Array of field mappings |

### FieldMapping

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | string | Yes | Unique field identifier |
| `sourcePath` | string | No* | Path in source object (e.g., `patient.name`) |
| `targetPath` | string | Yes | Path in target object (e.g., `name[0].given[0]`) |
| `dataType` | string | No | FHIR data type (string, integer, date, boolean, code) |
| `transformExpression` | string | No | JEXL expression to transform value |
| `condition` | string | No | JEXL expression (must evaluate to boolean) |
| `validator` | string | No | Validation rule (e.g., `notEmpty()`, `regex('^\\d{9}$')`) |
| `required` | boolean | No | If true, transformation fails if value is null |
| `defaultValue` | string | No | Default/constant value (supports `$ctx.*` variables) |
| `lookupTable` | string | No | Reference to code lookup table ID |
| `description` | string | No | Documentation |

*Note: `sourcePath` can be null if `defaultValue` is provided

### CodeLookupTable

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | string | Yes | Unique lookup identifier |
| `name` | string | No | Human-readable name |
| `sourceSystem` | string | No | Source coding system |
| `defaultTargetSystem` | string | No | Default target system (used when mapping doesn't specify) |
| `bidirectional` | boolean | No | Allow reverse lookups (default: false) |
| `defaultSourceCode` | string | No | Fallback for reverse lookup |
| `defaultTargetCode` | string | No | Fallback for forward lookup |
| `mappings` | array | Yes | Array of code mappings |

### CodeMapping (per-row in lookup table)

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `sourceCode` | string | Yes | Source code value |
| `targetCode` | string | Yes | Target code value |
| `targetSystem` | string | No | **Per-mapping target system** (overrides defaultTargetSystem) |
| `display` | string | No | Human-readable display text |

**Multi-System Example:**
```json
{
  "id": "diagnosis-lookup",
  "sourceSystem": "internal",
  "defaultTargetSystem": "http://snomed.info/sct",
  "mappings": [
    {
      "sourceCode": "DIAB",
      "targetCode": "73211009",
      "targetSystem": null,
      "display": "Diabetes mellitus"
    },
    {
      "sourceCode": "CVD",
      "targetCode": "I10",
      "targetSystem": "http://hl7.org/fhir/sid/icd-10",
      "display": "Hypertension"
    }
  ]
}
```

## REST API Integration Example

```java
@RestController
@RequestMapping("/api/fhir")
public class FhirTransformController {
    
    @Autowired private TransformationEngine engine;
    @Autowired private MappingRegistry registry;
    
    @PostMapping("/patient")
    public Patient createPatient(@RequestBody PatientDTO dto) throws Exception {
        TransformationContext ctx = buildContext();
        ResourceMapping mapping = registry.findBySourceAndDirection(
            "PatientDTO", MappingDirection.JSON_TO_FHIR
        );
        return engine.jsonToFhirResource(dto, mapping, ctx, Patient.class);
    }
    
    @GetMapping("/patient/{id}")
    public PatientDTO getPatient(@PathVariable String id) throws Exception {
        // Fetch from FHIR server
        Patient fhirPatient = fhirClient.read()
            .resource(Patient.class)
            .withId(id)
            .execute();
        
        TransformationContext ctx = buildContext();
        ResourceMapping mapping = registry.findBySourceAndDirection(
            "Patient", MappingDirection.FHIR_TO_JSON
        );
        return engine.fhirToJsonObject(fhirPatient, mapping, ctx, PatientDTO.class);
    }
    
    private TransformationContext buildContext() {
        TransformationContext ctx = new TransformationContext();
        ctx.setOrganizationId(getCurrentOrganizationId());
        ctx.getSettings().put("identifierSystem", getIdentifierSystem());
        return ctx;
    }
}
```
## Transformation Tracing

Track field-level transformation details for debugging and monitoring.

### Enable Tracing
```java
TransformationContext context = new TransformationContext();
context.setOrganizationId("org-123");

// Enable tracing with random UUID
context.enableTracing();

// OR with custom trace ID
context.enableTracing("trace-12345");
```

### Basic Usage
```java
ResourceMapping mapping = registry.findById("patient-json-to-fhir-v1");

try {
    Patient patient = engine.jsonToFhirResource(json, mapping, context, Patient.class);
    
    if (context.isEnableTracing()) {
        TransformationTrace trace = context.getTrace();
        
        // Print summary report
        trace.printTraceReport();
        
        // Get failed fields
        List<FieldTransformationTrace> failures = trace.failedFieldTransformationTraces();
        
        // Export as JSON
        System.out.println(trace.toString());
    }
    
} catch (Exception e) {
    // Trace available even on exception
    if (context.isEnableTracing()) {
        context.getTrace().printTraceReport();
    }
    throw e;
}
```

### Trace Report Output
```
=== Transformation Trace Report ===
Trace ID: abc-123-def-456
Mapping: PatientDTO → Patient (patient-json-to-fhir-v1)
Status: SUCCESS
Duration: 45ms
Total fields: 25
Successful: 23
Failed: 2

Failures:
  [patient-ssn] ssn
    Error: Required field is null after all transformations
    Source value: null

  [patient-middle-name] middleName
    Error: Transform failed: Expression evaluation error
    Expression: fn.uppercase(value)
    Source value: null
```

### JSON Trace Export
```java
// Get JSON representation
String traceJson = trace.toString();

// Save to file for analysis
String filename = "traces/" + trace.getTraceId() + ".json";
Files.write(Paths.get(filename), traceJson.getBytes());
```

**JSON Structure:**
```json
{
  "traceId": "98c15d0e-6508-4e3b-ab46-5f4e0b1256ab",
  "source": "ComplexPatientDTO",
  "target": "Patient",
  "mappingId": "complex-patient-v1",
  "success": true,
  "errorMessage": "null",
  "startTime": 1764175004539,
  "endTime": 1764175004623,
  "duration": "84 millis",
  "fieldTransformationTraces": [
    {
      "fieldId": "patient-mrn",
      "sourcePath": "patientId",
      "targetPath": "identifier[0].value",
      "sourceValue": "MRN-12345678",
      "resultValue": "MRN-12345678",
      "expression": "null",
      "condition": "null",
      "conditionPassed": false,
      "errorMessage": "null",
      "startTime": 1764175004548,
      "endTime": 1764175004548,
      "duration": "0 millis"
    },
    {
      "fieldId": "patient-mrn-system",
      "sourcePath": "null",
      "targetPath": "identifier[0].system",
      "sourceValue": "null",
      "resultValue": "urn:oid:2.16.840.1.113883.4.1",
      "expression": "null",
      "condition": "null",
      "conditionPassed": false,
      "errorMessage": "null",
      "startTime": 1764175004548,
      "endTime": 1764175004548,
      "duration": "0 millis"
    },
    {
      "fieldId": "patient-mrn-type-code",
      "sourcePath": "null",
      "targetPath": "identifier[0].type.coding[0].code",
      "sourceValue": "null",
      "resultValue": "MR",
      "expression": "null",
      "condition": "null",
      "conditionPassed": false,
      "errorMessage": "null",
      "startTime": 1764175004548,
      "endTime": 1764175004548,
      "duration": "0 millis"
    },
    {
      "fieldId": "patient-mrn-type-system",
      "sourcePath": "null",
      "targetPath": "identifier[0].type.coding[0].system",
      "sourceValue": "null",
      "resultValue": "http://terminology.hl7.org/CodeSystem/v2-0203",
      "expression": "null",
      "condition": "null",
      "conditionPassed": false,
      "errorMessage": "null",
      "startTime": 1764175004548,
      "endTime": 1764175004548,
      "duration": "0 millis"
    },
    {
      "fieldId": "patient-ssn",
      "sourcePath": "ssn",
      "targetPath": "identifier[1].value",
      "sourceValue": "123-45-6789",
      "resultValue": "123456789",
      "expression": "fn:replace(value, '-', '')",
      "condition": "ssn != null && ssn != ''",
      "conditionPassed": true,
      "errorMessage": "null",
      "startTime": 1764175004548,
      "endTime": 1764175004568,
      "duration": "20 millis"
    },
    {...},
    {...},
    .
    .
    .
  ]
}
```

### Analyze Specific Issues
```java
TransformationTrace trace = context.getTrace();

// Find null resolutions
List<FieldTransformationTrace> nullFields = trace.getFieldTransformationTraces().stream()
    .filter(f -> f.getSourceValue() == null)
    .collect(Collectors.toList());

// Find slow transformations (>10ms)
List<FieldTransformationTrace> slowFields = trace.getFieldTransformationTraces().stream()
    .filter(f -> f.getDuration() > 10)
    .collect(Collectors.toList());

// Find failed conditions
List<FieldTransformationTrace> failedConditions = trace.getFieldTransformationTraces().stream()
    .filter(f -> f.getCondition() != null && !f.isConditionPassed())
    .collect(Collectors.toList());

// Find transformation errors
List<FieldTransformationTrace> transformErrors = trace.getFieldTransformationTraces().stream()
    .filter(f -> f.getExpression() != null && f.getErrorMessage() != null)
    .collect(Collectors.toList());
```

### Production Usage

Tracing adds ~5-10% overhead. Enable selectively:
```java
// Only for specific patients
if (patientId.equals("debug-patient-123")) {
    context.enableTracing(patientId);
}

// Only in development
if (environment.equals("dev") || environment.equals("staging")) {
    context.enableTracing();
}

// For error investigation
try {
    Patient patient = engine.jsonToFhirResource(json, mapping, context, Patient.class);
} catch (TransformationException e) {
    // Enable tracing and retry for debugging
    context.enableTracing("error-investigation-" + System.currentTimeMillis());
    Patient patient = engine.jsonToFhirResource(json, mapping, context, Patient.class);
    context.getTrace().printTraceReport();
}
```

### REST API Integration
```java
@PostMapping("/transform/patient")
public ResponseEntity<PatientResponse> transform(
        @RequestBody PatientDTO dto,
        @RequestHeader(value = "X-Enable-Trace", required = false) String enableTrace) {
    
    TransformationContext context = buildContext();
    
    // Enable tracing if requested
    if ("true".equals(enableTrace)) {
        String traceId = UUID.randomUUID().toString();
        context.enableTracing(traceId);
    }
    
    Patient patient = engine.jsonToFhirResource(dto, mapping, context, Patient.class);
    
    PatientResponse response = new PatientResponse(patient);
    
    // Include trace in response if enabled
    if (context.isEnableTracing()) {
        response.setTrace(context.getTrace());
    }
    
    return ResponseEntity.ok(response);
}
```

### Field-Level Details

Access individual field transformation details:
```java
for (FieldTransformationTrace field : trace.getFieldTransformationTraces()) {
    System.out.println("Field: " + field.getFieldId());
    System.out.println("  Source path: " + field.getSourcePath());
    System.out.println("  Target path: " + field.getTargetPath());
    System.out.println("  Source value: " + field.getSourceValue());
    System.out.println("  Result value: " + field.getResultValue());
    System.out.println("  Duration: " + field.getDuration() + "ms");
    
    if (field.getExpression() != null) {
        System.out.println("  Expression: " + field.getExpression());
    }
    
    if (field.getCondition() != null) {
        System.out.println("  Condition: " + field.getCondition());
        System.out.println("  Condition passed: " + field.isConditionPassed());
    }
    
    if (field.getErrorMessage() != null) {
        System.out.println("  ERROR: " + field.getErrorMessage());
    }
}
```

## Complex Mapping Example

See the complete example in `ComplexRealTimeExample.java` which demonstrates:

- Multiple identifiers (MRN, SSN)
- Name with suffix
- Multiple addresses and contacts
- Race/ethnicity extensions (US Core)
- Birth sex extension
- Marital status with code system
- Preferred language
- Managing organization reference

The mapping configuration shows how to handle:
- Array indexing (`addresses[0]`, `name[0].given[1]`)
- Conditional fields (`condition: "ssn != null && ssn != ''"`)
- Extensions with nested structure
- Code lookups for standardization
- Context-based default values

## Validation & Security

### Validation Modes

```java
// Strict mode (default) - throws on validation errors
MappingLoader loader = new MappingLoader("./mappings", true);

// Lenient mode - logs errors but continues
MappingLoader loader = new MappingLoader("./mappings", false);
```

### Security Scanning

The framework automatically scans expressions for dangerous patterns:

**CRITICAL Issues** (always fails):
- Runtime execution (`Runtime.getRuntime()`)
- Process creation (`ProcessBuilder`)
- Script engines (`ScriptEngine`)

**HIGH Issues** (logged):
- Reflection (`Class.forName`, `Method.invoke`)
- Network I/O (`Socket`, `URLConnection`)
- Database access (`java.sql.*`)

**MEDIUM/LOW Issues** (warnings):
- File I/O (`File`, `FileInputStream`)
- Threading (`new Thread()`)


## Advanced Usage

### Batch Processing

```java
List<PatientDTO> patients = loadPatients();
List<Patient> fhirPatients = new ArrayList<>();

ResourceMapping mapping = registry.findBySourceAndDirection(
    "PatientDTO", MappingDirection.JSON_TO_FHIR
);

for (PatientDTO dto : patients) {
    try {
        Patient patient = engine.jsonToFhirResource(
            dto, mapping, context, Patient.class
        );
        fhirPatients.add(patient);
    } catch (Exception e) {
        logger.error("Failed to transform patient {}: {}", 
            dto.getPatientId(), e.getMessage());
    }
}
```

### Hot Reload

```java
// Initial load
MappingRegistry registry = loader.loadAll();

// Later, reload without restarting
loader.reload(registry);
```

### Custom FHIR Version

```java
// Use R5 instead of R4
FhirContext fhirContext = FhirContext.forR5();
MappingLoader loader = new MappingLoader("./mappings", true, fhirContext);
TransformationEngine engine = new TransformationEngine(registry, fhirContext);
```

### Dry-Run Validation

```java
// Validate mappings without loading
ValidationResult result = loader.validateOnly();

if (!result.isValid()) {
    for (ValidationError error : result.getErrors()) {
        System.err.println(error.getContext() + ": " + error.getMessage());
    }
}
```

## Limitations

### Current Limitations

1. **Nested Path Validation**: Only first-level fields are validated against FHIR structure definitions
2. **Array Iteration**: Cannot map "all elements" in an array automatically
3. **FHIRPath Queries**: Does not support FHIRPath expressions like `name.where(use='official').given[0]`
4. **Lookup Tables**: Only simple code-to-code mappings (no conditional or temporal lookups)
5. **Circular References**: Not detected or handled

### Workarounds

**Array Mapping**: Specify each index explicitly
```json
{"sourcePath": "addresses[0]", "targetPath": "address[0]"},
{"sourcePath": "addresses[1]", "targetPath": "address[1]"}
```

**Complex Queries**: Use JEXL expressions in transformations
```json
{
  "transformExpression": "addresses.stream().filter(a -> a.type == 'HOME').findFirst().orElse(null)"
}
```

## Dependencies

```xml
<!-- HAPI FHIR -->
<dependency>
    <groupId>ca.uhn.hapi.fhir</groupId>
    <artifactId>hapi-fhir-structures-r4</artifactId>
    <version>6.10.0</version>
</dependency>

<!-- Jackson for JSON -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.15.2</version>
</dependency>

<!-- Apache JEXL for expressions -->
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-jexl3</artifactId>
    <version>3.3</version>
</dependency>
```

## Building

```bash
mvn clean install
```

## Testing

Run the example:
```bash
mvn exec:java -Dexec.mainClass="com.fhir.mapper.examples.ComplexRealTimeExample"
```

## Troubleshooting

### Common Issues

**"Lookup table not found"**
- Ensure lookup table JSON is in `mappings/lookups/` directory
- Check `lookupTable` field references correct lookup `id`

**"Invalid FHIR path"**
- Verify path exists in FHIR resource structure
- Check spelling and case sensitivity (FHIR is case-sensitive)

**"Expression evaluation failed"**
- Test expressions with simple values first
- Check that variables are available in context
- Use `fn.` prefix for built-in functions

**"DataType mismatch"**
- Ensure `dataType` matches expected FHIR type
- Use compatible types (e.g., "string" works for "code", "id", "uri")

## Contributing

Contributions are welcome! Please:
1. Fork the repository
2. Create a feature branch
3. Add tests for new functionality
4. Ensure all tests pass
5. Submit a pull request

## License

[Apache-2.0 license](http://www.apache.org/licenses/LICENSE-2.0)

## Support

For issues and questions:
- GitHub Issues: [link](https://github.com/pradeepkk321/fhir-mapper/issues)
- Documentation: [link]
- Email: pradyskumar@gmail.com

---

**Version**: 1.0.0-SNAPSHOT  
**FHIR Version**: R4 (R5 support available)  
**Last Updated**: 25 Nov 2025
