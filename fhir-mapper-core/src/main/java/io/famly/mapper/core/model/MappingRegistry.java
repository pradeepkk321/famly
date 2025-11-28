package com.fhir.mapper.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Registry holding all loaded mappings and lookup tables
 * Enhanced with unique ID/name validation and findByName method
 */
public class MappingRegistry {
    private List<ResourceMapping> resourceMappings;
    private Map<String, CodeLookupTable> lookupTables; // Global lookup tables
    private String fhirVersion;       // R4, R5, etc.
    private long loadedTimestamp;
    
    // Indexes for fast lookup
    private Map<String, ResourceMapping> mappingById;
    private Map<String, ResourceMapping> mappingByName;

    public MappingRegistry() {
        this.loadedTimestamp = System.currentTimeMillis();
        this.lookupTables = new HashMap<>();
        this.mappingById = new HashMap<>();
        this.mappingByName = new HashMap<>();
    }

    public List<ResourceMapping> getResourceMappings() { 
        return resourceMappings; 
    }
    
    public void setResourceMappings(List<ResourceMapping> resourceMappings) { 
        this.resourceMappings = resourceMappings;
        rebuildIndexes();
    }

    public Map<String, CodeLookupTable> getLookupTables() { 
        return lookupTables; 
    }
    
    public void setLookupTables(Map<String, CodeLookupTable> lookupTables) { 
        this.lookupTables = lookupTables; 
    }

    public String getFhirVersion() { 
        return fhirVersion; 
    }
    
    public void setFhirVersion(String fhirVersion) { 
        this.fhirVersion = fhirVersion; 
    }

    public long getLoadedTimestamp() { 
        return loadedTimestamp; 
    }

    /**
     * Find mapping by ID (fast - O(1))
     */
    public ResourceMapping findById(String id) {
        return mappingById.get(id);
    }
    
    /**
     * Find mapping by name (fast - O(1))
     */
    public ResourceMapping findByName(String name) {
        return mappingByName.get(name);
    }

    /**
     * Find mapping by source type and direction
     */
    public ResourceMapping findBySourceAndDirection(String sourceType, MappingDirection direction) {
        return resourceMappings.stream()
            .filter(m -> m.getSourceType().equals(sourceType) && m.getDirection() == direction)
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Find all mappings for a source type (both directions)
     */
    public List<ResourceMapping> findBySourceType(String sourceType) {
        return resourceMappings.stream()
            .filter(m -> m.getSourceType().equals(sourceType))
            .collect(Collectors.toList());
    }
    
    /**
     * Find all mappings for a target type
     */
    public List<ResourceMapping> findByTargetType(String targetType) {
        return resourceMappings.stream()
            .filter(m -> m.getTargetType().equals(targetType))
            .collect(Collectors.toList());
    }
    
    /**
     * Find all mappings by direction
     */
    public List<ResourceMapping> findByDirection(MappingDirection direction) {
        return resourceMappings.stream()
            .filter(m -> m.getDirection() == direction)
            .collect(Collectors.toList());
    }

    /**
     * Get lookup table by ID
     */
    public CodeLookupTable getLookupTable(String id) {
        return lookupTables.get(id);
    }

    /**
     * Add lookup table
     */
    public void addLookupTable(CodeLookupTable table) {
        lookupTables.put(table.getId(), table);
    }
    
    /**
     * Add resource mapping with index update
     */
    public void addResourceMapping(ResourceMapping mapping) {
        if (resourceMappings == null) {
            resourceMappings = new java.util.ArrayList<>();
        }
        resourceMappings.add(mapping);
        
        // Update indexes
        if (mapping.getId() != null) {
            mappingById.put(mapping.getId(), mapping);
        }
        if (mapping.getName() != null) {
            mappingByName.put(mapping.getName(), mapping);
        }
    }
    
    /**
     * Rebuild indexes after bulk updates
     */
    private void rebuildIndexes() {
        mappingById.clear();
        mappingByName.clear();
        
        if (resourceMappings != null) {
            for (ResourceMapping mapping : resourceMappings) {
                if (mapping.getId() != null) {
                    mappingById.put(mapping.getId(), mapping);
                }
                if (mapping.getName() != null) {
                    mappingByName.put(mapping.getName(), mapping);
                }
            }
        }
    }
    
    /**
     * Get all mapping IDs
     */
    public List<String> getAllMappingIds() {
        return resourceMappings.stream()
            .map(ResourceMapping::getId)
            .collect(Collectors.toList());
    }
    
    /**
     * Get all mapping names
     */
    public List<String> getAllMappingNames() {
        return resourceMappings.stream()
            .map(ResourceMapping::getName)
            .collect(Collectors.toList());
    }
    
    /**
     * Check if mapping exists by ID
     */
    public boolean hasMappingWithId(String id) {
        return mappingById.containsKey(id);
    }
    
    /**
     * Check if mapping exists by name
     */
    public boolean hasMappingWithName(String name) {
        return mappingByName.containsKey(name);
    }
    
    /**
     * Get registry statistics
     */
    public RegistryStats getStats() {
        return new RegistryStats(
            resourceMappings != null ? resourceMappings.size() : 0,
            lookupTables.size(),
            (int) resourceMappings.stream().filter(m -> m.getDirection() == MappingDirection.JSON_TO_FHIR).count(),
            (int) resourceMappings.stream().filter(m -> m.getDirection() == MappingDirection.FHIR_TO_JSON).count()
        );
    }
    
    @Override
    public String toString() {
        return "MappingRegistry{" +
            "fhirVersion='" + fhirVersion + '\'' +
            ", resourceMappingsCount=" + (resourceMappings != null ? resourceMappings.size() : 0) +
            ", lookupTablesCount=" + (lookupTables != null ? lookupTables.size() : 0) +
            ", loadedTimestamp=" + loadedTimestamp +
            '}';
    }
    
    /**
     * Registry statistics
     */
    public static class RegistryStats {
        private final int totalMappings;
        private final int totalLookupTables;
        private final int jsonToFhirMappings;
        private final int fhirToJsonMappings;
        
        public RegistryStats(int totalMappings, int totalLookupTables, 
                           int jsonToFhirMappings, int fhirToJsonMappings) {
            this.totalMappings = totalMappings;
            this.totalLookupTables = totalLookupTables;
            this.jsonToFhirMappings = jsonToFhirMappings;
            this.fhirToJsonMappings = fhirToJsonMappings;
        }
        
        public int getTotalMappings() { return totalMappings; }
        public int getTotalLookupTables() { return totalLookupTables; }
        public int getJsonToFhirMappings() { return jsonToFhirMappings; }
        public int getFhirToJsonMappings() { return fhirToJsonMappings; }
        
        @Override
        public String toString() {
            return String.format(
                "Total Mappings: %d (JSON→FHIR: %d, FHIR→JSON: %d), Lookup Tables: %d",
                totalMappings, jsonToFhirMappings, fhirToJsonMappings, totalLookupTables
            );
        }
    }
}