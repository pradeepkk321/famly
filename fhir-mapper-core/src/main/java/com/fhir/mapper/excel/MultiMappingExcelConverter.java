package com.fhir.mapper.excel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fhir.mapper.model.FieldMapping;
import com.fhir.mapper.model.MappingDirection;
import com.fhir.mapper.model.ResourceMapping;

/**
 * Multi-mapping Excel converter.
 * 
 * Excel Format:
 * - Sheet "Config": Common metadata for all mappings
 * - Sheet "{MappingName}": Field mappings for each resource
 * 
 * Converts:
 * - Excel (1 workbook) → Multiple JSON files (one per mapping)
 * - Multiple JSON files → Excel (1 workbook with multiple sheets)
 */
public class MultiMappingExcelConverter {
    
    private final ObjectMapper objectMapper;
    
    public MultiMappingExcelConverter() {
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Convert single Excel workbook with multiple mappings to multiple JSON files
     * 
     * @param excelPath Path to Excel file
     * @param outputDir Directory to write JSON files
     * @return List of created JSON file paths
     */
    public List<String> excelToJsonFiles(String excelPath, String outputDir) throws IOException {
        List<ResourceMapping> mappings = excelToResourceMappings(excelPath);
        List<String> createdFiles = new ArrayList<>();
        
        // Create output directory if needed
        File dir = new File(outputDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        // Write each mapping to separate JSON file
        for (ResourceMapping mapping : mappings) {
            String filename = mapping.getId() + ".json";
            String filepath = outputDir + "/" + filename;
            
            objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(new File(filepath), mapping);
            
            createdFiles.add(filepath);
            System.out.println("  ✓ Created: " + filename);
        }
        
        return createdFiles;
    }
    
    /**
     * Convert single Excel workbook to list of ResourceMapping objects
     */
    public List<ResourceMapping> excelToResourceMappings(String excelPath) throws IOException {
        try (FileInputStream fis = new FileInputStream(excelPath);
             Workbook workbook = new XSSFWorkbook(fis)) {
            
            // Read common config from "Config" sheet
            Map<String, String> commonConfig = readConfigSheet(workbook);
            
            // Read each mapping sheet
            List<ResourceMapping> mappings = new ArrayList<>();
            
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                String sheetName = sheet.getSheetName();
                
                // Skip Config sheet
                if ("Config".equalsIgnoreCase(sheetName)) {
                    continue;
                }
                
                // Read mapping from sheet
                ResourceMapping mapping = readMappingFromSheet(sheet, commonConfig);
                mappings.add(mapping);
            }
            
            return mappings;
        }
    }
    
    /**
     * Convert multiple JSON files to single Excel workbook
     * 
     * @param jsonFiles List of JSON file paths
     * @param excelPath Output Excel path
     */
    public void jsonFilesToExcel(List<String> jsonFiles, String excelPath) throws IOException {
        // Read all mappings
        List<ResourceMapping> mappings = new ArrayList<>();
        for (String jsonFile : jsonFiles) {
            ResourceMapping mapping = objectMapper.readValue(new File(jsonFile), ResourceMapping.class);
            mappings.add(mapping);
        }
        
        // Convert to Excel
        resourceMappingsToExcel(mappings, excelPath);
    }
    
    /**
     * Convert multiple ResourceMapping objects to single Excel workbook
     */
    public void resourceMappingsToExcel(List<ResourceMapping> mappings, String excelPath) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            
            // Create Config sheet with common metadata
            writeConfigSheet(workbook, mappings);
            
            // Create a sheet for each mapping
            for (ResourceMapping mapping : mappings) {
                writeMappingSheet(workbook, mapping);
            }
            
            // Write to file
            try (FileOutputStream fos = new FileOutputStream(excelPath)) {
                workbook.write(fos);
            }
        }
    }
    
    /**
     * Convert directory of JSON files to single Excel workbook
     */
    public void jsonDirectoryToExcel(String jsonDir, String excelPath) throws IOException {
        File directory = new File(jsonDir);
        File[] jsonFiles = directory.listFiles((dir, name) -> name.endsWith(".json"));
        
        if (jsonFiles == null || jsonFiles.length == 0) {
            throw new IOException("No JSON files found in: " + jsonDir);
        }
        
        List<String> filePaths = new ArrayList<>();
        for (File file : jsonFiles) {
            filePaths.add(file.getAbsolutePath());
        }
        
        jsonFilesToExcel(filePaths, excelPath);
    }
    
    // ========================================================================
    // Reading Methods
    // ========================================================================
    
    /**
     * Read Config sheet with common metadata
     */
    private Map<String, String> readConfigSheet(Workbook workbook) {
        Sheet sheet = workbook.getSheet("Config");
        if (sheet == null) {
            throw new IllegalArgumentException("Excel file must have 'Config' sheet");
        }
        
        Map<String, String> config = new HashMap<>();
        
        Iterator<Row> rowIterator = sheet.iterator();
        rowIterator.next(); // Skip header
        
        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            String key = getCellValueAsString(row.getCell(0));
            String value = getCellValueAsString(row.getCell(1));
            
            if (key != null && !key.isEmpty()) {
                config.put(key.toLowerCase(), value);
            }
        }
        
        return config;
    }
    
    /**
     * Read mapping from a sheet
     */
    private ResourceMapping readMappingFromSheet(Sheet sheet, Map<String, String> commonConfig) {
        String sheetName = sheet.getSheetName();
        
        ResourceMapping mapping = new ResourceMapping();
        
        // Read mapping-specific metadata from first few rows
        Row metaRow0 = sheet.getRow(0); // ID row
        Row metaRow1 = sheet.getRow(1); // Direction row
        Row metaRow2 = sheet.getRow(2); // Source Type row
        Row metaRow3 = sheet.getRow(3); // Target Type row
        
        // Read ID from first meta row (takes precedence over generated ID)
        String id = null;
        if (metaRow0 != null) {
            String label = getCellValueAsString(metaRow0.getCell(0));
            if ("ID:".equalsIgnoreCase(label) || "id:".equalsIgnoreCase(label)) {
                id = getCellValueAsString(metaRow0.getCell(1));
            }
        }
        
        // If no ID in meta row, generate from sheet name
        if (id == null || id.trim().isEmpty()) {
            String[] parts = sheetName.split(" - ");
            String resourceName = parts[0].trim();
            
            id = resourceName.toLowerCase().replaceAll("\\s+", "-");
            if (parts.length > 1) {
                String direction = parts[1].trim().toLowerCase();
                if (direction.contains("json") && direction.contains("fhir")) {
                    id += "-json-to-fhir";
                } else if (direction.contains("fhir") && direction.contains("json")) {
                    id += "-fhir-to-json";
                }
            }
            
            // Get version from config or default
            String version = commonConfig.getOrDefault("version", "1.0.0");
            id += "-v" + version.replace(".", "");
        }
        
        mapping.setId(id);
        mapping.setName(sheetName);
        mapping.setVersion(commonConfig.get("version"));
        
        // Read direction
        if (metaRow1 != null) {
            String direction = getCellValueAsString(metaRow1.getCell(1));
            if (direction != null && !direction.trim().isEmpty()) {
                mapping.setDirection(MappingDirection.valueOf(direction));
            }
        }
        
        // Read source type
        if (metaRow2 != null) {
            String sourceType = getCellValueAsString(metaRow2.getCell(1));
            if (sourceType != null && !sourceType.trim().isEmpty()) {
                mapping.setSourceType(sourceType);
            }
        }
        
        // Read target type
        if (metaRow3 != null) {
            String targetType = getCellValueAsString(metaRow3.getCell(1));
            if (targetType != null && !targetType.trim().isEmpty()) {
                mapping.setTargetType(targetType);
            }
        }
        
        // Read field mappings starting from row 6 (after metadata, blank row, and header)
        List<FieldMapping> fieldMappings = readFieldMappings(sheet, 6);
        mapping.setFieldMappings(fieldMappings);
        
        return mapping;
    }
    
    /**
     * Read field mappings from sheet starting at given row
     */
    private List<FieldMapping> readFieldMappings(Sheet sheet, int startRow) {
        List<FieldMapping> fieldMappings = new ArrayList<>();
        
        // Build column map from header row
        Row headerRow = sheet.getRow(startRow - 1);
        Map<String, Integer> columnMap = buildColumnMap(headerRow);
        
        // Read data rows
        for (int i = startRow; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null || isRowEmpty(row)) continue;
            
            FieldMapping fieldMapping = new FieldMapping();
            
            fieldMapping.setId(getCellValue(row, columnMap, "id"));
            fieldMapping.setSourcePath(getCellValue(row, columnMap, "sourcepath"));
            fieldMapping.setTargetPath(getCellValue(row, columnMap, "targetpath"));
            fieldMapping.setDataType(getCellValue(row, columnMap, "datatype"));
            fieldMapping.setTransformExpression(getCellValue(row, columnMap, "transformexpression"));
            fieldMapping.setCondition(getCellValue(row, columnMap, "condition"));
            fieldMapping.setValidator(getCellValue(row, columnMap, "validator"));
            
            String required = getCellValue(row, columnMap, "required");
            fieldMapping.setRequired("true".equalsIgnoreCase(required) || "yes".equalsIgnoreCase(required));
            
            fieldMapping.setDefaultValue(getCellValue(row, columnMap, "defaultvalue"));
            fieldMapping.setLookupTable(getCellValue(row, columnMap, "lookuptable"));
            fieldMapping.setDescription(getCellValue(row, columnMap, "description"));
            
            fieldMappings.add(fieldMapping);
        }
        
        return fieldMappings;
    }
    
    // ========================================================================
    // Writing Methods
    // ========================================================================
    
    /**
     * Write Config sheet with common metadata
     */
    private void writeConfigSheet(Workbook workbook, List<ResourceMapping> mappings) {
        Sheet sheet = workbook.createSheet("Config");
        
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle instructionStyle = createInstructionStyle(workbook);
        
        // Add instructions
        Row instructionRow = sheet.createRow(0);
        Cell instructionCell = instructionRow.createCell(0);
        instructionCell.setCellValue("Common configuration for all mappings in this workbook");
        instructionCell.setCellStyle(instructionStyle);
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 2));
        
        // Create header
        Row headerRow = sheet.createRow(2);
        createCell(headerRow, 0, "Property", headerStyle);
        createCell(headerRow, 1, "Value", headerStyle);
        createCell(headerRow, 2, "Description", headerStyle);
        
        // Get common version (use first mapping's version)
        String version = mappings.isEmpty() ? "1.0.0" : mappings.get(0).getVersion();
        
        // Write config
        int rowNum = 3;
        createConfigRow(sheet, rowNum++, "version", version != null ? version : "1.0.0", 
            "Version for all mappings in this workbook");
        createConfigRow(sheet, rowNum++, "fhirVersion", "R4", 
            "FHIR version (R4, R5, etc.)");
        createConfigRow(sheet, rowNum++, "author", "", 
            "Author/creator of mappings");
        createConfigRow(sheet, rowNum++, "organization", "", 
            "Organization");
        
        // Auto-size
        sheet.autoSizeColumn(0);
        sheet.setColumnWidth(1, 5000);
        sheet.setColumnWidth(2, 10000);
    }
    
    /**
     * Write mapping sheet for a single ResourceMapping
     */
    private void writeMappingSheet(Workbook workbook, ResourceMapping mapping) {
        // Sheet name: use mapping name or construct from ID
        String sheetName = mapping.getName() != null ? mapping.getName() : 
            constructSheetName(mapping);
        
        // Excel sheet names limited to 31 chars
        if (sheetName.length() > 31) {
            sheetName = sheetName.substring(0, 31);
        }
        
        Sheet sheet = workbook.createSheet(sheetName);
        
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle metaStyle = createMetaStyle(workbook);
        
        int rowNum = 0;
        
        // Write mapping metadata (with ID row first)
        Row metaRow0 = sheet.createRow(rowNum++);
        createCell(metaRow0, 0, "ID:", metaStyle);
        createCell(metaRow0, 1, mapping.getId());
        
        Row metaRow1 = sheet.createRow(rowNum++);
        createCell(metaRow1, 0, "Direction:", metaStyle);
        createCell(metaRow1, 1, mapping.getDirection() != null ? mapping.getDirection().name() : "");
        
        Row metaRow2 = sheet.createRow(rowNum++);
        createCell(metaRow2, 0, "Source Type:", metaStyle);
        createCell(metaRow2, 1, mapping.getSourceType());
        
        Row metaRow3 = sheet.createRow(rowNum++);
        createCell(metaRow3, 0, "Target Type:", metaStyle);
        createCell(metaRow3, 1, mapping.getTargetType());
        
        rowNum++; // Blank row
        
        // Write field mappings header
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {
            "id", "sourcePath", "targetPath", "dataType", "transformExpression",
            "condition", "validator", "required", "defaultValue", "lookupTable", "description"
        };
        
        for (int i = 0; i < headers.length; i++) {
            createCell(headerRow, i, headers[i], headerStyle);
        }
        
        // Write field mappings
        for (FieldMapping field : mapping.getFieldMappings()) {
            Row row = sheet.createRow(rowNum++);
            
            int colNum = 0;
            createCell(row, colNum++, field.getId());
            createCell(row, colNum++, field.getSourcePath());
            createCell(row, colNum++, field.getTargetPath());
            createCell(row, colNum++, field.getDataType());
            createCell(row, colNum++, field.getTransformExpression());
            createCell(row, colNum++, field.getCondition());
            createCell(row, colNum++, field.getValidator());
            createCell(row, colNum++, field.isRequired() ? "TRUE" : "FALSE");
            createCell(row, colNum++, field.getDefaultValue());
            createCell(row, colNum++, field.getLookupTable());
            createCell(row, colNum++, field.getDescription());
        }
        
        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
            if (sheet.getColumnWidth(i) < 3000) {
                sheet.setColumnWidth(i, 3000);
            }
        }
    }
    
    // ========================================================================
    // Helper Methods
    // ========================================================================
    
    private String constructSheetName(ResourceMapping mapping) {
        String name = mapping.getSourceType();
        if (mapping.getDirection() == MappingDirection.JSON_TO_FHIR) {
            name += " - JSON to FHIR";
        } else if (mapping.getDirection() == MappingDirection.FHIR_TO_JSON) {
            name += " - FHIR to JSON";
        }
        return name;
    }
    
    private Map<String, Integer> buildColumnMap(Row headerRow) {
        Map<String, Integer> map = new HashMap<>();
        
        if (headerRow == null) return map;
        
        Iterator<Cell> cellIterator = headerRow.cellIterator();
        while (cellIterator.hasNext()) {
            Cell cell = cellIterator.next();
            String header = getCellValueAsString(cell);
            if (header != null && !header.isEmpty()) {
                map.put(header.toLowerCase().trim(), cell.getColumnIndex());
            }
        }
        
        return map;
    }
    
    private String getCellValue(Row row, Map<String, Integer> columnMap, String columnName) {
        Integer colIndex = columnMap.get(columnName.toLowerCase());
        if (colIndex == null) return null;
        
        Cell cell = row.getCell(colIndex);
        return getCellValueAsString(cell);
    }
    
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;
        
        switch (cell.getCellType()) {
            case STRING:
                String value = cell.getStringCellValue();
                // Return null for empty strings to avoid blank JSON values
                return (value != null && !value.trim().isEmpty()) ? value : null;
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    // Evaluate formula and get result
                    String formulaValue = cell.getStringCellValue();
                    return (formulaValue != null && !formulaValue.trim().isEmpty()) ? formulaValue : null;
                } catch (Exception e) {
                    return null;
                }
            case BLANK:
                return null;
            default:
                return null;
        }
    }
    
    private boolean isRowEmpty(Row row) {
        if (row == null) return true;
        
        for (int i = row.getFirstCellNum(); i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String value = getCellValueAsString(cell);
                if (value != null && !value.trim().isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }
    
    private void createConfigRow(Sheet sheet, int rowNum, String property, String value, String description) {
        Row row = sheet.createRow(rowNum);
        createCell(row, 0, property);
        createCell(row, 1, value != null ? value : "");
        createCell(row, 2, description);
    }
    
    private void createCell(Row row, int column, String value) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value != null ? value : "");
    }
    
    private void createCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }
    
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }
    
    private CellStyle createInstructionStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setItalic(true);
        font.setColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setWrapText(true);
        return style;
    }
    
    private CellStyle createMetaStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }
}