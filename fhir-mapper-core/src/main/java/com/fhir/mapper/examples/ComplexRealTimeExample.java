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
		String inputJson = getComplexInput(); // Your complex JSON above
//		Patient patient = engine.jsonToFhirResource(inputJson, mapping, context, Patient.class);
		
		
		System.out.println("\n\n Patient JSON: \n " + engine.jsonToFhirJson(inputJson, mapping, context));
		
		// Verify results
//		System.out.println("Patient ID: " + patient.getIdentifierFirstRep().getValue());
//		System.out.println("Name: " + patient.getNameFirstRep().getNameAsSingleString());
//		System.out.println("Gender: " + patient.getGender());
//		System.out.println("Extensions: " + patient.getExtension().size());
	}
	
	public static String getComplexInput() {
		return "{\r\n"
				+ "    \"patientId\": \"MRN-12345678\",\r\n"
				+ "    \"ssn\": \"123-45-6789\",\r\n"
				+ "    \"firstName\": \"Maria\",\r\n"
				+ "    \"middleName\": \"Isabella\",\r\n"
				+ "    \"lastName\": \"Garcia\",\r\n"
				+ "    \"suffix\": \"Jr.\",\r\n"
				+ "    \"gender\": \"F\",\r\n"
				+ "    \"dateOfBirth\": \"1985-03-15\",\r\n"
				+ "    \"maritalStatus\": \"M\",\r\n"
				+ "    \"race\": \"2054-5\",\r\n"
				+ "    \"ethnicity\": \"2135-2\",\r\n"
				+ "    \"preferredLanguage\": \"es\",\r\n"
				+ "    \"addresses\": [\r\n"
				+ "      {\r\n"
				+ "        \"type\": \"HOME\",\r\n"
				+ "        \"line1\": \"123 Main Street\",\r\n"
				+ "        \"line2\": \"Apt 4B\",\r\n"
				+ "        \"city\": \"Boston\",\r\n"
				+ "        \"state\": \"MA\",\r\n"
				+ "        \"zip\": \"02101\",\r\n"
				+ "        \"country\": \"USA\",\r\n"
				+ "        \"isPrimary\": true\r\n"
				+ "      },\r\n"
				+ "      {\r\n"
				+ "        \"type\": \"WORK\",\r\n"
				+ "        \"line1\": \"456 Business Ave\",\r\n"
				+ "        \"city\": \"Cambridge\",\r\n"
				+ "        \"state\": \"MA\",\r\n"
				+ "        \"zip\": \"02139\",\r\n"
				+ "        \"country\": \"USA\",\r\n"
				+ "        \"isPrimary\": false\r\n"
				+ "      }\r\n"
				+ "    ],\r\n"
				+ "    \"contacts\": [\r\n"
				+ "      {\r\n"
				+ "        \"type\": \"MOBILE\",\r\n"
				+ "        \"value\": \"617-555-1234\",\r\n"
				+ "        \"isPrimary\": true\r\n"
				+ "      },\r\n"
				+ "      {\r\n"
				+ "        \"type\": \"HOME\",\r\n"
				+ "        \"value\": \"617-555-5678\",\r\n"
				+ "        \"isPrimary\": false\r\n"
				+ "      },\r\n"
				+ "      {\r\n"
				+ "        \"type\": \"EMAIL\",\r\n"
				+ "        \"value\": \"maria.garcia@email.com\",\r\n"
				+ "        \"isPrimary\": true\r\n"
				+ "      }\r\n"
				+ "    ],\r\n"
				+ "    \"emergencyContacts\": [\r\n"
				+ "      {\r\n"
				+ "        \"name\": \"Juan Garcia\",\r\n"
				+ "        \"relationship\": \"SPOUSE\",\r\n"
				+ "        \"phone\": \"617-555-9999\"\r\n"
				+ "      }\r\n"
				+ "    ],\r\n"
				+ "    \"insurance\": {\r\n"
				+ "      \"memberId\": \"INS-987654\",\r\n"
				+ "      \"groupNumber\": \"GRP-12345\",\r\n"
				+ "      \"payerName\": \"Blue Cross Blue Shield\",\r\n"
				+ "      \"payerId\": \"BCBS-MA\",\r\n"
				+ "      \"coverageType\": \"PRIMARY\"\r\n"
				+ "    },\r\n"
				+ "    \"demographics\": {\r\n"
				+ "      \"birthSex\": \"F\",\r\n"
				+ "      \"genderIdentity\": \"female\",\r\n"
				+ "      \"sexualOrientation\": \"heterosexual\",\r\n"
				+ "      \"deceasedFlag\": false\r\n"
				+ "    },\r\n"
				+ "    \"identifiers\": [\r\n"
				+ "      {\r\n"
				+ "        \"type\": \"DL\",\r\n"
				+ "        \"value\": \"S12345678\",\r\n"
				+ "        \"state\": \"MA\"\r\n"
				+ "      },\r\n"
				+ "      {\r\n"
				+ "        \"type\": \"PASSPORT\",\r\n"
				+ "        \"value\": \"123456789\"\r\n"
				+ "      }\r\n"
				+ "    ]\r\n"
				+ "  }";
	}

}
