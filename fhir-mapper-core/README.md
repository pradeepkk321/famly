# FHIR Mapper Core

A declarative, JSON/Excel-driven transformation framework for converting between custom JSON/POJO formats and HL7 FHIR resources.

[![Maven](https://img.shields.io/badge/Maven-3.8+-blue.svg)](https://maven.apache.org/)
[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![HAPI FHIR](https://img.shields.io/badge/HAPI%20FHIR-6.10.0-green.svg)](https://hapifhir.io/)

---

## Overview

FHIR Mapper Core eliminates the need to write Java transformation code for each FHIR resource mapping. Instead, you define mappings in JSON or Excel configuration files with support for:

- **Bidirectional transformations** (JSON ↔ FHIR)
- **Excel-based configuration** for business users (mappings and lookup tables)
- **Expression-based transformations** using JEXL
- **Code lookups** for terminology mapping with multi-system support
- **Conditional field mapping** with context variables
- **Transformation tracing** for debugging and monitoring
- **Validation** against HAPI FHIR structure definitions
- **Security scanning** to prevent malicious expressions

---

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
// 1. Load mappings
MappingLoader loader = new MappingLoader("./mappings");
MappingRegistry registry = loader.loadAll();

// 2. Create engine
TransformationEngine engine = new TransformationEngine(registry);

// 3. Setup context
TransformationContext context = new TransformationContext();
context.setOrganizationId("org-123");
context.getSettings().put("identifierSystem", "urn:oid:2.16.840.1.113883.4.1");

// 4. Transform
String json = "{\"patientId\":\"P123\",\"firstName\":\"John\",\"lastName\":\"Doe\"}";
ResourceMapping mapping = registry.findById("patient-json-to-fhir-v1");
Patient patient = engine.jsonToFhirResource(json, mapping, context, Patient.class);
```

---

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

### Directory Structure

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

### Excel Mapping Format

```
Sheet: "Patient - JSON to FHIR"

Row 1: ID:            | patient-json-to-fhir-v1
Row 2: Direction:     | JSON_TO_FHIR
Row 3: Source Type:   | PatientDTO
Row 4: Target Type:   | Patient
Row 5: (blank)
Row 6: id           | sourcePath | targetPath           | dataType | transformExpression        | condition         | required | defaultValue              | lookupTable    | description
Row 7: patient-id   | patientId  | identifier[0].value  | string   |                            |                   | TRUE     |                           |                | Patient MRN
Row 8: patient-sys  |            | identifier[0].system | uri      |                            |                   | TRUE     | $ctx.settings['mrnSystem']|                | MRN system
Row 9: patient-name | firstName  | name[0].given[0]     | string   |                            |                   | TRUE     |                           |                | First name
Row 10: patient-ssn | ssn        | identifier[1].value  | string   | fn.replace(value, '-', '') | ssn != null      | FALSE    |                           |                | SSN no dashes
Row 11: patient-gen | gender     | gender               | code     |                            |                   | TRUE     |                           | gender-lookup  | Gender code
```

**Multiple sheets = Multiple mappings** in one workbook.

### Excel Lookup Format

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

**All Excel files are scanned:**
- `mappings/excel/*.xlsx` → Resource mappings
- `mappings/lookups-excel/*.xlsx` → Lookup tables
- IDs must be unique across **all** workbooks

---

## Core Features

### Transformation Methods

The engine provides 12 methods covering all conversion scenarios:

| Method | Input | Output | Use Case |
|--------|-------|--------|----------|
| `jsonToFhirMap` | JSON String/Map/POJO | FHIR Map | Get FHIR structure as Map |
| `jsonToFhirJson` | JSON String/Map/POJO | FHIR JSON String | Get FHIR as JSON string |
| `jsonToFhirResource` | JSON String/Map/POJO | HAPI Resource | Get typed HAPI resource |
| `fhirToJsonMap` | FHIR JSON/Map/Resource | JSON Map | Get JSON structure as Map |
| `fhirToJsonString` | FHIR JSON/Map/Resource | JSON String | Get JSON as string |
| `fhirToJsonObject` | FHIR JSON/Map/Resource | POJO | Get typed POJO |

**Examples:**

```java
// JSON String → HAPI Resource
Patient patient = engine.jsonToFhirResource(jsonString, mapping, context, Patient.class);

// POJO → HAPI Resource
PatientDTO dto = new PatientDTO();
Patient patient = engine.jsonToFhirResource(dto, mapping, context, Patient.class);

// HAPI Resource → POJO
PatientDTO dto = engine.fhirToJsonObject(patient, mapping, context, PatientDTO.class);

// JSON String → FHIR JSON String
String fhirJson = engine.jsonToFhirJson(jsonString, mapping, context);
```

### Mapping Configuration

**JSON Structure:**

```json
{
  "id": "patient-json-to-fhir-v1",
  "name": "Patient JSON to FHIR Mapping",
  "version": "1.0.0",
  "direction": "JSON_TO_FHIR",
  "sourceType": "PatientDTO",
  "targetType": "Patient",
  "fieldMappings": [
    {
      "id": "patient-id",
      "sourcePath": "patientId",
      "targetPath": "identifier[0].value",
      "dataType": "string",
      "required": true,
      "description": "Patient MRN"
    }
  ]
}
```

**FieldMapping Properties:**

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `id` | string | Yes | Unique field identifier |
| `sourcePath` | string | No* | Path in source (e.g., `patient.name`) |
| `targetPath` | string | Yes | Path in target (e.g., `name[0].given[0]`) |
| `dataType` | string | No | FHIR type: string, integer, date, boolean, code, etc. |
| `transformExpression` | string | No | JEXL expression (e.g., `fn.uppercase(value)`) |
| `condition` | string | No | JEXL boolean expression (e.g., `value != null`) |
| `validator` | string | No | Validation rule (e.g., `notEmpty()`) |
| `required` | boolean | No | Fail if value is null (default: false) |
| `defaultValue` | string | No | Static value or $ctx variable |
| `lookupTable` | string | No | Reference to lookup table ID |
| `description` | string | No | Documentation |

*sourcePath can be null if defaultValue is provided

**Common Patterns:**

```json
// Required field
{
  "id": "patient-name",
  "sourcePath": "firstName",
  "targetPath": "name[0].given[0]",
  "required": true
}

// Conditional mapping
{
  "id": "patient-middle-name",
  "sourcePath": "middleName",
  "targetPath": "name[0].given[1]",
  "condition": "middleName != null && middleName != ''"
}

// With transformation
{
  "id": "patient-ssn",
  "sourcePath": "ssn",
  "targetPath": "identifier[1].value",
  "transformExpression": "fn.replace(value, '-', '')"
}

// With lookup
{
  "id": "patient-gender",
  "sourcePath": "gender",
  "targetPath": "gender",
  "lookupTable": "gender-lookup"
}

// Context default value
{
  "id": "patient-id-system",
  "targetPath": "identifier[0].system",
  "defaultValue": "$ctx.settings['identifierSystem']"
}
```

### Context Variables

Context variables provide runtime values using the `$ctx` prefix, preventing collision with source data fields.

**Built-in Properties:**

```java
TransformationContext context = new TransformationContext();
context.setOrganizationId("org-123");
context.setFacilityId("facility-456");
context.setTenantId("tenant-789");
```

**Settings Map:**

```java
context.getSettings().put("identifierSystem", "urn:oid:2.16.840.1.113883.4.1");
context.getSettings().put("mrnSystem", "urn:oid:1.2.3.4");
```

**Custom Variables:**

```java
context.setVariable("customKey", "customValue");
```

**Usage in Expressions:**

```json
{
  "transformExpression": "fn.concat('Organization/', $ctx.organizationId)",
  "condition": "$ctx.organizationId != null"
}
```

**Usage in Default Values:**

```json
{
  "defaultValue": "$ctx.settings['identifierSystem']"
}
```

**Why $ctx prefix?**
- Prevents collision with source fields named `organizationId`
- Clear semantic distinction
- Easy to search/grep

### Expression Language (JEXL)

Transform data using JEXL expressions with built-in functions:

**String Functions:**

| Function | Description | Example |
|----------|-------------|---------|
| `fn.uppercase(str)` | Convert to uppercase | `fn.uppercase(value)` → `"JOHN"` |
| `fn.lowercase(str)` | Convert to lowercase | `fn.lowercase(value)` → `"john"` |
| `fn.trim(str)` | Remove whitespace | `fn.trim(value)` |
| `fn.replace(str, old, new)` | Replace substring | `fn.replace(value, '-', '')` |
| `fn.concat(str...)` | Concatenate strings | `fn.concat('Org/', $ctx.organizationId)` |
| `fn.removeHyphens(str)` | Remove all hyphens | `fn.removeHyphens(value)` |

**Date/Time Functions:**

| Function | Description | Example |
|----------|-------------|---------|
| `fn.formatDate(date, pattern)` | Format date | `fn.formatDate(value, 'yyyy-MM-dd')` |
| `fn.now()` | Current timestamp | `fn.now()` |
| `fn.today()` | Current date | `fn.today()` |

**Type Conversion:**

| Function | Description | Example |
|----------|-------------|---------|
| `fn.toInt(value)` | Convert to integer | `fn.toInt("42")` → `42` |
| `fn.toDouble(value)` | Convert to double | `fn.toDouble("3.14")` → `3.14` |
| `fn.toBoolean(value)` | Convert to boolean | `fn.toBoolean("true")` → `true` |

**Utility Functions:**

| Function | Description | Example |
|----------|-------------|---------|
| `fn.defaultIfNull(val, default)` | Fallback value | `fn.defaultIfNull(value, 'Unknown')` |
| `fn.coalesce(val1, val2, ...)` | First non-null | `fn.coalesce(value, defaultVal, 'N/A')` |
| `fn.uuid()` | Generate UUID | `fn.uuid()` |

**Condition Examples:**

```json
// Simple null check
"condition": "value != null"

// String validation
"condition": "value != null && value != ''"

// Multiple conditions
"condition": "ssn != null && ssn.length() == 11"

// Context-based
"condition": "$ctx.organizationId != null"

// Complex logic
"condition": "(age >= 18 && status == 'active') || isVIP == true"
```

**Security Restrictions:**

Expressions are scanned and blocked if they contain:
- System/Runtime access
- File I/O operations
- Network operations
- Reflection/ClassLoader
- Database access
- Script engines

### Code Lookups

Map codes between systems with support for multiple target systems.

**Single-System Lookup:**

```json
{
  "id": "gender-lookup",
  "name": "Gender Code Mapping",
  "sourceSystem": "internal",
  "defaultTargetSystem": "http://hl7.org/fhir/administrative-gender",
  "bidirectional": false,
  "mappings": [
    {
      "sourceCode": "M",
      "targetCode": "male",
      "targetSystem": null,
      "display": "Male"
    },
    {
      "sourceCode": "F",
      "targetCode": "female",
      "targetSystem": null,
      "display": "Female"
    }
  ]
}
```

**Multi-System Lookup:**

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
      "display": "Diabetes mellitus (SNOMED)"
    },
    {
      "sourceCode": "HTN",
      "targetCode": "I10",
      "targetSystem": "http://hl7.org/fhir/sid/icd-10",
      "display": "Hypertension (ICD-10)"
    }
  ]
}
```

When `targetSystem` is null, uses `defaultTargetSystem`. This allows most mappings to use the default while specific codes can override.

**Bidirectional Lookups:**

```json
{
  "id": "gender-lookup-bidirectional",
  "sourceSystem": "internal",
  "defaultTargetSystem": "http://hl7.org/fhir/administrative-gender",
  "bidirectional": true,
  "mappings": [
    {"sourceCode": "M", "targetCode": "male"},
    {"sourceCode": "F", "targetCode": "female"}
  ]
}
```

Enables both:
- `M` → `male` (forward)
- `male` → `M` (reverse)

**Usage in Mapping:**

```json
{
  "id": "patient-gender",
  "sourcePath": "gender",
  "targetPath": "gender",
  "lookupTable": "gender-lookup"
}
```

### Transformation Tracing

Track field-level transformation details for debugging and monitoring.

**Enable Tracing:**

```java
TransformationContext context = new TransformationContext();
context.setOrganizationId("org-123");

// Enable tracing with random UUID
context.enableTracing();

// OR with custom trace ID
context.enableTracing("trace-12345");
```

**Basic Usage:**

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

**Trace Report Output:**

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

**JSON Trace Export:**

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
  "traceId": "abc-123-def-456",
  "source": "PatientDTO",
  "target": "Patient",
  "mappingId": "patient-json-to-fhir-v1",
  "success": true,
  "startTime": 1701234567890,
  "endTime": 1701234567935,
  "fieldTransformationTraces": [
    {
      "fieldId": "patient-id",
      "sourcePath": "patientId",
      "targetPath": "identifier[0].value",
      "sourceValue": "P123",
      "resultValue": "P123",
      "expression": null,
      "condition": null,
      "conditionPassed": false,
      "errorMessage": null,
      "startTime": 1701234567891,
      "endTime": 1701234567892
    }
  ]
}
```

**Analyze Specific Issues:**

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

**Production Usage:**

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

**REST API Integration:**

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

---

## Advanced Usage

### Batch Processing

```java
List<PatientDTO> patients = loadPatients();
List<Patient> fhirPatients = new ArrayList<>();

ResourceMapping mapping = registry.findById("patient-json-to-fhir-v1");
TransformationContext context = createContext();

for (PatientDTO dto : patients) {
    try {
        Patient patient = engine.jsonToFhirResource(dto, mapping, context, Patient.class);
        fhirPatients.add(patient);
    } catch (Exception e) {
        logger.error("Failed patient {}: {}", dto.getPatientId(), e.getMessage());
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
MappingLoader loader = new MappingLoader("./mappings");
ValidationResult result = loader.validateOnly();

if (!result.isValid()) {
    for (ValidationError error : result.getErrors()) {
        System.err.println(error.getContext() + ": " + error.getMessage());
    }
}
```

### Excel ↔ JSON Conversion CLI

**Single File:**

```bash
# Excel to JSON
java -jar fhir-mapper-cli.jar convert --excel-to-json mapping.xlsx mapping.json

# JSON to Excel
java -jar fhir-mapper-cli.jar convert --json-to-excel mapping.json mapping.xlsx
```

**Batch Directory:**

```bash
# Convert all Excel files to JSON
java -jar fhir-mapper-cli.jar batch --excel-to-json ./excel-dir ./json-dir

# Convert all JSON files to Excel
java -jar fhir-mapper-cli.jar batch --json-to-excel ./json-dir ./excel-dir
```

**Validate:**

```bash
java -jar fhir-mapper-cli.jar validate ./mappings
```

---

## Configuration Reference

### ResourceMapping

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | string | Yes | Unique mapping identifier |
| `name` | string | No | Human-readable name |
| `version` | string | No | Mapping version |
| `direction` | enum | Yes | `JSON_TO_FHIR` or `FHIR_TO_JSON` |
| `sourceType` | string | Yes | Source type name |
| `targetType` | string | Yes | Target type name |
| `fieldMappings` | array | Yes | Array of field mappings |

### FieldMapping

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | string | Yes | Unique field identifier |
| `sourcePath` | string | No* | Path in source (e.g., `patient.name`) |
| `targetPath` | string | Yes | Path in target (e.g., `name[0].given[0]`) |
| `dataType` | string | No | FHIR type: string, integer, date, boolean, code |
| `transformExpression` | string | No | JEXL expression |
| `condition` | string | No | JEXL boolean expression |
| `validator` | string | No | Validation rule |
| `required` | boolean | No | Fail if null (default: false) |
| `defaultValue` | string | No | Static value or $ctx variable |
| `lookupTable` | string | No | Reference to lookup table ID |
| `description` | string | No | Documentation |

*sourcePath can be null if defaultValue is provided

### CodeLookupTable

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | string | Yes | Unique lookup identifier |
| `name` | string | No | Human-readable name |
| `sourceSystem` | string | No | Source coding system |
| `defaultTargetSystem` | string | No | Default target system |
| `bidirectional` | boolean | No | Allow reverse lookups (default: false) |
| `defaultSourceCode` | string | No | Fallback for reverse lookup |
| `defaultTargetCode` | string | No | Fallback for forward lookup |
| `mappings` | array | Yes | Array of code mappings |

### CodeMapping

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `sourceCode` | string | Yes | Source code value |
| `targetCode` | string | Yes | Target code value |
| `targetSystem` | string | No | Per-mapping target system (overrides default) |
| `display` | string | No | Human-readable display text |

### TransformationContext

| Field | Type | Description |
|-------|------|-------------|
| `organizationId` | string | Current organization ID |
| `facilityId` | string | Current facility ID |
| `tenantId` | string | Current tenant ID |
| `settings` | Map<String, String> | Configuration settings |
| `variables` | Map<String, Object> | Custom variables |
| `enableTracing` | boolean | Enable transformation tracing |
| `trace` | TransformationTrace | Trace data (if enabled) |

---

## Project Structure

```
your-project/
├── mappings/
│   ├── lookups/              # JSON lookup tables
│   │   ├── gender-lookup.json
│   │   ├── marital-status-lookup.json
│   │   └── address-use-lookup.json
│   ├── lookups-excel/        # Excel lookup workbooks
│   │   ├── standard-terminology.xlsx
│   │   └── custom-codes.xlsx
│   ├── json/                 # JSON resource mappings
│   │   ├── patient-json-to-fhir.json
│   │   ├── patient-fhir-to-json.json
│   │   └── encounter-json-to-fhir.json
│   ├── excel/                # Excel resource mappings
│   │   ├── epic-mappings.xlsx
│   │   └── cerner-mappings.xlsx
│   └── excel-generated/      # Auto-generated (cleaned on load)
└── src/
    └── main/
        └── java/
            └── com/example/
                └── FhirTransformService.java
```

**File Naming:**
- IDs must be unique across all files
- Use descriptive names: `patient-json-to-fhir-v1.json`
- Excel sheets: `"Patient - JSON to FHIR"`

---

## Validation & Security

### Startup Validation

Validates:
- **FHIR Resource Types**: Valid FHIR resources (Patient, Observation, etc.)
- **FHIR Paths**: First-level fields exist in FHIR structure
- **Data Types**: Compatible with FHIR expectations
- **Expressions**: Syntactically valid JEXL
- **Lookup Tables**: All referenced lookups exist
- **Uniqueness**: No duplicate mapping IDs

**Validation Modes:**

```java
// Strict mode (default) - throws on errors
MappingLoader loader = new MappingLoader("./mappings", true);

// Lenient mode - logs errors but continues
MappingLoader loader = new MappingLoader("./mappings", false);
```

### Security Scanning

Scans all expressions for dangerous patterns:

**CRITICAL (always fails):**
- Runtime execution
- Process creation
- Script engines

**HIGH (logged):**
- Reflection
- Network I/O
- Database access

**MEDIUM/LOW (warnings):**
- File I/O
- Threading

Example output:

```
Security Validation Report:

CRITICAL (0):

HIGH (0):

MEDIUM (1):
  - Mapping: patient-json-to-fhir-v1, Field: custom-field (transform)
    File system access
    Expression: new File('/tmp/data')

Total issues: 1
```

---

## REST API Integration Example

```java
@RestController
@RequestMapping("/api/fhir")
public class FhirTransformController {
    
    @Autowired private TransformationEngine engine;
    @Autowired private MappingRegistry registry;
    
    @PostMapping("/patient")
    public ResponseEntity<PatientResponse> createPatient(
            @RequestBody PatientDTO dto,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
            @RequestHeader(value = "X-Enable-Trace", required = false) boolean enableTrace) {
        
        TransformationContext ctx = buildContext();
        
        // Enable tracing if requested
        if (enableTrace) {
            ctx.enableTracing(traceId != null ? traceId : UUID.randomUUID().toString());
        }
        
        ResourceMapping mapping = registry.findById("patient-json-to-fhir-v1");
        Patient patient = engine.jsonToFhirResource(dto, mapping, ctx, Patient.class);
        
        PatientResponse response = new PatientResponse(patient);
        
        // Include trace if enabled
        if (ctx.isEnableTracing()) {
            response.setTrace(ctx.getTrace());
        }
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/patient/{id}")
    public PatientDTO getPatient(@PathVariable String id) throws Exception {
        // Fetch from FHIR server
        Patient fhirPatient = fhirClient.read()
            .resource(Patient.class)
            .withId(id)
            .execute();
        
        TransformationContext ctx = buildContext();
        ResourceMapping mapping = registry.findById("patient-fhir-to-json-v1");
        
        return engine.fhirToJsonObject(fhirPatient, mapping, ctx, PatientDTO.class);
    }
    
    @PostMapping("/batch/patients")
    public ResponseEntity<BatchResponse> batchTransform(
            @RequestBody List<PatientDTO> patients) {
        
        TransformationContext ctx = buildContext();
        ResourceMapping mapping = registry.findById("patient-json-to-fhir-v1");
        
        List<Patient> results = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        
        for (PatientDTO dto : patients) {
            try {
                Patient patient = engine.jsonToFhirResource(dto, mapping, ctx, Patient.class);
                results.add(patient);
            } catch (Exception e) {
                errors.add("Patient " + dto.getPatientId() + ": " + e.getMessage());
            }
        }
        
        return ResponseEntity.ok(new BatchResponse(results, errors));
    }
    
    private TransformationContext buildContext() {
        TransformationContext ctx = new TransformationContext();
        ctx.setOrganizationId(SecurityContextHolder.getContext().getOrganizationId());
        ctx.getSettings().put("identifierSystem", configService.getIdentifierSystem());
        return ctx;
    }
}
```

---

## Examples

### Simple Patient Mapping

**Input JSON:**
```json
{
  "patientId": "P123",
  "firstName": "John",
  "lastName": "Doe",
  "gender": "M"
}
```

**Mapping:**
```json
{
  "id": "patient-simple-v1",
  "direction": "JSON_TO_FHIR",
  "sourceType": "PatientDTO",
  "targetType": "Patient",
  "fieldMappings": [
    {
      "id": "patient-id",
      "sourcePath": "patientId",
      "targetPath": "identifier[0].value",
      "required": true
    },
    {
      "id": "patient-name-family",
      "sourcePath": "lastName",
      "targetPath": "name[0].family",
      "required": true
    },
    {
      "id": "patient-name-given",
      "sourcePath": "firstName",
      "targetPath": "name[0].given[0]",
      "required": true
    },
    {
      "id": "patient-gender",
      "sourcePath": "gender",
      "targetPath": "gender",
      "lookupTable": "gender-lookup"
    }
  ]
}
```

**Usage:**
```java
Patient patient = engine.jsonToFhirResource(jsonInput, mapping, context, Patient.class);
```

### Complex Mapping with Extensions

**US Core Patient with Race/Ethnicity:**

```json
{
  "id": "patient-race-extension",
  "sourcePath": "race",
  "targetPath": "extension[0].extension[0].valueCoding.code",
  "condition": "race != null"
},
{
  "id": "patient-race-system",
  "targetPath": "extension[0].extension[0].valueCoding.system",
  "defaultValue": "urn:oid:2.16.840.1.113883.6.238",
  "condition": "race != null"
},
{
  "id": "patient-race-url-inner",
  "targetPath": "extension[0].extension[0].url",
  "defaultValue": "ombCategory",
  "condition": "race != null"
},
{
  "id": "patient-race-url-outer",
  "targetPath": "extension[0].url",
  "defaultValue": "http://hl7.org/fhir/us/core/StructureDefinition/us-core-race",
  "condition": "race != null"
}
```

### Bidirectional Transformation

**JSON → FHIR:**
```java
PatientDTO dto = new PatientDTO();
dto.setPatientId("P123");
dto.setFirstName("John");

ResourceMapping jsonToFhir = registry.findById("patient-json-to-fhir-v1");
Patient patient = engine.jsonToFhirResource(dto, jsonToFhir, context, Patient.class);
```

**FHIR → JSON:**
```java
ResourceMapping fhirToJson = registry.findById("patient-fhir-to-json-v1");
PatientDTO dto = engine.fhirToJsonObject(patient, fhirToJson, context, PatientDTO.class);
```

---

## Troubleshooting

### Lookup table not found

**Error:**
```
TransformationException: Lookup table not found: gender-lookup
```

**Solutions:**
1. Check file exists: `mappings/lookups/gender-lookup.json`
2. Verify ID in lookup file matches: `"id": "gender-lookup"`
3. Check loader logs: `grep "Loaded lookup" application.log`

### Invalid FHIR path

**Error:**
```
[ERROR] Mapping: patient-v1, Field: custom-field: Invalid FHIR path 'customField'
```

**Solutions:**
1. Verify field exists in FHIR resource
2. Check spelling and case (FHIR is case-sensitive)
3. Use HAPI FHIR documentation for valid paths

### Expression evaluation failed

**Error:**
```
ExpressionEvaluationException: Failed to evaluate: fn.uppercase(value)
```

**Solutions:**
1. Check expression syntax
2. Verify function exists: use `fn.` prefix
3. Ensure value is not null: add condition `value != null`
4. Test expression with simple values first

### Context variable not resolved

**Error:**
```
Expression evaluated to null: 'fn.concat('Organization/', $ctx.organizationId)'
```

**Solutions:**
1. Check context is set: `context.setOrganizationId("org-123")`
2. Verify $ctx prefix is used
3. Check variable name spelling
4. Enable tracing to see actual values

### Type conversion issues

**Error:**
```
Type conversion failed: cannot convert 'abc' to integer
```

**Solutions:**
1. Verify source data type matches expected
2. Add validation in source system
3. Use transformExpression to convert
4. Make field non-required if optional

---

## Building & Testing

### Build

```bash
mvn clean install
```

### Run Examples

```bash
mvn exec:java -Dexec.mainClass="com.fhir.mapper.examples.ComplexRealTimeExample"
```

### Run Tests

```bash
mvn test
```

### Validate Mappings

```bash
java -jar fhir-mapper-cli.jar validate ./mappings
```

---

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

<!-- Apache POI for Excel -->
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.3</version>
</dependency>
```

---

## Limitations

### Current Limitations

1. **Path Validation**: Only first-level fields validated against FHIR structure
2. **Array Wildcards**: Cannot map "all elements" automatically (e.g., `addresses[*]`)
3. **FHIRPath Queries**: Does not support complex FHIRPath (e.g., `name.where(use='official')`)
4. **Circular References**: Not detected or handled

### Workarounds

**Array Mapping:**
Specify each index explicitly:
```json
{"sourcePath": "addresses[0]", "targetPath": "address[0]"},
{"sourcePath": "addresses[1]", "targetPath": "address[1]"}
```

**Complex Queries:**
Use JEXL in transformExpression:
```json
{
  "transformExpression": "addresses.stream().filter(a -> a.type == 'HOME').findFirst().orElse(null)"
}
```

---

## Contributing

### Development Setup

```bash
# Clone repository
git clone https://github.com/your-org/fhir-mapper-core.git
cd fhir-mapper-core

# Build
mvn clean install

# Run tests
mvn test

# Run examples
mvn exec:java -Dexec.mainClass="com.fhir.mapper.examples.ComplexRealTimeExample"
```

### Code Style

- Follow Google Java Style Guide
- Use SLF4J for logging (no System.out)
- Write tests for all new features
- Update documentation

### Pull Request Process

1. Create feature branch: `feature/your-feature-name`
2. Write tests (aim for >80% coverage)
3. Update documentation
4. Run full test suite
5. Submit PR with description
6. Address review comments

---

## License & Support

### License

Apache License 2.0

Copyright 2025 Pradeep Kumara Krishnegowda

### Support

- 📖 **Documentation**: [Wiki](https://github.com/your-org/fhir-mapper-core/wiki)
- 💬 **Discussions**: [GitHub Discussions](https://github.com/your-org/fhir-mapper-core/discussions)
- 🐛 **Issues**: [GitHub Issues](https://github.com/your-org/fhir-mapper-core/issues)
- 📧 **Email**: support@example.com

---

**Version**: 1.0.0-SNAPSHOT  
**FHIR Version**: R4 (R5 support available)  
**Last Updated**: November 2025