package com.fhir.mapper.examples;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hl7.fhir.r4.model.Patient;

import com.fhir.mapper.engine.TransformationEngine;
import com.fhir.mapper.loader.MappingLoader;
import com.fhir.mapper.model.MappingDirection;
import com.fhir.mapper.model.MappingRegistry;
import com.fhir.mapper.model.ResourceMapping;
import com.fhir.mapper.model.TransformationContext;

import ca.uhn.fhir.context.FhirContext;

/**
 * Comprehensive examples of the new simplified API
 */
public class TransformationExamples {

    public static void main(String[] args) throws Exception {
        // Setup
        FhirContext fhirContext = FhirContext.forR4();
        MappingLoader loader = new MappingLoader("./mappings", true, fhirContext);
        MappingRegistry registry = loader.loadAll();
        TransformationEngine engine = new TransformationEngine(registry, fhirContext);
        
        // Create transformation context
        TransformationContext context = new TransformationContext();
        context.setOrganizationId("org-123");
        context.setFacilityId("facility-456");
        context.getSettings().put("identifierSystem", "urn:oid:2.16.840.1.113883.4.1");

        // Get mappings - no direction needed in method call!
        ResourceMapping jsonToFhirMapping = registry.findBySourceAndDirection(
            "PatientDTO", MappingDirection.JSON_TO_FHIR);
        ResourceMapping fhirToJsonMapping = registry.findBySourceAndDirection(
            "Patient", MappingDirection.FHIR_TO_JSON);

        // JSON to FHIR Examples
        example1_JsonStringToFhirMap(engine, jsonToFhirMapping, context);
        example2_JsonStringToFhirJson(engine, jsonToFhirMapping, context);
        example3_JsonStringToHapiResource(engine, jsonToFhirMapping, context);
        example4_PojoToFhirMap(engine, jsonToFhirMapping, context);
        example5_PojoToHapiResource(engine, jsonToFhirMapping, context);
        example6_MapToFhirJson(engine, jsonToFhirMapping, context);

        // FHIR to JSON Examples
        example7_FhirJsonToMap(engine, fhirToJsonMapping, context);
        example8_FhirJsonToJsonString(engine, fhirToJsonMapping, context);
        example9_FhirJsonToPojo(engine, fhirToJsonMapping, context);
        example10_HapiResourceToMap(engine, fhirToJsonMapping, context);
        example11_HapiResourceToJsonString(engine, fhirToJsonMapping, context);
        example12_HapiResourceToPojo(engine, fhirToJsonMapping, context);
    }

    // ========================================================================
    // JSON to FHIR Examples
    // ========================================================================

    private static void example1_JsonStringToFhirMap(TransformationEngine engine,
                                                     ResourceMapping mapping,
                                                     TransformationContext context) throws Exception {
        System.out.println("=== Example 1: JSON String → FHIR Map ===");
        
        String json = "{\"patientId\":\"P123\",\"firstName\":\"John\",\"lastName\":\"Doe\",\"gender\":\"M\"}";
        
        Map<String, Object> fhirMap = engine.jsonToFhirMap(json, mapping, context);
        
        System.out.println("Result: " + fhirMap);
        System.out.println();
    }

    private static void example2_JsonStringToFhirJson(TransformationEngine engine,
                                                      ResourceMapping mapping,
                                                      TransformationContext context) throws Exception {
        System.out.println("=== Example 2: JSON String → FHIR JSON String ===");
        
        String json = "{\"patientId\":\"P123\",\"firstName\":\"John\",\"lastName\":\"Doe\",\"gender\":\"M\"}";
        
        String fhirJson = engine.jsonToFhirJson(json, mapping, context);
        
        System.out.println("Result: " + fhirJson);
        System.out.println();
    }

    private static void example3_JsonStringToHapiResource(TransformationEngine engine,
                                                          ResourceMapping mapping,
                                                          TransformationContext context) throws Exception {
        System.out.println("=== Example 3: JSON String → HAPI Patient Resource ===");
        
        String json = "{\"patientId\":\"P123\",\"firstName\":\"John\",\"lastName\":\"Doe\",\"gender\":\"M\"}";
        
        Patient patient = engine.jsonToFhirResource(json, mapping, context, Patient.class);
        
        System.out.println("Patient ID: " + patient.getIdentifierFirstRep().getValue());
        System.out.println("Patient Name: " + patient.getNameFirstRep().getNameAsSingleString());
        System.out.println("Patient Gender: " + patient.getGender());
        System.out.println();
    }

    private static void example4_PojoToFhirMap(TransformationEngine engine,
                                               ResourceMapping mapping,
                                               TransformationContext context) throws Exception {
        System.out.println("=== Example 4: POJO → FHIR Map ===");
        
        PatientDTO patient = new PatientDTO();
        patient.setPatientId("P456");
        patient.setFirstName("Jane");
        patient.setLastName("Smith");
        patient.setGender("F");
        
        Map<String, Object> fhirMap = engine.jsonToFhirMap(patient, mapping, context);
        
        System.out.println("Result: " + fhirMap);
        System.out.println();
    }

    private static void example5_PojoToHapiResource(TransformationEngine engine,
                                                    ResourceMapping mapping,
                                                    TransformationContext context) throws Exception {
        System.out.println("=== Example 5: POJO → HAPI Patient Resource ===");
        
        PatientDTO patientDTO = new PatientDTO();
        patientDTO.setPatientId("P789");
        patientDTO.setFirstName("Alice");
        patientDTO.setLastName("Johnson");
        patientDTO.setGender("F");
        
        Patient patient = engine.jsonToFhirResource(patientDTO, mapping, context, Patient.class);
        
        System.out.println("Patient ID: " + patient.getIdentifierFirstRep().getValue());
        System.out.println("Patient Name: " + patient.getNameFirstRep().getNameAsSingleString());
        System.out.println();
    }

    private static void example6_MapToFhirJson(TransformationEngine engine,
                                               ResourceMapping mapping,
                                               TransformationContext context) throws Exception {
        System.out.println("=== Example 6: Map → FHIR JSON ===");
        
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("patientId", "P999");
        dataMap.put("firstName", "Bob");
        dataMap.put("lastName", "Williams");
        dataMap.put("gender", "M");
        
        String fhirJson = engine.jsonToFhirJson(dataMap, mapping, context);
        
        System.out.println("Result: " + fhirJson);
        System.out.println();
    }

    // ========================================================================
    // FHIR to JSON Examples
    // ========================================================================

    private static void example7_FhirJsonToMap(TransformationEngine engine,
                                               ResourceMapping mapping,
                                               TransformationContext context) throws Exception {
        System.out.println("=== Example 7: FHIR JSON → JSON Map ===");
        
        String fhirJson = "{\"resourceType\":\"Patient\",\"identifier\":[{\"value\":\"P111\"}]," +
                         "\"name\":[{\"family\":\"Brown\",\"given\":[\"Charlie\"]}],\"gender\":\"male\"}";
        
        Map<String, Object> jsonMap = engine.fhirToJsonMap(fhirJson, mapping, context);
        
        System.out.println("Result: " + jsonMap);
        System.out.println();
    }

    private static void example8_FhirJsonToJsonString(TransformationEngine engine,
                                                      ResourceMapping mapping,
                                                      TransformationContext context) throws Exception {
        System.out.println("=== Example 8: FHIR JSON → JSON String ===");
        
        String fhirJson = "{\"resourceType\":\"Patient\",\"identifier\":[{\"value\":\"P222\"}]," +
                         "\"name\":[{\"family\":\"Davis\",\"given\":[\"Diana\"]}],\"gender\":\"female\"}";
        
        String jsonString = engine.fhirToJsonString(fhirJson, mapping, context);
        
        System.out.println("Result: " + jsonString);
        System.out.println();
    }

    private static void example9_FhirJsonToPojo(TransformationEngine engine,
                                                ResourceMapping mapping,
                                                TransformationContext context) throws Exception {
        System.out.println("=== Example 9: FHIR JSON → POJO ===");
        
        String fhirJson = "{\"resourceType\":\"Patient\",\"identifier\":[{\"value\":\"P333\"}]," +
                         "\"name\":[{\"family\":\"Evans\",\"given\":[\"Eve\"]}],\"gender\":\"female\"}";
        
        PatientDTO patient = engine.fhirToJsonObject(fhirJson, mapping, context, PatientDTO.class);
        
        System.out.println("Patient: " + patient);
        System.out.println();
    }

    private static void example10_HapiResourceToMap(TransformationEngine engine,
                                                    ResourceMapping mapping,
                                                    TransformationContext context) throws Exception {
        System.out.println("=== Example 10: HAPI Resource → JSON Map ===");
        
        Patient hapiPatient = new Patient();
        hapiPatient.addIdentifier().setValue("P444");
        hapiPatient.addName().setFamily("Foster").addGiven("Frank");
        hapiPatient.setGender(org.hl7.fhir.r4.model.Enumerations.AdministrativeGender.MALE);
        
        Map<String, Object> jsonMap = engine.fhirToJsonMap(hapiPatient, mapping, context);
        
        System.out.println("Result: " + jsonMap);
        System.out.println();
    }

    private static void example11_HapiResourceToJsonString(TransformationEngine engine,
                                                           ResourceMapping mapping,
                                                           TransformationContext context) throws Exception {
        System.out.println("=== Example 11: HAPI Resource → JSON String ===");
        
        Patient hapiPatient = new Patient();
        hapiPatient.addIdentifier().setValue("P555");
        hapiPatient.addName().setFamily("Green").addGiven("Grace");
        hapiPatient.setGender(org.hl7.fhir.r4.model.Enumerations.AdministrativeGender.FEMALE);
        
        String jsonString = engine.fhirToJsonString(hapiPatient, mapping, context);
        
        System.out.println("Result: " + jsonString);
        System.out.println();
    }

    private static void example12_HapiResourceToPojo(TransformationEngine engine,
                                                     ResourceMapping mapping,
                                                     TransformationContext context) throws Exception {
        System.out.println("=== Example 12: HAPI Resource → POJO ===");
        
        Patient hapiPatient = new Patient();
        hapiPatient.addIdentifier().setValue("P666");
        hapiPatient.addName().setFamily("Harris").addGiven("Henry");
        hapiPatient.setGender(org.hl7.fhir.r4.model.Enumerations.AdministrativeGender.MALE);
        
        PatientDTO patient = engine.fhirToJsonObject(hapiPatient, mapping, context, PatientDTO.class);
        
        System.out.println("Patient: " + patient);
        System.out.println();
    }
}

/**
 * Example POJO class
 */
class PatientDTO {
    private String patientId;
    private String firstName;
    private String lastName;
    private String gender;
    private String dateOfBirth;

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    @Override
    public String toString() {
        return "PatientDTO{" +
            "patientId='" + patientId + '\'' +
            ", firstName='" + firstName + '\'' +
            ", lastName='" + lastName + '\'' +
            ", gender='" + gender + '\'' +
            ", dateOfBirth='" + dateOfBirth + '\'' +
            '}';
    }
}

/**
 * REST API Integration Example
 */
class RestApiExample {
    /*
    @RestController
    @RequestMapping("/api/fhir")
    public class FhirTransformController {
        
        @Autowired private TransformationEngine engine;
        @Autowired private MappingRegistry registry;
        
        // JSON → FHIR endpoints
        
        @PostMapping("/patient")
        public Patient createPatient(@RequestBody PatientDTO patient) throws Exception {
            TransformationContext ctx = buildContext();
            ResourceMapping mapping = registry.findBySourceAndDirection(
                "PatientDTO", MappingDirection.JSON_TO_FHIR);
            
            return engine.jsonToFhirResource(patient, mapping, ctx, Patient.class);
        }
        
        @PostMapping("/patient/json")
        public String createPatientJson(@RequestBody String json) throws Exception {
            TransformationContext ctx = buildContext();
            ResourceMapping mapping = registry.findBySourceAndDirection(
                "PatientDTO", MappingDirection.JSON_TO_FHIR);
            
            return engine.jsonToFhirJson(json, mapping, ctx);
        }
        
        // FHIR → JSON endpoints
        
        @GetMapping("/patient/{id}")
        public PatientDTO getPatient(@PathVariable String id) throws Exception {
            // Fetch HAPI Patient from FHIR server
            Patient hapiPatient = fhirClient.read().resource(Patient.class).withId(id).execute();
            
            TransformationContext ctx = buildContext();
            ResourceMapping mapping = registry.findBySourceAndDirection(
                "Patient", MappingDirection.FHIR_TO_JSON);
            
            return engine.fhirToJsonObject(hapiPatient, mapping, ctx, PatientDTO.class);
        }
        
        @GetMapping("/patient/{id}/json")
        public String getPatientJson(@PathVariable String id) throws Exception {
            Patient hapiPatient = fhirClient.read().resource(Patient.class).withId(id).execute();
            
            TransformationContext ctx = buildContext();
            ResourceMapping mapping = registry.findBySourceAndDirection(
                "Patient", MappingDirection.FHIR_TO_JSON);
            
            return engine.fhirToJsonString(hapiPatient, mapping, ctx);
        }
        
        private TransformationContext buildContext() {
            TransformationContext ctx = new TransformationContext();
            // Set from security context, headers, etc.
            ctx.setOrganizationId(SecurityContextHolder.getContext().getOrganizationId());
            return ctx;
        }
    }
    */
}

/**
 * Batch Processing Example
 */
class BatchExample {
    
    public static void batchTransform(TransformationEngine engine,
                                     MappingRegistry registry,
                                     List<PatientDTO> patients) throws Exception {
        ResourceMapping mapping = registry.findBySourceAndDirection(
            "PatientDTO", MappingDirection.JSON_TO_FHIR);
        
        TransformationContext context = new TransformationContext();
        context.setOrganizationId("org-batch");
        
        List<Patient> fhirPatients = new ArrayList<>();
        
        for (PatientDTO patient : patients) {
            try {
                Patient fhirPatient = engine.jsonToFhirResource(
                    patient, mapping, context, Patient.class);
                fhirPatients.add(fhirPatient);
                
            } catch (Exception e) {
                System.err.println("Failed patient: " + patient.getPatientId() + " - " + e.getMessage());
            }
        }
        
        System.out.println("Successfully transformed " + fhirPatients.size() + " patients");
    }
}