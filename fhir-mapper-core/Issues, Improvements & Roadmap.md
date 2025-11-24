# FHIR Mapper Core - Issues, Improvements & Roadmap

**Version**: 1.0.0-SNAPSHOT  
**Last Updated**: November 2025  
**Status**: Production-Ready with Recommended Improvements

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Critical Issues](#critical-issues)
3. [High Priority Improvements](#high-priority-improvements)
4. [Medium Priority Enhancements](#medium-priority-enhancements)
5. [Future Enhancements](#future-enhancements)
6. [Implementation Roadmap](#implementation-roadmap)
7. [Breaking Changes](#breaking-changes)
8. [Migration Guide](#migration-guide)

---

## Executive Summary

The FHIR Mapper Core is a **well-architected framework** with strong separation of concerns, comprehensive security validation, and robust FHIR integration. However, several areas need attention before widespread production deployment.

### Overall Assessment

| Category | Status | Notes |
|----------|--------|-------|
| Architecture | ✅ Excellent | Clean separation, extensible design |
| Security | ✅ Strong | Comprehensive validation, restricted execution |
| API Design | ✅ Good | 12 methods cover all use cases |
| Documentation | ⚠️ Adequate | Needs user guide and tutorials |
| Testing | ❌ Incomplete | No visible unit test coverage |
| Performance | ⚠️ Unoptimized | No caching, needs profiling |
| Error Handling | ⚠️ Basic | Context loss, poor debugging |

### Recommended Action

**Phase 1 (Stabilization)** must be completed before production deployment. Phases 2-4 can be implemented incrementally based on usage patterns and feedback.

---

## Critical Issues

### 🔴 ISSUE-001: Debug Logging in Production Code

**Severity**: High  
**Impact**: Performance, Code Quality  
**Effort**: Low (1-2 days)

**Problem:**
```java
// MappingExpressionEvaluator.java
public Object evaluate(...) {
    System.out.println("=== MappingExpressionEvaluator.evaluate ===");
    System.out.println("Expression: " + expression);
    System.out.println("Value: " + value);
    System.out.println("Value type: " + (value != null ? value.getClass().getName() : "null"));
    // ... 10+ debug statements per evaluation
}
```

**Impact:**
- Console pollution in production
- Performance overhead from string concatenation
- Cannot be disabled without code changes
- Makes logs unreadable at scale

**Solution:**
```java
private static final Logger logger = LoggerFactory.getLogger(MappingExpressionEvaluator.class);

public Object evaluate(...) {
    if (logger.isDebugEnabled()) {
        logger.debug("Evaluating expression: {} with value: {}", expression, value);
    }
    
    if (logger.isTraceEnabled()) {
        logger.trace("Value type: {}", value != null ? value.getClass().getName() : "null");
    }
    
    // ... execution
}
```

**Files to Update:**
- `MappingExpressionEvaluator.java` (22+ System.out statements)
- `TransformFunctions.java` (5+ System.out statements)

---

### 🔴 ISSUE-002: Missing Unit Test Coverage

**Severity**: Critical  
**Impact**: Reliability, Maintainability  
**Effort**: High (1-2 weeks)

**Problem:**
No JUnit tests found in the provided codebase. Only example files exist.

**Required Test Coverage:**

```java
// Core transformation tests
@Test void testJsonStringToFhirMap()
@Test void testJsonStringToFhirResource()
@Test void testPojoToFhirResource()
@Test void testFhirToJsonMap()
@Test void testFhirResourceToPojo()

// Edge case tests
@Test void testNullSourcePath()
@Test void testMissingRequiredField()
@Test void testInvalidLookupTable()
@Test void testCircularReference()
@Test void testLargeArrayHandling()

// Expression tests
@Test void testBasicTransformExpression(me)
@Test void testContextVariableAccess()
@Test void testConditionalMapping()
@Test void testInvalidExpression()

// Security tests
@Test void testDangerousExpressionRejected()
@Test void testFileIOBlocked()
@Test void testReflectionBlocked()

// Validation tests
@Test void testInvalidFhirPath()
@Test void testDataTypeMismatch()
@Test void testDuplicateFieldId()
```

**Recommended Coverage Targets:**
- Line Coverage: >80%
- Branch Coverage: >70%
- Critical paths: 100%

---

### 🔴 ISSUE-003: Silent Type Conversion Failures

**Severity**: Medium-High  
**Impact**: Data Integrity  
**Effort**: Low (1 day)

**Problem:**
```java
private Object convertToType(Object value, String dataType) {
    try {
        switch (dataType.toLowerCase()) {
            case "integer": return Integer.parseInt(strValue);
            // ...
        }
    } catch (Exception e) {
        // Returns original value - NO LOGGING!
        return value;
    }
}
```

**Impact:**
- Data corruption: "abc" passed as integer stays as string
- No visibility into conversion failures
- Difficult to debug data quality issues

**Solution:**
```java
private Object convertToType(Object value, String dataType, String fieldId) {
    String strValue = value.toString();
    
    try {
        switch (dataType.toLowerCase()) {
            case "integer":
            case "unsignedint":
            case "positiveint":
                return Integer.parseInt(strValue);
            case "decimal":
                return Double.parseDouble(strValue);
            case "boolean":
                return Boolean.parseBoolean(strValue);
            default:
                return value;
        }
    } catch (NumberFormatException e) {
        String msg = String.format(
            "Type conversion failed for field '%s': cannot convert '%s' to %s",
            fieldId, value, dataType
        );
        logger.warn(msg);
        
        if (strictTypeConversion) {
            throw new TransformationException(msg, e);
        }
        
        return value; // Fall back to original
    }
}
```

**Configuration:**
```java
TransformationEngine engine = new TransformationEngine(registry)
    .setStrictTypeConversion(true); // Fail-fast mode
```

---

### 🟡 ISSUE-004: Context Variable Access Inconsistency

**Severity**: Medium  
**Impact**: Developer Experience, Maintainability  
**Effort**: Medium (2-3 days)

**Problem:**
Multiple ways to access the same variable causes confusion:

```java
// In JEXL expressions - THREE different ways:
"$ctx.organizationId"           // String replacement (deprecated)
"ctx.organizationId"            // JEXL object access
"organizationId"                // Direct variable (also works!)

// In defaultValue - different syntax:
"defaultValue": "$ctx.settings['mrnSystem']"
```

**Issues:**
- Documentation needs to explain all three
- Easy to use wrong syntax
- Maintenance burden supporting multiple approaches

**Solution:**

**Option A: Standardize on JEXL Object Access (Recommended)**
```json
{
  "transformExpression": "fn.concat('Organization/', ctx.organizationId)",
  "condition": "ctx.organizationId != null",
  "defaultValue": "ctx.settings.mrnSystem"
}
```

**Option B: Keep Both with Clear Deprecation**
```java
// Mark as deprecated in 1.1.0, remove in 2.0.0
@Deprecated
private String resolveContextInExpression(String expression, TransformationContext context) {
    logger.warn("$ctx. syntax is deprecated. Use 'ctx.' instead: {}", expression);
    return expression.replace("$ctx.", "ctx.");
}
```

**Migration Path:**
1. Add deprecation warnings in 1.1.0
2. Provide migration tool to update JSON files
3. Remove support in 2.0.0

---

## High Priority Improvements

### 🟡 IMPROVEMENT-001: Enhanced Error Context

**Priority**: High  
**Impact**: Developer Experience  
**Effort**: Medium (3-4 days)

**Problem:**
Errors lose valuable context:

```java
throw new TransformationException("Required field missing: " + sourcePath);
// Lost: Which mapping? Which input record? What was the source data?
```

When processing 10,000 patients and one fails, you get:
```
TransformationException: Required field missing: demographics.birthSex
```

But you don't know which patient, what file, or what the input looked like.

**Solution:**

```java
public class TransformationException extends RuntimeException {
    private final TransformationErrorContext errorContext;
    
    public TransformationException(String message, TransformationErrorContext context) {
        super(formatMessage(message, context));
        this.errorContext = context;
    }
    
    private static String formatMessage(String message, TransformationErrorContext ctx) {
        return String.format(
            "%s\n" +
            "  Mapping: %s\n" +
            "  Field: %s\n" +
            "  Source Path: %s\n" +
            "  Target Path: %s\n" +
            "  Record ID: %s\n" +
            "  Input Sample: %s",
            message,
            ctx.getMappingId(),
            ctx.getFieldId(),
            ctx.getSourcePath(),
            ctx.getTargetPath(),
            ctx.getRecordIdentifier(),
            truncate(ctx.getSourceData(), 200)
        );
    }
}
```

**Usage:**
```java
try {
    processMapping(source, target, fieldMapping, context);
} catch (Exception e) {
    throw new TransformationException(
        "Failed to map required field",
        TransformationErrorContext.builder()
            .mappingId(parentMapping.getId())
            .fieldId(fieldMapping.getId())
            .sourcePath(fieldMapping.getSourcePath())
            .targetPath(fieldMapping.getTargetPath())
            .recordIdentifier(extractRecordId(source))
            .sourceData(source)
            .build(),
        e
    );
}
```

**Benefits:**
- Pinpoint exact failure location
- Include input data for debugging
- Stack trace includes all context
- Easy to parse for automated error handling

---

### 🟡 IMPROVEMENT-002: Path Navigation Enhancements

**Priority**: High  
**Impact**: Functionality, Flexibility  
**Effort**: High (1 week)

**Current Limitations:**

```java
// PathNavigator only handles simple cases:
"address[0].line[0]"           // ✓ Works
"address[1].line[2]"           // ✓ Works

// But not:
"address[*].line[0]"           // ✗ Map all addresses
"name[?(@.use='official')]"    // ✗ FHIRPath queries
"telecom[0..2]"                // ✗ Range selection
```

**Proposed Solution:**

```java
public class EnhancedPathNavigator {
    
    /**
     * Supports:
     * - Simple paths: "name.family"
     * - Array indices: "name[0].given[0]"
     * - Wildcards: "name[*].given[0]"
     * - Filters: "telecom[?(@.system='phone')].value"
     * - Ranges: "address[0..2]"
     */
    public Object getValue(Map<String, Object> data, String path) {
        if (path.contains("[*]")) {
            return getValuesForWildcard(data, path);
        }
        if (path.contains("[?(")) {
            return getValuesWithFilter(data, path);
        }
        if (path.contains("..")) {
            return getValuesInRange(data, path);
        }
        return getSimpleValue(data, path); // Current implementation
    }
    
    private List<Object> getValuesForWildcard(Map<String, Object> data, String path) {
        // Implementation for wildcard paths
    }
}
```

**Configuration:**
```json
{
  "id": "all-addresses",
  "sourcePath": "addresses[*]",
  "targetPath": "address[*]",
  "iterationStrategy": "ALL",
  "itemMapping": {
    "line1": "line[0]",
    "line2": "line[1]",
    "city": "city"
  }
}
```

**Alternative: Use FHIRPath Library**
```xml
<dependency>
    <groupId>ca.uhn.hapi.fhir</groupId>
    <artifactId>hapi-fhir-fhirpath</artifactId>
    <version>6.10.0</version>
</dependency>
```

---

### 🟡 IMPROVEMENT-003: Performance Optimization

**Priority**: High  
**Impact**: Scalability  
**Effort**: Medium (1 week)

**Current Performance Issues:**

1. **Expression Re-parsing**: Every transformation re-parses JEXL expressions
2. **No Path Compilation**: Paths like "name[0].given[0]" are re-parsed every time
3. **Repeated Validations**: Same validations run for every record
4. **No Batching**: Each record processed individually

**Proposed Solution:**

```java
public class CompiledMapping {
    private final ResourceMapping mapping;
    private final Map<String, JexlExpression> compiledExpressions;
    private final Map<String, PathAccessor> compiledPaths;
    private final Map<String, CodeLookupTable> resolvedLookups;
    
    // Pre-compiled execution plan
    private final List<ExecutionStep> executionPlan;
}

public class TransformationEngine {
    private final LoadingCache<String, CompiledMapping> compiledMappings;
    
    public TransformationEngine(MappingRegistry registry) {
        this.compiledMappings = CacheBuilder.newBuilder()
            .maximumSize(100)
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build(new CacheLoader<String, CompiledMapping>() {
                @Override
                public CompiledMapping load(String mappingId) {
                    return compileMapping(registry.findById(mappingId));
                }
            });
    }
    
    /**
     * Fast path - use pre-compiled mapping
     */
    public <T> List<T> transformBatch(
        List<Map<String, Object>> sources,
        String mappingId,
        TransformationContext context,
        Class<T> targetClass
    ) throws Exception {
        CompiledMapping compiled = compiledMappings.get(mappingId);
        
        return sources.parallelStream()
            .map(source -> {
                try {
                    return compiled.execute(source, context, targetClass);
                } catch (Exception e) {
                    logger.error("Transformation failed", e);
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
}
```

**Expected Performance Gains:**
- 3-5x faster for repeated transformations
- 10x faster for batch operations (with parallelization)
- Reduced memory pressure from cached compiled expressions

---

### 🟡 IMPROVEMENT-004: Array Mapping Patterns

**Priority**: Medium-High  
**Impact**: Functionality, Usability  
**Effort**: High (1 week)

**Problem:**

Current approach requires explicit index mapping:
```json
{
  "sourcePath": "addresses[0].line1",
  "targetPath": "address[0].line[0]"
},
{
  "sourcePath": "addresses[1].line1",
  "targetPath": "address[1].line[0]"
}
```

This doesn't scale for variable-length arrays.

**Solution:**

```json
{
  "id": "address-array-mapping",
  "sourcePath": "addresses",
  "targetPath": "address",
  "dataType": "array",
  "arrayMapping": {
    "strategy": "MAP_ALL",
    "filter": "type == 'HOME' || type == 'WORK'",
    "sort": "isPrimary desc, type asc",
    "limit": 5,
    "itemMappings": [
      {
        "sourcePath": "type",
        "targetPath": "use",
        "lookupTable": "address-use-lookup"
      },
      {
        "sourcePath": "line1",
        "targetPath": "line[0]"
      },
      {
        "sourcePath": "line2",
        "targetPath": "line[1]",
        "condition": "line2 != null && line2 != ''"
      },
      {
        "sourcePath": "city",
        "targetPath": "city"
      },
      {
        "sourcePath": "state",
        "targetPath": "state"
      }
    ]
  }
}
```

**Strategies:**
- `MAP_ALL`: Transform all elements
- `MAP_FIRST`: Transform first element only
- `MAP_FILTERED`: Apply filter expression
- `MAP_PRIMARY`: Find and map "primary" element

---

## Medium Priority Enhancements

### 🟢 ENHANCEMENT-001: Transformation Hooks

**Priority**: Medium  
**Impact**: Extensibility  
**Effort**: Medium (3-4 days)

**Use Case:**
Allow custom logic injection without modifying core engine.

```java
public interface TransformationHook {
    void beforeTransformation(Map<String, Object> source, TransformationContext context);
    void afterTransformation(Map<String, Object> result, TransformationContext context);
    void onError(Exception e, Map<String, Object> source, TransformationContext context);
}

public class TransformationEngine {
    private final List<TransformationHook> hooks = new ArrayList<>();
    
    public void registerHook(String resourceType, TransformationHook hook) {
        hooks.add(hook);
    }
    
    private Map<String, Object> performTransformation(...) {
        // Execute pre-processors
        hooks.forEach(hook -> hook.beforeTransformation(source, context));
        
        try {
            Map<String, Object> result = doTransform(source, mapping, context);
            
            // Execute post-processors
            hooks.forEach(hook -> hook.afterTransformation(result, context));
            
            return result;
        } catch (Exception e) {
            hooks.forEach(hook -> hook.onError(e, source, context));
            throw e;
        }
    }
}
```

**Example Usage:**
```java
// Audit logging hook
engine.registerHook("Patient", new TransformationHook() {
    @Override
    public void beforeTransformation(Map<String, Object> source, TransformationContext ctx) {
        auditLog.info("Transforming patient: {}", source.get("patientId"));
    }
    
    @Override
    public void afterTransformation(Map<String, Object> result, TransformationContext ctx) {
        auditLog.info("Transformation complete: {}", result.get("id"));
    }
});

// Data enrichment hook
engine.registerHook("Patient", new TransformationHook() {
    @Override
    public void beforeTransformation(Map<String, Object> source, TransformationContext ctx) {
        // Enrich from external system
        String patientId = (String) source.get("patientId");
        Demographics demo = externalSystem.fetchDemographics(patientId);
        source.put("enrichedData", demo);
    }
});
```

---

### 🟢 ENHANCEMENT-002: Enhanced Lookup Tables

**Priority**: Medium  
**Impact**: Functionality  
**Effort**: Medium (4-5 days)

**Current Limitation:**
Simple code-to-code mapping only.

**Proposed Enhancement:**

```json
{
  "id": "diagnosis-code-lookup",
  "name": "ICD-10 to SNOMED Mapping",
  "sourceSystem": "http://hl7.org/fhir/sid/icd-10",
  "targetSystem": "http://snomed.info/sct",
  "lookupType": "ADVANCED",
  "mappings": [
    {
      "sourceCode": "E11.9",
      "targetCode": "44054006",
      "display": "Type 2 diabetes mellitus without complications",
      "context": {
        "ageMin": 18,
        "effectiveDate": "2020-01-01"
      },
      "equivalence": "equivalent",
      "comments": "Direct 1:1 mapping"
    },
    {
      "sourceCode": "I10",
      "targetCodes": [
        {"code": "38341003", "primary": true},
        {"code": "429457004", "primary": false}
      ],
      "display": "Essential hypertension",
      "equivalence": "wider"
    }
  ],
  "hierarchical": true,
  "parentMapping": {
    "E11": ["E11.0", "E11.1", "E11.9"]
  }
}
```

**Features:**
- **Multi-valued mappings**: One source → multiple targets
- **Contextual mappings**: Age, date, or custom context-based selection
- **Hierarchical codes**: Parent-child relationships
- **Equivalence indicators**: equivalent, wider, narrower, inexact
- **Temporal validity**: effectiveDate, expirationDate
- **Fallback chains**: Try specific first, fall back to general

---

### 🟢 ENHANCEMENT-003: Fluent Builder API

**Priority**: Medium  
**Impact**: Developer Experience  
**Effort**: High (1 week)

**Problem:**
JSON configuration is verbose and prone to typos. No IDE support.

**Solution:**

```java
ResourceMapping mapping = ResourceMapping.builder()
    .id("patient-mapping-v1")
    .name("Patient DTO to FHIR")
    .direction(JSON_TO_FHIR)
    .sourceType("PatientDTO")
    .targetType("Patient")
    
    // Simple field mapping
    .field(f -> f
        .id("patient-id")
        .from("patientId")
        .to("identifier[0].value")
        .required()
        .dataType("string"))
    
    // Field with lookup
    .field(f -> f
        .id("patient-gender")
        .from("gender")
        .to("gender")
        .lookup("gender-lookup")
        .required())
    
    // Field with transformation
    .field(f -> f
        .id("patient-ssn")
        .from("ssn")
        .to("identifier[1].value")
        .transform("fn.replace(value, '-', '')")
        .when("ssn != null && ssn != ''"))
    
    // Field with default from context
    .field(f -> f
        .id("patient-id-system")
        .to("identifier[0].system")
        .defaultValue("$ctx.settings['identifierSystem']")
        .required())
    
    // Array mapping
    .arrayField(f -> f
        .id("addresses")
        .from("addresses")
        .to("address")
        .mapAll()
        .itemMapping(item -> item
            .field("type", "use", lookup("address-use-lookup"))
            .field("line1", "line[0]")
            .field("city", "city")))
    
    .build();

// Compile and register
registry.register(mapping);
```

**Benefits:**
- Type-safe configuration
- IDE autocomplete and validation
- Refactoring-friendly
- Can be version-controlled as code
- Unit testable

---

### 🟢 ENHANCEMENT-004: Mapping Visualization

**Priority**: Low-Medium  
**Impact**: Documentation, Understanding  
**Effort**: Medium (3-4 days)

**Use Case:**
Generate visual diagrams of mappings for documentation and review.

```java
MappingVisualizer visualizer = new MappingVisualizer();

// Generate Mermaid diagram
String mermaidDiagram = visualizer.toMermaid(mapping);

// Generate HTML interactive diagram
String htmlDiagram = visualizer.toHtml(mapping);

// Generate Graphviz DOT format
String dotDiagram = visualizer.toGraphviz(mapping);
```

**Output Example (Mermaid):**
``` mermaid
graph LR
    A[PatientDTO] --> B[Patient]
    
    A1[patientId] --> B1[identifier.value]
    A2[firstName] --> B2[name.given]
    A3[lastName] --> B3[name.family]
    A4[gender] -->|lookup| B4[gender]
    A5[ssn] -->|transform| B5[identifier.value]
  
    style A4 fill:#ff9
    style A5 fill:#9f9
```
```

**Features:**
- Color-code by operation type (lookup, transform, default)
- Show conditions and validators
- Export to PNG/SVG
- Interactive HTML with tooltips
---

## Future Enhancements

### 🔵 FUTURE-001: GraphQL-Style Path Queries

**Priority**: Low  
**Effort**: High (2 weeks)

Allow more powerful path expressions:

```json
{
  "sourcePath": "contacts{type='MOBILE'}.value",
  "targetPath": "telecom[0].value"
}
```

---

### 🔵 FUTURE-002: Transformation Templates

**Priority**: Low  
**Effort**: Medium (1 week)

Reusable mapping fragments:

```json
{
  "templates": {
    "standardIdentifier": {
      "fieldMappings": [
        {"sourcePath": "id", "targetPath": "identifier[0].value"},
        {"defaultValue": "$ctx.idSystem", "targetPath": "identifier[0].system"}
      ]
    }
  },
  "fieldMappings": [
    {"$ref": "#/templates/standardIdentifier"}
  ]
}
```

---

### 🔵 FUTURE-003: Bidirectional Single Mapping

**Priority**: Low  
**Effort**: High (2 weeks)

Define both directions in one file:

```json
{
  "id": "patient-mapping-bidirectional",
  "bidirectional": true,
  "sourceType": "PatientDTO",
  "targetType": "Patient",
  "fieldMappings": [
    {
      "jsonPath": "patientId",
      "fhirPath": "identifier[0].value",
      "forwardTransform": null,
      "reverseTransform": null
    }
  ]
}
```

---

### 🔵 FUTURE-004: Schema Generation

**Priority**: Low  
**Effort**: Medium (1 week)

Generate JSON schemas from mappings:

```java
String jsonSchema = SchemaGenerator.fromMapping(mapping)
    .withRequired(true)
    .withDescriptions(true)
    .generate();
```

---

### 🔵 FUTURE-005: Mapping Testing Framework

**Priority**: Low  
**Effort**: High (1-2 weeks)

Built-in testing DSL:

```json
{
  "mappingTests": [
    {
      "name": "Basic patient transformation",
      "input": {
        "patientId": "P123",
        "firstName": "John",
        "lastName": "Doe"
      },
      "expected": {
        "resourceType": "Patient",
        "identifier[0].value": "P123",
        "name[0].given[0]": "John"
      }
    }
  ]
}
```

---

## Implementation Roadmap

### Phase 1: Stabilization (4-6 weeks) ⚠️ **REQUIRED**

**Goal**: Production-ready release

| Task | Priority | Effort | Owner | Status |
|------|----------|--------|-------|--------|
| Remove debug logging (ISSUE-001) | 🔴 Critical | 2d | TBD | Not Started |
| Add unit test suite (ISSUE-002) | 🔴 Critical | 2w | TBD | Not Started |
| Fix type conversion (ISSUE-003) | 🔴 Critical | 1d | TBD | Not Started |
| Standardize context access (ISSUE-004) | 🟡 High | 3d | TBD | Not Started |
| Enhanced error context (IMP-001) | 🟡 High | 4d | TBD | Not Started |
| Documentation updates | 🟡 High | 3d | TBD | Not Started |

**Deliverables:**
- ✅ 80%+ test coverage
- ✅ No System.out statements
- ✅ Proper error handling with context
- ✅ User guide and tutorials
- ✅ v1.0.0 release

---

### Phase 2: Performance (4-6 weeks)

**Goal**: Handle production scale (1000s of records/sec)

| Task | Priority | Effort | Owner | Status |
|------|----------|--------|-------|--------|
| Compiled mappings (IMP-003) | 🟡 High | 1w | TBD | Not Started |
| Batch operations | 🟡 High | 1w | TBD | Not Started |
| Performance profiling | 🟡 High | 3d | TBD | Not Started |
| Caching strategy | 🟡 High | 3d | TBD | Not Started |
| Load testing | 🟡 High | 2d | TBD | Not Started |

**Deliverables:**
- ✅ 3-5x performance improvement
- ✅ Batch API
- ✅ Performance benchmarks
- ✅ v1.1.0 release

---

### Phase 3: Enhanced Features (6-8 weeks)

**Goal**: Advanced mapping capabilities

| Task | Priority | Effort | Owner | Status |
|------|----------|--------|-------|--------|
| Enhanced path navigation (IMP-002) | 🟡 High | 1w | TBD | Not Started |
| Array mapping patterns (IMP-004) | 🟡 High | 1w | TBD | Not Started |
| Transformation hooks (ENH-001) | 🟢 Medium | 4d | TBD | Not Started |
| Enhanced lookups (ENH-002) | 🟢 Medium | 5d | TBD | Not Started |
| Fluent builder API (ENH-003) | 🟢 Medium | 1w | TBD | Not Started |

**Deliverables:**
- ✅ Array mapping support
- ✅ Hook system
- ✅ Advanced lookups
- ✅ Builder API
- ✅ v1.2.0 release

---

### Phase 4: Advanced Features (8-10 weeks)

**Goal**: Best-in-class mapping framework

| Task | Priority | Effort | Owner | Status |
|------|----------|--------|-------|--------|
| Mapping visualization (ENH-004) | 🟢 Medium | 4d | TBD | Not Started |
| GraphQL paths (FUT-001) | 🔵 Low | 2w | TBD | Not Started |
| Transformation templates (FUT-002) | 🔵 Low | 1w | TBD | Not Started |
| Schema generation (FUT-004) | 🔵 Low | 1w | TBD | Not Started |
| Testing framework (FUT-005) | 🔵 Low | 2w | TBD | Not Started |

**Deliverables:**
- ✅ Visual mapping tools
- ✅ Template system
- ✅ Testing framework
- ✅ v2.0.0 release

---

## Breaking Changes

### Version 1.x → 2.0

**Planned Breaking Changes:**

1. **Context Variable Syntax**
   - ❌ Remove: `$ctx.organizationId` string replacement
   - ✅ Use: `ctx.organizationId` JEXL object access

2. **Deprecated Methods**
   - ❌ Remove: `resolveContextInExpression()`
   - ✅ Context available directly in JEXL

3. **Array Mapping**
   - ❌ Old: Manual index specification
   - ✅ New: Array mapping patterns with `arrayMapping` configuration

4. **Type Conversion**
   - ❌ Old: Silent failures return original value
   - ✅ New: Strict mode throws exceptions by default

5. **Path Navigation**
   - ❌ Old: Simple dot notation only
   - ✅ New: Enhanced path syntax with wildcards and filters

**Migration Timeline:**
- **v1.1.0** (Q1 2025): Deprecation warnings added
- **v1.2.0** (Q2 2025): Migration tools provided
- **v2.0.0** (Q3 2025): Breaking changes enforced

---

## Migration Guide

### From 1.0 to 1.1

**No breaking changes.** Optional improvements:

#### 1. Update Context Variable References

**Before:**
```json
{
  "defaultValue": "$ctx.settings['mrnSystem']",
  "transformExpression": "fn.concat('Organization/', $ctx.organizationId)"
}
```

**After:**
```json
{
  "defaultValue": "ctx.settings.mrnSystem",
  "transformExpression": "fn.concat('Organization/', ctx.organizationId)"
}
```

#### 2. Add Strict Mode (Optional)

```java
// Enable strict type conversion
TransformationEngine engine = new TransformationEngine(registry)
    .setStrictTypeConversion(true);
```

#### 3. Replace Debug Code

If you have custom extensions, replace:
```java
System.out.println("Debug: " + value);
```

With:
```java
logger.debug("Processing value: {}", value);
```

---

### From 1.1 to 2.0

**Breaking changes.** Migration required:

#### Migration Tool

```bash
# Automatically update mapping files
java -jar fhir-mapper-migrator.jar \
  --input ./mappings \
  --output ./mappings-v2 \
  --from 1.1 \
  --to 2.0
```

#### Manual Migration Steps

**1. Update Context Syntax**

Run the migration tool or manually update all instances:
```bash
# Find all uses of old syntax
grep -r "\$ctx\." ./mappings

# Replace with new syntax
sed -i 's/\$ctx\./ctx./g' ./mappings/**/*.json
```

**2. Update Array Mappings**

Convert explicit index mappings to array patterns:

**Before:**
```json
{
  "fieldMappings": [
    {
      "sourcePath": "addresses[0].line1",
      "targetPath": "address[0].line[0]"
    },
    {
      "sourcePath": "addresses[1].line1",
      "targetPath": "address[1].line[0]"
    }
  ]
}
```

**After:**
```json
{
  "fieldMappings": [
    {
      "id": "address-mapping",
      "sourcePath": "addresses",
      "targetPath": "address",
      "dataType": "array",
      "arrayMapping": {
        "strategy": "MAP_ALL",
        "itemMappings": [
          {
            "sourcePath": "line1",
            "targetPath": "line[0]"
          }
        ]
      }
    }
  ]
}
```

**3. Update Type Conversion Handling**

Add try-catch blocks for strict mode:

```java
try {
    Patient patient = engine.jsonToFhirResource(dto, mapping, context, Patient.class);
} catch (TypeConversionException e) {
    // Handle type conversion failures
    logger.error("Type conversion failed: {}", e.getFieldId());
}
```

**4. Test Thoroughly**

```bash
# Run test suite
mvn test

# Run validation
java -jar fhir-mapper-validator.jar \
  --mappings ./mappings-v2 \
  --test-data ./test-data
```

---

## Testing Strategy

### Unit Testing

**Required Test Categories:**

1. **Transformation Tests**
   - ✅ All 12 transformation methods
   - ✅ Each data type conversion
   - ✅ Null handling
   - ✅ Array handling

2. **Expression Tests**
   - ✅ All built-in functions
   - ✅ Context variable access
   - ✅ Conditional expressions
   - ✅ Transform expressions

3. **Validation Tests**
   - ✅ Invalid FHIR paths
   - ✅ Invalid data types
   - ✅ Missing required fields
   - ✅ Duplicate field IDs

4. **Security Tests**
   - ✅ All forbidden patterns
   - ✅ Malicious expressions blocked
   - ✅ Safe expressions allowed

5. **Lookup Tests**
   - ✅ Forward lookup
   - ✅ Reverse lookup (bidirectional)
   - ✅ Default values
   - ✅ Missing codes

### Integration Testing

```java
@SpringBootTest
@TestPropertySource(locations = "classpath:test.properties")
public class FhirMappingIntegrationTest {
    
    @Autowired
    private TransformationEngine engine;
    
    @Autowired
    private MappingRegistry registry;
    
    @Test
    public void testEndToEndPatientTransformation() throws Exception {
        // Load test data
        PatientDTO dto = loadTestPatient();
        
        // Transform
        Patient patient = engine.jsonToFhirResource(
            dto,
            registry.findBySourceAndDirection("PatientDTO", JSON_TO_FHIR),
            createTestContext(),
            Patient.class
        );
        
        // Verify
        assertNotNull(patient);
        assertEquals("P12345", patient.getIdentifierFirstRep().getValue());
        assertEquals("Doe", patient.getNameFirstRep().getFamily());
        
        // Reverse transform
        PatientDTO reversed = engine.fhirToJsonObject(
            patient,
            registry.findBySourceAndDirection("Patient", FHIR_TO_JSON),
            createTestContext(),
            PatientDTO.class
        );
        
        // Verify round-trip
        assertEquals(dto.getPatientId(), reversed.getPatientId());
    }
    
    @Test
    public void testBatchTransformation() throws Exception {
        List<PatientDTO> patients = loadTestPatients(1000);
        
        long startTime = System.currentTimeMillis();
        
        List<Patient> results = engine.transformBatch(
            patients,
            "patient-json-to-fhir-v1",
            createTestContext(),
            Patient.class
        );
        
        long duration = System.currentTimeMillis() - startTime;
        
        assertEquals(1000, results.size());
        assertTrue(duration < 5000, "Batch should complete in <5s");
    }
}
```

### Performance Testing

```java
@Test
public void performanceTest() {
    int iterations = 10000;
    
    // Warm up
    for (int i = 0; i < 100; i++) {
        engine.jsonToFhirResource(testDto, mapping, context, Patient.class);
    }
    
    // Measure
    long start = System.nanoTime();
    for (int i = 0; i < iterations; i++) {
        engine.jsonToFhirResource(testDto, mapping, context, Patient.class);
    }
    long end = System.nanoTime();
    
    double avgMs = (end - start) / 1_000_000.0 / iterations;
    
    System.out.printf("Average transformation time: %.3f ms%n", avgMs);
    assertTrue(avgMs < 5.0, "Should transform in <5ms");
}
```

### Load Testing

```bash
# Using JMeter or Gatling
gatling.sh -s FhirMappingLoadTest

# Target metrics:
# - 1000 transformations/sec
# - p95 latency < 10ms
# - p99 latency < 50ms
# - 0% error rate
```

---

## Metrics and Monitoring

### Key Performance Indicators

| Metric | Target | Current | Status |
|--------|--------|---------|--------|
| Transformation Speed | >1000/sec | TBD | 🟡 |
| P95 Latency | <10ms | TBD | 🟡 |
| Memory Usage | <512MB/10k records | TBD | 🟡 |
| Test Coverage | >80% | 0% | ❌ |
| Security Scan Coverage | 100% | 100% | ✅ |
| Documentation Coverage | 100% | 60% | 🟡 |

### Monitoring Integration

```java
@Component
public class TransformationMetrics {
    private final MeterRegistry registry;
    
    public TransformationMetrics(MeterRegistry registry) {
        this.registry = registry;
    }
    
    public void recordTransformation(String resourceType, long durationMs, boolean success) {
        Timer.builder("fhir.transformation.duration")
            .tag("resource", resourceType)
            .tag("success", String.valueOf(success))
            .register(registry)
            .record(durationMs, TimeUnit.MILLISECONDS);
        
        registry.counter("fhir.transformation.total",
            "resource", resourceType,
            "success", String.valueOf(success)
        ).increment();
    }
    
    public void recordCacheHit(String mappingId) {
        registry.counter("fhir.mapping.cache.hits",
            "mapping", mappingId
        ).increment();
    }
}
```

### Logging Standards

**Log Levels:**

```java
// ERROR - Transformation failures that need attention
logger.error("Failed to transform patient {}: {}", patientId, e.getMessage(), e);

// WARN - Data quality issues, fallbacks used
logger.warn("Type conversion failed for field {}, using original value", fieldId);

// INFO - Significant events
logger.info("Loaded {} mappings and {} lookup tables", mappings.size(), lookups.size());

// DEBUG - Detailed execution flow
logger.debug("Evaluating condition '{}' for field {}", condition, fieldId);

// TRACE - Very detailed debugging
logger.trace("Path navigation: {} -> {}", sourcePath, value);
```

---

## Security Considerations

### Expression Sandbox

The JEXL engine uses **restricted permissions** to prevent dangerous operations:

```java
// Allowed
fn.concat('Hello', ' ', 'World')
ctx.organizationId
value.toUpperCase()

// Blocked
System.exit(0)                    // System access
Runtime.getRuntime().exec("...")  // Process execution
Class.forName("...")              // Reflection
new File("/etc/passwd")           // File I/O
new Socket("evil.com", 80)        // Network I/O
```

### Audit Logging

```java
@Component
public class TransformationAuditor implements TransformationHook {
    
    @Override
    public void beforeTransformation(Map<String, Object> source, TransformationContext ctx) {
        auditLog.info("TRANSFORM_START: user={}, org={}, resource={}, recordId={}",
            ctx.getUserId(),
            ctx.getOrganizationId(),
            ctx.getResourceType(),
            extractRecordId(source)
        );
    }
    
    @Override
    public void afterTransformation(Map<String, Object> result, TransformationContext ctx) {
        auditLog.info("TRANSFORM_SUCCESS: resource={}, resultId={}",
            ctx.getResourceType(),
            result.get("id")
        );
    }
    
    @Override
    public void onError(Exception e, Map<String, Object> source, TransformationContext ctx) {
        auditLog.error("TRANSFORM_FAILURE: resource={}, error={}, recordId={}",
            ctx.getResourceType(),
            e.getMessage(),
            extractRecordId(source)
        );
    }
}
```

### Data Masking

```java
public class SensitiveDataMasker implements TransformationHook {
    
    private static final Set<String> SENSITIVE_FIELDS = Set.of(
        "ssn", "taxId", "creditCard", "password"
    );
    
    @Override
    public void beforeTransformation(Map<String, Object> source, TransformationContext ctx) {
        // Mask sensitive fields in logs
        SENSITIVE_FIELDS.forEach(field -> {
            if (source.containsKey(field)) {
                source.put(field, "***MASKED***");
            }
        });
    }
}
```

---

## Configuration Management

### Environment-Specific Settings

```yaml
# application.yml
fhir:
  mapper:
    mappings-path: ${MAPPINGS_PATH:/opt/fhir-mapper/mappings}
    strict-validation: ${STRICT_VALIDATION:true}
    strict-type-conversion: ${STRICT_TYPE_CONVERSION:false}
    cache:
      enabled: true
      max-size: 100
      expire-after-access: 1h
    performance:
      parallel-batch-processing: true
      batch-size: 1000
    security:
      expression-timeout-ms: 5000
      max-expression-length: 1000
```

### Configuration Bean

```java
@Configuration
@ConfigurationProperties(prefix = "fhir.mapper")
public class FhirMapperConfig {
    
    private String mappingsPath;
    private boolean strictValidation = true;
    private boolean strictTypeConversion = false;
    private CacheConfig cache = new CacheConfig();
    private PerformanceConfig performance = new PerformanceConfig();
    private SecurityConfig security = new SecurityConfig();
    
    @Bean
    public MappingLoader mappingLoader(FhirContext fhirContext) {
        return new MappingLoader(mappingsPath, strictValidation, fhirContext);
    }
    
    @Bean
    public TransformationEngine transformationEngine(
            MappingRegistry registry,
            FhirContext fhirContext) {
        return new TransformationEngine(registry, fhirContext)
            .setStrictTypeConversion(strictTypeConversion)
            .setCacheConfig(cache)
            .setPerformanceConfig(performance)
            .setSecurityConfig(security);
    }
    
    // Getters and setters...
}
```

---

## Troubleshooting Guide

### Common Issues

#### Issue: "Lookup table not found: gender-lookup"

**Cause:** Lookup table not loaded or incorrect ID

**Solution:**
```bash
# Check lookup files exist
ls -la mappings/lookups/

# Verify ID in lookup file matches
grep '"id"' mappings/lookups/gender-lookup.json

# Check MappingLoader logs
grep "Loaded lookup" application.log
```

---

#### Issue: "Expression evaluation failed: undefined variable 'ctx'"

**Cause:** Using old `$ctx.` syntax or context not set

**Solution:**
```json
// Update expression
{
  "transformExpression": "fn.concat('Organization/', ctx.organizationId)"
}
```

```java
// Ensure context is set
TransformationContext context = new TransformationContext();
context.setOrganizationId("org-123"); // Must set before use
```

---

#### Issue: "Type conversion failed silently"

**Cause:** Non-strict mode (default in 1.0)

**Solution:**
```java
// Enable strict mode
TransformationEngine engine = new TransformationEngine(registry)
    .setStrictTypeConversion(true);

// Or handle conversion errors
try {
    Patient patient = engine.jsonToFhirResource(...);
} catch (TypeConversionException e) {
    logger.error("Field {} conversion failed: {}", e.getFieldId(), e.getMessage());
}
```

---

#### Issue: "Performance degradation with large batches"

**Cause:** No caching, sequential processing

**Solution:**
```java
// Use batch API (Phase 2)
List<Patient> results = engine.transformBatch(
    sources,
    mappingId,
    context,
    Patient.class
);

// Or implement caching
CompiledMapping compiled = engine.compile(mapping);
for (PatientDTO dto : patients) {
    Patient p = compiled.execute(dto, context, Patient.class);
}
```

---

#### Issue: "Validation errors at startup"

**Cause:** Invalid FHIR paths or expressions

**Solution:**
```bash
# Dry-run validation
java -jar fhir-mapper-validator.jar \
  --mappings ./mappings \
  --report validation-report.json

# Check specific mapping
ValidationResult result = loader.validateOnly();
if (!result.isValid()) {
    result.getErrors().forEach(error -> 
        System.err.println(error.getContext() + ": " + error.getMessage())
    );
}
```

---

## FAQ

### Q: Can I use R5 instead of R4?

**A:** Yes, specify FHIR version when creating context:

```java
FhirContext fhirContext = FhirContext.forR5();
MappingLoader loader = new MappingLoader("./mappings", true, fhirContext);
TransformationEngine engine = new TransformationEngine(registry, fhirContext);
```

---

### Q: How do I handle large files (>1GB)?

**A:** Use streaming approach:

```java
// Phase 2 feature
StreamingTransformer streamer = new StreamingTransformer(engine, mapping);

try (Stream<PatientDTO> patients = loadPatientsAsStream("large-file.json")) {
    patients.forEach(dto -> {
        try {
            Patient patient = streamer.transform(dto, context);
            fhirClient.create().resource(patient).execute();
        } catch (Exception e) {
            logger.error("Failed to process patient: {}", dto.getPatientId(), e);
        }
    });
}
```

---

### Q: Can I map custom extensions?

**A:** Yes, use nested path notation:

```json
{
  "sourcePath": "customField",
  "targetPath": "extension[0].url",
  "defaultValue": "http://example.com/fhir/StructureDefinition/custom"
},
{
  "sourcePath": "customField",
  "targetPath": "extension[0].valueString",
  "dataType": "string"
}
```

---

### Q: How do I debug mapping issues?

**A:** Enable debug logging:

```properties
# logback.xml
<logger name="com.fhir.mapper" level="DEBUG"/>

# Or programmatically
Logger logger = LoggerFactory.getLogger("com.fhir.mapper");
((ch.qos.logback.classic.Logger) logger).setLevel(Level.DEBUG);
```

Then check logs for:
- Expression evaluation details
- Path navigation steps
- Lookup table queries
- Condition evaluation results

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

## Support

### Getting Help

- 📖 **Documentation**: [link to docs]
- 💬 **Community Forum**: [link to forum]
- 🐛 **Issue Tracker**: [GitHub Issues](link)
- 📧 **Email Support**: support@example.com
- 💼 **Enterprise Support**: enterprise@example.com

### Reporting Bugs

Use issue template:
```markdown
**Version**: 1.0.0
**FHIR Version**: R4
**Environment**: Production/Development

**Description**:
Clear description of the issue

**Steps to Reproduce**:
1. Load mapping X
2. Transform data Y
3. Observe error Z

**Expected Behavior**:
What should happen

**Actual Behavior**:
What actually happens

**Mapping Configuration**:
```json
{...}
```

**Error Logs**:
```
Stack trace here
```

**Workaround** (if any):
How you worked around it
```
```

---

## Glossary

| Term | Definition |
|------|------------|
| **FHIR** | Fast Healthcare Interoperability Resources - HL7 standard |
| **HAPI** | HL7 API - Java implementation of FHIR |
| **JEXL** | Java Expression Language - Apache Commons expression engine |
| **DTO** | Data Transfer Object - POJO for data transfer |
| **Resource Mapping** | Configuration file defining transformations |
| **Field Mapping** | Individual field transformation rules |
| **Lookup Table** | Code system translation table |
| **Context** | Runtime variables and settings |
| **Transform Expression** | JEXL expression to modify values |
| **Condition** | JEXL boolean expression for conditional mapping |
| **Path** | Dot notation to access nested fields (e.g., `name[0].given[0]`) |

---

## Changelog

### Version 1.0.0-SNAPSHOT (Current)

**Initial Release**
- ✅ Declarative JSON mapping configuration
- ✅ 12 transformation methods (JSON ↔ FHIR)
- ✅ JEXL expression support
- ✅ Code lookup tables
- ✅ Context variables
- ✅ HAPI FHIR validation
- ✅ Security scanning
- ✅ Hot reload support

**Known Issues**
- Debug logging in production code
- No unit tests
- Silent type conversion failures
- Limited array mapping

---

### Version 1.1.0 (Planned Q1 2025)

**Features**
- ✅ Enhanced error context
- ✅ Strict type conversion mode
- ✅ Compiled mapping cache
- ✅ Batch transformation API
- ✅ Transformation hooks

**Improvements**
- ✅ All debug logging removed
- ✅ 80%+ test coverage
- ✅ Performance optimizations
- ✅ Better error messages

**Deprecations**
- ⚠️ `$ctx.` string replacement syntax
- ⚠️ `resolveContextInExpression()` method

---

### Version 2.0.0 (Planned Q3 2025)

**Breaking Changes**
- ❌ Removed deprecated `$ctx.` syntax
- ❌ Strict type conversion by default
- ❌ New array mapping configuration

**Features**
- ✅ Enhanced path navigation
- ✅ Array mapping patterns
- ✅ Fluent builder API
- ✅ Advanced lookup tables
- ✅ Mapping visualization

---

## License

http://www.apache.org/licenses/LICENSE-2.0

---

## Acknowledgments

- **HAPI FHIR** team for excellent FHIR Java implementation
- **Apache Commons** for JEXL expression engine
- **Jackson** for JSON processing
- Contributors and community members

---

**Document Version**: 1.0  
**Last Updated**: November 2025  
**Maintainers**: Pradeep Kumara Krishnegowda