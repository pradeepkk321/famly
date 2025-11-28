package com.fhir.mapper.tests;

import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import com.fhir.mapper.engine.TransformationEngine;
import com.fhir.mapper.loader.MappingLoader;
import com.fhir.mapper.model.MappingRegistry;
import com.fhir.mapper.model.ResourceMapping;
import com.fhir.mapper.model.TransformationContext;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

/**
 * Comprehensive real-time tests covering all mapping features:
 * 
 * JSON to FHIR:
 * 1. Patient - Complex with extensions, lookups, conditions, transformations
 * 2. Observation - Nested structures, context variables, required fields
 * 3. Medication - Arrays, default values, multiple lookups
 * 
 * FHIR to JSON:
 * 4. Patient - Reverse transformation with bidirectional lookups
 * 5. Encounter - Complex nested extraction
 * 6. Condition - CodeableConcept handling, multiple codings
 */
public class ComprehensiveTransformationTests {
    
    private MappingRegistry registry;
    private TransformationEngine engine;
    private TransformationContext context;
    
    @BeforeEach
    public void setup() throws Exception {
        MappingLoader loader = new MappingLoader("./src/test/resources/test-mappings");
        registry = loader.loadAll();
        engine = new TransformationEngine(registry, FhirContext.forR4());
        
        context = new TransformationContext();
        context.setOrganizationId("org-test-123");
        context.setFacilityId("facility-456");
        context.getSettings().put("identifierSystem", "urn:oid:2.16.840.1.113883.4.1");
        context.getSettings().put("encounterSystem", "http://hospital.org/encounters");
    }
    
    // ========================================================================
    // TEST 1: Patient JSON to FHIR - Complex Transformation
    // ========================================================================
    
    @Test
    public void test1_PatientJsonToFhir_Complex() throws Exception {
        // Tests: lookups, conditions, transformations, extensions, context variables
        
        String patientJson = """
        {
          "patientId": "P123456",
          "ssn": "123-45-6789",
          "firstName": "Maria",
          "middleName": "Isabella",
          "lastName": "Garcia",
          "suffix": "Jr.",
          "gender": "F",
          "dateOfBirth": "1985-03-15",
          "maritalStatus": "M",
          "race": "2054-5",
          "ethnicity": "2135-2",
          "preferredLanguage": "es",
          "isDeceased": false,
          "emailAddress": "maria.garcia@example.com",
          "mobilePhone": "617-555-1234",
          "homeAddress": {
            "line1": "123 Main Street",
            "line2": "Apt 4B",
            "city": "Boston",
            "state": "MA",
            "zip": "02101",
            "country": "USA"
          }
        }
        """;
        
        ResourceMapping mapping = registry.findById("patient-json-to-fhir-complex-v1");
        assertNotNull(mapping, "Mapping should exist");
        
        // Enable tracing
        context.enableTracing("test-patient-1");
        
        Patient patient = engine.jsonToFhirResource(patientJson, mapping, context, Patient.class);
        
        // Assertions
        assertNotNull(patient);
        assertEquals("Patient", patient.getResourceType().name());
        
        // Identifier with context variable
        assertEquals("P123456", patient.getIdentifier().get(0).getValue());
        assertEquals("urn:oid:2.16.840.1.113883.4.1", patient.getIdentifier().get(0).getSystem());
        assertEquals("MR", patient.getIdentifier().get(0).getType().getCoding().get(0).getCode());
        
        // SSN with transformation (remove hyphens)
        assertEquals("123456789", patient.getIdentifier().get(1).getValue());
        assertEquals("http://hl7.org/fhir/sid/us-ssn", patient.getIdentifier().get(1).getSystem());
        
        // Name with conditional middle name and suffix
        assertEquals("Garcia", patient.getName().get(0).getFamily());
        assertEquals("Maria", patient.getName().get(0).getGiven().get(0).getValue());
        assertEquals("Isabella", patient.getName().get(0).getGiven().get(1).getValue());
        assertEquals("Jr.", patient.getName().get(0).getSuffix().get(0).getValue());
        
        // Gender with lookup
        assertEquals(Enumerations.AdministrativeGender.FEMALE, patient.getGender());
        
        // Birth date
        assertNotNull(patient.getBirthDate());
        
        // Marital status with lookup
        assertEquals("M", patient.getMaritalStatus().getCoding().get(0).getCode());
        
        // Deceased flag
        assertFalse(patient.getDeceasedBooleanType().booleanValue());
        
        // Telecom with condition
        assertTrue(patient.getTelecom().size() >= 2);
        assertEquals("maria.garcia@example.com", 
            patient.getTelecom().stream()
                .filter(t -> t.getSystem() == ContactPoint.ContactPointSystem.EMAIL)
                .findFirst().get().getValue());
        
        // Address
        assertEquals("123 Main Street", patient.getAddress().get(0).getLine().get(0).getValue());
        assertEquals("Apt 4B", patient.getAddress().get(0).getLine().get(1).getValue());
        assertEquals("Boston", patient.getAddress().get(0).getCity());
        
        // US Core Race Extension
        Extension raceExt = patient.getExtension().stream()
            .filter(e -> e.getUrl().equals("http://hl7.org/fhir/us/core/StructureDefinition/us-core-race"))
            .findFirst().orElse(null);
        assertNotNull(raceExt);
        assertEquals("2054-5", 
            ((Coding)raceExt.getExtension().get(0).getValue()).getCode());
        
        // Ethnicity Extension
        Extension ethnicityExt = patient.getExtension().stream()
            .filter(e -> e.getUrl().equals("http://hl7.org/fhir/us/core/StructureDefinition/us-core-ethnicity"))
            .findFirst().orElse(null);
        assertNotNull(ethnicityExt);
        
        // Managing Organization with context
        assertEquals("Organization/org-test-123", 
            patient.getManagingOrganization().getReference());
        
        // Check trace
        if (context.isEnableTracing()) {
            context.getTrace().printTraceReport();
            assertTrue(context.getTrace().isSuccess());
            assertEquals(0, context.getTrace().failedFieldTransformationTraces().size());
        }
    }
    
    // ========================================================================
    // TEST 2: Observation JSON to FHIR - Nested Structures
    // ========================================================================
    
    @Test
    public void test2_ObservationJsonToFhir_Nested() throws Exception {
        // Tests: nested paths, required fields, numeric conversions, references
        
        String observationJson = """
        {
          "observationId": "OBS-12345",
          "patientId": "P123456",
          "encounterId": "ENC-789",
          "status": "final",
          "category": "vital-signs",
          "code": "85354-9",
          "codeDisplay": "Blood pressure",
          "effectiveDateTime": "2024-01-15T10:30:00Z",
          "systolicValue": 120,
          "systolicUnit": "mm[Hg]",
          "diastolicValue": 80,
          "diastolicUnit": "mm[Hg]",
          "interpretation": "normal",
          "performerId": "Practitioner/DR-001",
          "note": "Patient was calm during measurement"
        }
        """;
        
        ResourceMapping mapping = registry.findById("observation-json-to-fhir-v1");
        assertNotNull(mapping);
        
        context.enableTracing("test-observation-1");
        
        Observation observation = engine.jsonToFhirResource(
            observationJson, mapping, context, Observation.class);
        
//        IParser parser = FhirContext.forR4().newJsonParser();
//        String encodeResourceToString = parser.setPrettyPrint(true).encodeResourceToString(observation);
//        System.out.println("\n\n" + encodeResourceToString + "\n\n");
        
        // Assertions
        assertNotNull(observation);
        
        // Identifier
        assertEquals("OBS-12345", observation.getIdentifier().get(0).getValue());
        assertEquals("http://hospital.org/observations", 
            observation.getIdentifier().get(0).getSystem());
        
        // Status with lookup
        assertEquals(Observation.ObservationStatus.FINAL, observation.getStatus());
        
        // Subject reference
        assertEquals("Patient/P123456", observation.getSubject().getReference());
        
        // Encounter reference with condition
        assertEquals("Encounter/ENC-789", observation.getEncounter().getReference());
        
        // Category with lookup
        assertNotNull(observation.getCategory());
        assertEquals("vital-signs", 
            observation.getCategory().get(0).getCoding().get(0).getCode());
        
        // Code
        assertEquals("85354-9", observation.getCode().getCoding().get(0).getCode());
        assertEquals("http://loinc.org", observation.getCode().getCoding().get(0).getSystem());
        assertEquals("Blood pressure", observation.getCode().getCoding().get(0).getDisplay());
        
        // Component values (systolic/diastolic)
        assertEquals(2, observation.getComponent().size());
        
        Observation.ObservationComponentComponent systolic = observation.getComponent().get(0);
        assertEquals("8480-6", systolic.getCode().getCoding().get(0).getCode());
        assertEquals(120, systolic.getValueQuantity().getValue().intValue());
        assertEquals("mm[Hg]", systolic.getValueQuantity().getUnit());
        
        Observation.ObservationComponentComponent diastolic = observation.getComponent().get(1);
        assertEquals("8462-4", diastolic.getCode().getCoding().get(0).getCode());
        assertEquals(80, diastolic.getValueQuantity().getValue().intValue());
        
        // Interpretation
        assertEquals("N", 
            observation.getInterpretation().get(0).getCoding().get(0).getCode());
        
        // Performer
        assertEquals("Practitioner/DR-001", observation.getPerformer().get(0).getReference());
        
        // Note
        assertEquals("Patient was calm during measurement", 
            observation.getNote().get(0).getText());
        
        // Verify trace
        assertTrue(context.getTrace().isSuccess());
    }
    
    // ========================================================================
    // TEST 3: Medication JSON to FHIR - Arrays and Multiple Lookups
    // ========================================================================
    
    @Test
    public void test3_MedicationRequestJsonToFhir_Arrays() throws Exception {
        // Tests: array handling, multiple lookups, default values, compound expressions
        
        String medicationJson = """
        {
          "requestId": "MED-REQ-999",
          "patientId": "P123456",
          "encounterId": "ENC-789",
          "status": "active",
          "intent": "order",
          "priority": "routine",
          "medicationCode": "197361",
          "medicationName": "Lisinopril 10mg tablet",
          "dosageText": "Take 1 tablet by mouth daily",
          "route": "oral",
          "frequency": "daily",
          "quantityValue": 30,
          "quantityUnit": "tablet",
          "refills": 3,
          "substitutionAllowed": true,
          "requestedDate": "2024-01-15",
          "reasonCodes": ["I10", "E11.9"],
          "reasonTexts": ["Hypertension", "Type 2 Diabetes"],
          "notes": [
            "Take with food",
            "Monitor blood pressure weekly"
          ]
        }
        """;
        
        ResourceMapping mapping = registry.findById("medication-request-json-to-fhir-v1");
        assertNotNull(mapping);
        
        context.enableTracing("test-medication-1");
        
        MedicationRequest medRequest = engine.jsonToFhirResource(
            medicationJson, mapping, context, MedicationRequest.class);
        
      IParser parser = FhirContext.forR4().newJsonParser();
      String encodeResourceToString = parser.setPrettyPrint(true).encodeResourceToString(medRequest);
      System.out.println("\n\n" + encodeResourceToString + "\n\n");
        
        // Assertions
        assertNotNull(medRequest);
        
        // Identifier
        assertEquals("MED-REQ-999", medRequest.getIdentifier().get(0).getValue());
        
        // Status with lookup
        assertEquals(MedicationRequest.MedicationRequestStatus.ACTIVE, medRequest.getStatus());
        
        // Intent with lookup
        assertEquals(MedicationRequest.MedicationRequestIntent.ORDER, medRequest.getIntent());
        
        // Priority with lookup
        assertEquals(MedicationRequest.MedicationRequestPriority.ROUTINE, medRequest.getPriority());
        
        // Subject reference
        assertEquals("Patient/P123456", medRequest.getSubject().getReference());
        
        // Encounter reference
        assertEquals("Encounter/ENC-789", medRequest.getEncounter().getReference());
        
        // Medication (CodeableConcept)
        CodeableConcept medication = medRequest.getMedicationCodeableConcept();
        assertEquals("197361", medication.getCoding().get(0).getCode());
        assertEquals("http://www.nlm.nih.gov/research/umls/rxnorm", 
            medication.getCoding().get(0).getSystem());
        assertEquals("Lisinopril 10mg tablet", medication.getText());
        
        // Dosage instruction
        assertEquals("Take 1 tablet by mouth daily", 
            medRequest.getDosageInstruction().get(0).getText());
        assertEquals("26643006", 
            medRequest.getDosageInstruction().get(0).getRoute().getCoding().get(0).getCode());
        
        // Dispense request
        assertEquals(30, medRequest.getDispenseRequest().getQuantity().getValue().intValue());
        assertEquals("tablet", medRequest.getDispenseRequest().getQuantity().getUnit());
        assertEquals(3, medRequest.getDispenseRequest().getNumberOfRepeatsAllowed());
        
        // Substitution
        assertTrue(medRequest.getSubstitution().getAllowedBooleanType().booleanValue());
        
        // Reason codes (array handling)
        assertTrue(medRequest.getReasonCode().size() >= 2);
        assertEquals("I10", medRequest.getReasonCode().get(0).getCoding().get(0).getCode());
        assertEquals("E11.9", medRequest.getReasonCode().get(1).getCoding().get(0).getCode());
        
        // Notes (array handling)
        assertEquals(2, medRequest.getNote().size());
        assertEquals("Take with food", medRequest.getNote().get(0).getText());
        assertEquals("Monitor blood pressure weekly", medRequest.getNote().get(1).getText());
        
        // Verify trace
        assertTrue(context.getTrace().isSuccess());
    }
    
    // ========================================================================
    // TEST 4: Patient FHIR to JSON - Reverse Transformation
    // ========================================================================
    
    @Test
    public void test4_PatientFhirToJson_Reverse() throws Exception {
        // Tests: bidirectional lookups, extension extraction, array extraction
        
        Patient patient = new Patient();
        
        // Identifiers
        patient.addIdentifier()
            .setSystem("urn:oid:2.16.840.1.113883.4.1")
            .setValue("P123456")
            .setType(new CodeableConcept()
                .addCoding(new Coding()
                    .setSystem("http://terminology.hl7.org/CodeSystem/v2-0203")
                    .setCode("MR")));
        
        patient.addIdentifier()
            .setSystem("http://hl7.org/fhir/sid/us-ssn")
            .setValue("123456789");
        
        // Name
        patient.addName()
            .setUse(HumanName.NameUse.OFFICIAL)
            .setFamily("Garcia")
            .addGiven("Maria")
            .addGiven("Isabella")
            .addSuffix("Jr.");
        
        // Gender
        patient.setGender(Enumerations.AdministrativeGender.FEMALE);
        
        // Birth date
        patient.setBirthDate(java.sql.Date.valueOf("1985-03-15"));
        
        // Marital status
        patient.setMaritalStatus(new CodeableConcept()
            .addCoding(new Coding()
                .setSystem("http://terminology.hl7.org/CodeSystem/v3-MaritalStatus")
                .setCode("M")));
        
        // Telecom
        patient.addTelecom()
            .setSystem(ContactPoint.ContactPointSystem.EMAIL)
            .setValue("maria.garcia@example.com")
            .setUse(ContactPoint.ContactPointUse.HOME);
        
        patient.addTelecom()
            .setSystem(ContactPoint.ContactPointSystem.PHONE)
            .setValue("617-555-1234")
            .setUse(ContactPoint.ContactPointUse.MOBILE);
        
        // Address
        patient.addAddress()
            .setUse(Address.AddressUse.HOME)
            .addLine("123 Main Street")
            .addLine("Apt 4B")
            .setCity("Boston")
            .setState("MA")
            .setPostalCode("02101")
            .setCountry("USA");
        
        // Race extension
        Extension raceExt = new Extension()
            .setUrl("http://hl7.org/fhir/us/core/StructureDefinition/us-core-race");
        raceExt.addExtension(new Extension()
            .setUrl("ombCategory")
            .setValue(new Coding()
                .setSystem("urn:oid:2.16.840.1.113883.6.238")
                .setCode("2054-5")));
        raceExt.addExtension(new Extension()
            .setUrl("text")
            .setValue(new StringType("Black or African American")));
        patient.addExtension(raceExt);
        
        // Managing organization
        patient.setManagingOrganization(new Reference("Organization/org-test-123"));
        
        ResourceMapping mapping = registry.findById("patient-fhir-to-json-v1");
        assertNotNull(mapping);
        
        context.enableTracing("test-patient-reverse-1");
        
        // Transform
        String resultJson = engine.fhirToJsonString(patient, mapping, context);
        
        assertNotNull(resultJson);
        assertTrue(resultJson.contains("\"patientId\":\"P123456\""));
        assertTrue(resultJson.contains("\"ssn\":\"123-45-6789\""));
        assertTrue(resultJson.contains("\"firstName\":\"Maria\""));
        assertTrue(resultJson.contains("\"middleName\":\"Isabella\""));
        assertTrue(resultJson.contains("\"lastName\":\"Garcia\""));
        assertTrue(resultJson.contains("\"suffix\":\"Jr.\""));
        
        // Gender reverse lookup
        assertTrue(resultJson.contains("\"gender\":\"F\""));
        
        // Email
        assertTrue(resultJson.contains("\"emailAddress\":\"maria.garcia@example.com\""));
        
        // Address
        assertTrue(resultJson.contains("\"line1\":\"123 Main Street\""));
        assertTrue(resultJson.contains("\"line2\":\"Apt 4B\""));
        assertTrue(resultJson.contains("\"city\":\"Boston\""));
        
        // Race extraction
        assertTrue(resultJson.contains("\"race\":\"2054-5\""));
        
        // Organization ID extraction
        assertTrue(resultJson.contains("\"organizationId\":\"org-test-123\""));
        
        // Verify trace
        assertTrue(context.getTrace().isSuccess());
    }
    
    // ========================================================================
    // TEST 5: Encounter FHIR to JSON - Complex Nested Extraction
    // ========================================================================
    
    @Test
    public void test5_EncounterFhirToJson_Nested() throws Exception {
        // Tests: nested extraction, period handling, participant extraction
        
        Encounter encounter = new Encounter();
        
        // Identifier
        encounter.addIdentifier()
            .setSystem("http://hospital.org/encounters")
            .setValue("ENC-789");
        
        // Status
        encounter.setStatus(Encounter.EncounterStatus.FINISHED);
        
        // Class
        encounter.setClass_(new Coding()
            .setSystem("http://terminology.hl7.org/CodeSystem/v3-ActCode")
            .setCode("IMP")
            .setDisplay("inpatient encounter"));
        
        // Type
        encounter.addType(new CodeableConcept()
            .addCoding(new Coding()
                .setSystem("http://snomed.info/sct")
                .setCode("185347001")
                .setDisplay("Emergency visit")));
        
        // Priority
        encounter.setPriority(new CodeableConcept()
            .addCoding(new Coding()
                .setCode("EM")
                .setDisplay("Emergency")));
        
        // Subject
        encounter.setSubject(new Reference("Patient/P123456"));
        
        // Participant
        encounter.addParticipant()
            .addType(new CodeableConcept()
                .addCoding(new Coding()
                    .setCode("ATND")
                    .setDisplay("attender")))
            .setIndividual(new Reference("Practitioner/DR-001"));
        
        // Period
        encounter.setPeriod(new Period()
            .setStartElement(new DateTimeType("2024-01-15T10:00:00Z"))
            .setEndElement(new DateTimeType("2024-01-15T14:30:00Z")));
        
        // Reason
        encounter.addReasonCode(new CodeableConcept()
            .addCoding(new Coding()
                .setSystem("http://snomed.info/sct")
                .setCode("386661006")
                .setDisplay("Fever")));
        
        // Hospitalization
        encounter.setHospitalization(new Encounter.EncounterHospitalizationComponent()
            .setAdmitSource(new CodeableConcept()
                .addCoding(new Coding()
                    .setCode("emd")
                    .setDisplay("From emergency department")))
            .setDischargeDisposition(new CodeableConcept()
                .addCoding(new Coding()
                    .setCode("home")
                    .setDisplay("Home"))));
        
        // Location
        encounter.addLocation()
            .setLocation(new Reference("Location/ER-01"))
            .setStatus(Encounter.EncounterLocationStatus.COMPLETED);
        
        ResourceMapping mapping = registry.findById("encounter-fhir-to-json-v1");
        assertNotNull(mapping);
        
        context.enableTracing("test-encounter-reverse-1");
        
        String resultJson = engine.fhirToJsonString(encounter, mapping, context);
        
        assertNotNull(resultJson);
        assertTrue(resultJson.contains("\"encounterId\":\"ENC-789\""));
        assertTrue(resultJson.contains("\"status\":\"finished\""));
        assertTrue(resultJson.contains("\"classCode\":\"IMP\""));
        assertTrue(resultJson.contains("\"patientId\":\"P123456\""));
        assertTrue(resultJson.contains("\"attendingPhysicianId\":\"DR-001\""));
        assertTrue(resultJson.contains("\"admitSource\":\"emd\""));
        assertTrue(resultJson.contains("\"dischargeDisposition\":\"home\""));
        assertTrue(resultJson.contains("\"locationId\":\"ER-01\""));
        
        // Verify trace
        assertTrue(context.getTrace().isSuccess());
    }
    
    // ========================================================================
    // TEST 6: Condition FHIR to JSON - CodeableConcept Handling
    // ========================================================================
    
    @Test
    public void test6_ConditionFhirToJson_CodeableConcept() throws Exception {
        // Tests: multiple codings extraction, category handling, date extraction
        
        Condition condition = new Condition();
        
        // Identifier
        condition.addIdentifier()
            .setSystem("http://hospital.org/conditions")
            .setValue("COND-555");
        
        // Clinical status
        condition.setClinicalStatus(new CodeableConcept()
            .addCoding(new Coding()
                .setSystem("http://terminology.hl7.org/CodeSystem/condition-clinical")
                .setCode("active")));
        
        // Verification status
        condition.setVerificationStatus(new CodeableConcept()
            .addCoding(new Coding()
                .setSystem("http://terminology.hl7.org/CodeSystem/condition-ver-status")
                .setCode("confirmed")));
        
        // Category
        condition.addCategory(new CodeableConcept()
            .addCoding(new Coding()
                .setSystem("http://terminology.hl7.org/CodeSystem/condition-category")
                .setCode("encounter-diagnosis")));
        
        // Severity
        condition.setSeverity(new CodeableConcept()
            .addCoding(new Coding()
                .setSystem("http://snomed.info/sct")
                .setCode("24484000")
                .setDisplay("Severe")));
        
        // Code with multiple codings
        CodeableConcept code = new CodeableConcept();
        code.addCoding(new Coding()
            .setSystem("http://snomed.info/sct")
            .setCode("386661006")
            .setDisplay("Fever"));
        code.addCoding(new Coding()
            .setSystem("http://hl7.org/fhir/sid/icd-10")
            .setCode("R50.9")
            .setDisplay("Fever, unspecified"));
        code.setText("High fever");
        condition.setCode(code);
        
        // Body site
        condition.addBodySite(new CodeableConcept()
            .addCoding(new Coding()
                .setSystem("http://snomed.info/sct")
                .setCode("123456")
                .setDisplay("Head")));
        
        // Subject
        condition.setSubject(new Reference("Patient/P123456"));
        
        // Encounter
        condition.setEncounter(new Reference("Encounter/ENC-789"));
        
        // Onset
        condition.setOnset(new DateTimeType("2024-01-10T08:00:00Z"));
        
        // Recorded date
        condition.setRecordedDate(java.sql.Date.valueOf("2024-01-15"));
        
        // Recorder
        condition.setRecorder(new Reference("Practitioner/DR-001"));
        
        // Note
        condition.addNote(new Annotation()
            .setText("Patient reports fever for 5 days"));
        
        ResourceMapping mapping = registry.findById("condition-fhir-to-json-v1");
        assertNotNull(mapping);
        
        context.enableTracing("test-condition-reverse-1");
        
        String resultJson = engine.fhirToJsonString(condition, mapping, context);
        
        assertNotNull(resultJson);
        assertTrue(resultJson.contains("\"conditionId\":\"COND-555\""));
        assertTrue(resultJson.contains("\"clinicalStatus\":\"active\""));
        assertTrue(resultJson.contains("\"verificationStatus\":\"confirmed\""));
        assertTrue(resultJson.contains("\"category\":\"encounter-diagnosis\""));
        assertTrue(resultJson.contains("\"severity\":\"Severe\""));
        
        // Multiple codings
        assertTrue(resultJson.contains("\"snomedCode\":\"386661006\""));
        assertTrue(resultJson.contains("\"icd10Code\":\"R50.9\""));
        assertTrue(resultJson.contains("\"conditionText\":\"High fever\""));
        
        assertTrue(resultJson.contains("\"patientId\":\"P123456\""));
        assertTrue(resultJson.contains("\"encounterId\":\"ENC-789\""));
        assertTrue(resultJson.contains("\"recorderId\":\"DR-001\""));
        
        // Verify trace
        assertTrue(context.getTrace().isSuccess());
    }
}