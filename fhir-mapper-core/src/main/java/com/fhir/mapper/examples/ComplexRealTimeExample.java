package com.fhir.mapper.examples;

import org.hl7.fhir.r4.model.Patient;

import com.fhir.mapper.engine.TransformationEngine;
import com.fhir.mapper.loader.MappingLoader;
import com.fhir.mapper.model.MappingDirection;
import com.fhir.mapper.model.MappingRegistry;
import com.fhir.mapper.model.ResourceMapping;
import com.fhir.mapper.model.TransformationContext;

public class ComplexRealTimeExample {
	public static void main(String[] args) throws Exception {
		// Load mappings
		MappingLoader loader = new MappingLoader("./mappings");
		MappingRegistry registry = loader.loadAll();
		TransformationEngine engine = new TransformationEngine(registry);

		// Setup context
		TransformationContext context = new TransformationContext();
		context.setOrganizationId("org-123");
		context.getSettings().put("mrnSystem", "urn:oid:2.16.840.1.113883.4.1");

		// Get mapping
		ResourceMapping mapping = registry.findBySourceAndDirection("ComplexPatientDTO", MappingDirection.JSON_TO_FHIR);
		// Transform
		String inputJson = inputJSON(); // Your complex JSON above
		Patient patient = engine.jsonToFhirResource(inputJson, mapping, context, Patient.class);
		String fhirJson = engine.jsonToFhirJson(inputJson, mapping, context);
		System.out.println("\n\n Patient JSON: \n " + engine.jsonToFhirJson(inputJson, mapping, context));
		
		System.out.println("Output Validation Success: " + expectedOutput().equals(fhirJson));;
		
		// Verify results
		System.out.println("Patient ID: " + patient.getIdentifierFirstRep().getValue());
		System.out.println("Name: " + patient.getNameFirstRep().getNameAsSingleString());
		System.out.println("Gender: " + patient.getGender());
		System.out.println("Extensions: " + patient.getExtension().size());
	}
	
	public static String inputJSON() {
		return """
{
  "patientId": "MRN-12345678",
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
  "addresses": [
    {
      "type": "HOME",
      "line1": "123 Main Street",
      "line2": "Apt 4B",
      "city": "Boston",
      "state": "MA",
      "zip": "02101",
      "country": "USA",
      "isPrimary": true
    },
    {
      "type": "WORK",
      "line1": "456 Business Ave",
      "city": "Cambridge",
      "state": "MA",
      "zip": "02139",
      "country": "USA",
      "isPrimary": false
    }
  ],
  "contacts": [
    {
      "type": "MOBILE",
      "value": "617-555-1234",
      "isPrimary": true
    },
    {
      "type": "HOME",
      "value": "617-555-5678",
      "isPrimary": false
    },
    {
      "type": "EMAIL",
      "value": "maria.garcia@email.com",
      "isPrimary": true
    }
  ],
  "emergencyContacts": [
    {
      "name": "Juan Garcia",
      "relationship": "SPOUSE",
      "phone": "617-555-9999"
    }
  ],
  "insurance": {
    "memberId": "INS-987654",
    "groupNumber": "GRP-12345",
    "payerName": "Blue Cross Blue Shield",
    "payerId": "BCBS-MA",
    "coverageType": "PRIMARY"
  },
  "demographics": {
    "birthSex": "F",
    "genderIdentity": "female",
    "sexualOrientation": "heterosexual",
    "deceasedFlag": false
  },
  "identifiers": [
    {
      "type": "DL",
      "value": "S12345678",
      "state": "MA"
    },
    {
      "type": "PASSPORT",
      "value": "123456789"
    }
  ]
}
				""";
	}
	
	private static String expectedOutput() {
		return "{\"resourceType\":\"Patient\",\"identifier\":[{\"value\":\"MRN-12345678\",\"system\":\"urn:oid:2.16.840.1.113883.4.1\",\"type\":{\"coding\":[{\"code\":\"MR\",\"system\":\"http://terminology.hl7.org/CodeSystem/v2-0203\"}]}},{\"value\":\"123456789\",\"system\":\"http://hl7.org/fhir/sid/us-ssn\",\"type\":{\"coding\":[{\"code\":\"SS\"}]}}],\"name\":[{\"use\":\"official\",\"family\":\"Garcia\",\"given\":[\"Maria\",\"Isabella\"],\"suffix\":[\"Jr.\"]}],\"gender\":\"female\",\"birthDate\":\"1985-03-15\",\"maritalStatus\":{\"coding\":[{\"code\":\"M\",\"system\":\"http://terminology.hl7.org/CodeSystem/v3-MaritalStatus\"}]},\"deceasedBoolean\":false,\"address\":[{\"use\":\"home\",\"line\":[\"123 Main Street\",\"Apt 4B\"],\"city\":\"Boston\",\"state\":\"MA\",\"postalCode\":\"02101\",\"country\":\"USA\"}],\"telecom\":[{\"system\":\"phone\",\"value\":\"617-555-1234\",\"use\":\"mobile\"},{\"system\":\"email\",\"value\":\"maria.garcia@email.com\"}],\"communication\":[{\"language\":{\"coding\":[{\"code\":\"es\",\"system\":\"urn:ietf:bcp:47\"}]},\"preferred\":true}],\"extension\":[{\"extension\":[{\"valueCoding\":{\"code\":\"2054-5\",\"system\":\"urn:oid:2.16.840.1.113883.6.238\"},\"url\":\"ombCategory\"},{\"valueString\":\"Black or African American\",\"url\":\"text\"}],\"url\":\"http://hl7.org/fhir/us/core/StructureDefinition/us-core-race\"},{\"extension\":[{\"valueCoding\":{\"code\":\"2135-2\",\"system\":\"urn:oid:2.16.840.1.113883.6.238\"},\"url\":\"ombCategory\"},{\"valueString\":\"Hispanic or Latino\",\"url\":\"text\"}],\"url\":\"http://hl7.org/fhir/us/core/StructureDefinition/us-core-ethnicity\"},{\"valueCode\":\"F\",\"url\":\"http://hl7.org/fhir/us/core/StructureDefinition/us-core-birthsex\"}],\"managingOrganization\":{\"reference\":\"Organization/org-123\"}}";
	}

}
