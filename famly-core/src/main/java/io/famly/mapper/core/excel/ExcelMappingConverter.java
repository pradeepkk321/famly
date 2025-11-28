package io.famly.mapper.core.excel;

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

import io.famly.mapper.core.model.FieldMapping;
import io.famly.mapper.core.model.MappingDirection;
import io.famly.mapper.core.model.ResourceMapping;

/**
 * Converts between Excel spreadsheets and JSON mapping files.
 * 
 * Excel Format:
 * 
 * Sheet 1: "Mapping Info"
 * - id, name, version, direction, sourceType, targetType
 * 
 * Sheet 2: "Field Mappings"
 * - id, sourcePath, targetPath, dataType, transformExpression, condition,
 *   validator, required, defaultValue, lookupTable, description
 */
public class ExcelMappingConverter {
    
    private final ObjectMapper objectMapper;
    
    public ExcelMappingConverter() {
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Convert Excel file to JSON ResourceMapping file
     */
    public ResourceMapping excelToResourceMapping(String excelPath) throws IOException {
        try (FileInputStream fis = new FileInputStream(excelPath);
             Workbook workbook = new XSSFWorkbook(fis)) {
            
            // Read mapping info
            ResourceMapping mapping = readMappingInfo(workbook);
            
            // Read field mappings
            List<FieldMapping> fieldMappings = readFieldMappings(workbook);
            mapping.setFieldMappings(fieldMappings);
            
            return mapping;
        }
    }
    
    /**
     * Convert Excel to JSON file
     */
    public void excelToJson(String excelPath, String jsonPath) throws IOException {
        ResourceMapping mapping = excelToResourceMapping(excelPath);
        objectMapper.writerWithDefaultPrettyPrinter()
            .writeValue(new File(jsonPath), mapping);
    }
    
    /**
     * Convert JSON ResourceMapping to Excel file
     */
    public void resourceMappingToExcel(ResourceMapping mapping, String excelPath) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            
            // Create mapping info sheet
            writeMappingInfo(workbook, mapping);
            
            // Create field mappings sheet
            writeFieldMappings(workbook, mapping.getFieldMappings());
            
            // Write to file
            try (FileOutputStream fos = new FileOutputStream(excelPath)) {
                workbook.write(fos);
            }
        }
    }
    
    /**
     * Convert JSON file to Excel
     */
    public void jsonToExcel(String jsonPath, String excelPath) throws IOException {
        ResourceMapping mapping = objectMapper.readValue(
            new File(jsonPath), 
            ResourceMapping.class
        );
        resourceMappingToExcel(mapping, excelPath);
    }
    
    /**
     * Read mapping info from first sheet
     */
    private ResourceMapping readMappingInfo(Workbook workbook) {
        Sheet sheet = workbook.getSheet("Mapping Info");
        if (sheet == null) {
            throw new IllegalArgumentException("Excel file must have 'Mapping Info' sheet");
        }
        
        ResourceMapping mapping = new ResourceMapping();
        
        // Read key-value pairs
        Iterator<Row> rowIterator = sheet.iterator();
        rowIterator.next(); // Skip header
        
        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            String key = getCellValueAsString(row.getCell(0));
            String value = getCellValueAsString(row.getCell(1));
            
            if (key == null || key.isEmpty()) continue;
            
            switch (key.toLowerCase()) {
                case "id":
                    mapping.setId(value);
                    break;
                case "name":
                    mapping.setName(value);
                    break;
                case "version":
                    mapping.setVersion(value);
                    break;
                case "direction":
                    mapping.setDirection(MappingDirection.valueOf(value));
                    break;
                case "sourcetype":
                    mapping.setSourceType(value);
                    break;
                case "targettype":
                    mapping.setTargetType(value);
                    break;
            }
        }
        
        return mapping;
    }
    
    /**
     * Read field mappings from second sheet
     */
    private List<FieldMapping> readFieldMappings(Workbook workbook) {
        Sheet sheet = workbook.getSheet("Field Mappings");
        if (sheet == null) {
            throw new IllegalArgumentException("Excel file must have 'Field Mappings' sheet");
        }
        
        List<FieldMapping> fieldMappings = new ArrayList<>();
        
        // Read header row to get column indices
        Row headerRow = sheet.getRow(0);
        Map<String, Integer> columnMap = buildColumnMap(headerRow);
        
        // Read data rows
        Iterator<Row> rowIterator = sheet.iterator();
        rowIterator.next(); // Skip header
        
        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            
            // Skip empty rows
            if (isRowEmpty(row)) continue;
            
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
    
    /**
     * Write mapping info to first sheet
     */
    private void writeMappingInfo(Workbook workbook, ResourceMapping mapping) {
        Sheet sheet = workbook.createSheet("Mapping Info");
        
        // Create header style
        CellStyle headerStyle = createHeaderStyle(workbook);
        
        // Create header row
        Row headerRow = sheet.createRow(0);
        createCell(headerRow, 0, "Property", headerStyle);
        createCell(headerRow, 1, "Value", headerStyle);
        
        // Write mapping info
        int rowNum = 1;
        createInfoRow(sheet, rowNum++, "id", mapping.getId());
        createInfoRow(sheet, rowNum++, "name", mapping.getName());
        createInfoRow(sheet, rowNum++, "version", mapping.getVersion());
        createInfoRow(sheet, rowNum++, "direction", 
            mapping.getDirection() != null ? mapping.getDirection().name() : "");
        createInfoRow(sheet, rowNum++, "sourceType", mapping.getSourceType());
        createInfoRow(sheet, rowNum++, "targetType", mapping.getTargetType());
        
        // Auto-size columns
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }
    
    /**
     * Write field mappings to second sheet
     */
    private void writeFieldMappings(Workbook workbook, List<FieldMapping> fieldMappings) {
        Sheet sheet = workbook.createSheet("Field Mappings");
        
        // Create header style
        CellStyle headerStyle = createHeaderStyle(workbook);
        
        // Create header row
        Row headerRow = sheet.createRow(0);
        String[] headers = {
            "id", "sourcePath", "targetPath", "dataType", "transformExpression",
            "condition", "validator", "required", "defaultValue", "lookupTable", "description"
        };
        
        for (int i = 0; i < headers.length; i++) {
            createCell(headerRow, i, headers[i], headerStyle);
        }
        
        // Write field mappings
        int rowNum = 1;
        for (FieldMapping field : fieldMappings) {
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
        }
    }
    
    // ========================================================================
    // Helper Methods
    // ========================================================================
    
    private Map<String, Integer> buildColumnMap(Row headerRow) {
        Map<String, Integer> map = new HashMap<>();
        
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
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
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
    
    private void createInfoRow(Sheet sheet, int rowNum, String property, String value) {
        Row row = sheet.createRow(rowNum);
        createCell(row, 0, property);
        createCell(row, 1, value != null ? value : "");
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
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }
    
    /**
     * Batch convert all JSON files in directory to Excel
     */
    public void convertDirectoryToExcel(String jsonDir, String excelDir) throws IOException {
        File jsonDirectory = new File(jsonDir);
        File excelDirectory = new File(excelDir);
        
        if (!excelDirectory.exists()) {
            excelDirectory.mkdirs();
        }
        
        File[] jsonFiles = jsonDirectory.listFiles((dir, name) -> name.endsWith(".json"));
        if (jsonFiles == null) return;
        
        for (File jsonFile : jsonFiles) {
            String baseName = jsonFile.getName().replace(".json", "");
            String excelPath = excelDir + "/" + baseName + ".xlsx";
            
            System.out.println("Converting: " + jsonFile.getName() + " -> " + baseName + ".xlsx");
            jsonToExcel(jsonFile.getAbsolutePath(), excelPath);
        }
        
        System.out.println("Converted " + jsonFiles.length + " files");
    }
    
    /**
     * Batch convert all Excel files in directory to JSON
     */
    public void convertDirectoryToJson(String excelDir, String jsonDir) throws IOException {
        File excelDirectory = new File(excelDir);
        File jsonDirectory = new File(jsonDir);
        
        if (!jsonDirectory.exists()) {
            jsonDirectory.mkdirs();
        }
        
        File[] excelFiles = excelDirectory.listFiles((dir, name) -> 
            name.endsWith(".xlsx") || name.endsWith(".xls"));
        if (excelFiles == null) return;
        
        for (File excelFile : excelFiles) {
            String baseName = excelFile.getName()
                .replace(".xlsx", "")
                .replace(".xls", "");
            String jsonPath = jsonDir + "/" + baseName + ".json";
            
            System.out.println("Converting: " + excelFile.getName() + " -> " + baseName + ".json");
            excelToJson(excelFile.getAbsolutePath(), jsonPath);
        }
        
        System.out.println("Converted " + excelFiles.length + " files");
    }
}