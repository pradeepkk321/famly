# FHIR Mapper Core

A declarative, JSON-driven transformation framework for converting between custom JSON/POJO formats and HL7 FHIR resources.

[![Maven](https://img.shields.io/badge/Maven-3.8+-blue.svg)](https://maven.apache.org/)
[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![HAPI FHIR](https://img.shields.io/badge/HAPI%20FHIR-6.10.0-green.svg)](https://hapifhir.io/)

## Overview

FHIR Mapper Core eliminates the need to write Java transformation code for each FHIR resource mapping. Instead, you define mappings in JSON configuration files with support for:

- **Bidirectional transformations** (JSON ↔ FHIR)
- **Expression-based transformations** using JEXL
- **Code lookups** for terminology mapping
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
ResourceMapping mapping = registry.findBySourceAndDirection(
    "PatientDTO", 
    MappingDirection.JSON_TO_FHIR
);

// 5. Transform!
String jsonInput = "{\"patientId\":\"P123\",\"firstName\":\"John\",\"lastName\":\"Doe\",\"gender\":\"M\"}";
Patient patient = engine.jsonToFhirResource(jsonInput, mapping, context, Patient.class);
```

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
| `targetSystem` | string | No | Target coding system |
| `bidirectional` | boolean | No | Allow reverse lookups (default: false) |
| `defaultSourceCode` | string | No | Fallback for reverse lookup |
| `defaultTargetCode` | string | No | Fallback for forward lookup |
| `mappings` | array | Yes | Array of code mappings |

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

[Your License Here]

## Support

For issues and questions:
- GitHub Issues: [link]
- Documentation: [link]
- Email: [email]

---

**Version**: 1.0.0-SNAPSHOT  
**FHIR Version**: R4 (R5 support available)  
**Last Updated**: 2024