package io.famly.mapper.core.cli;

import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.*;

/**
 * Generates Excel template with validation rules and examples
 */
public class ExcelTemplateGenerator {
    
    public void generateTemplate(String outputPath) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            
            // Create sheets
            createMappingInfoSheet(workbook);
            createFieldMappingsSheet(workbook);
            createReferenceSheet(workbook);
            createExampleSheet(workbook);
            
            // Write to file
            try (FileOutputStream fos = new FileOutputStream(outputPath)) {
                workbook.write(fos);
            }
        }
    }
    
    /**
     * Create Mapping Info sheet with instructions
     */
    private void createMappingInfoSheet(XSSFWorkbook workbook) {
        XSSFSheet sheet = workbook.createSheet("Mapping Info");
        
        // Create styles
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle instructionStyle = createInstructionStyle(workbook);
        
        // Add instructions
        Row instructionRow = sheet.createRow(0);
        Cell instructionCell = instructionRow.createCell(0);
        instructionCell.setCellValue("Enter mapping metadata below. Do not modify the property names in column A.");
        instructionCell.setCellStyle(instructionStyle);
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 2));
        
        // Create header row
        Row headerRow = sheet.createRow(2);
        createCell(headerRow, 0, "Property", headerStyle);
        createCell(headerRow, 1, "Value", headerStyle);
        createCell(headerRow, 2, "Description", headerStyle);
        
        // Add template rows with descriptions
        int rowNum = 3;
        createTemplateRow(sheet, rowNum++, "id", "", "Unique identifier (e.g., patient-json-to-fhir-v1)");
        createTemplateRow(sheet, rowNum++, "name", "", "Human-readable name (e.g., Patient JSON to FHIR Mapping)");
        createTemplateRow(sheet, rowNum++, "version", "1.0.0", "Version number");
        createTemplateRow(sheet, rowNum++, "direction", "JSON_TO_FHIR", "Direction: JSON_TO_FHIR or FHIR_TO_JSON");
        createTemplateRow(sheet, rowNum++, "sourceType", "", "Source type name (e.g., PatientDTO or Patient)");
        createTemplateRow(sheet, rowNum++, "targetType", "", "Target type name (e.g., Patient or PatientDTO)");
        
        // Add dropdown validation for direction
        XSSFDataValidationHelper validationHelper = new XSSFDataValidationHelper(sheet);
        XSSFDataValidationConstraint constraint = (XSSFDataValidationConstraint) 
            validationHelper.createExplicitListConstraint(new String[]{"JSON_TO_FHIR", "FHIR_TO_JSON"});
        CellRangeAddressList addressList = new CellRangeAddressList(5, 5, 1, 1); // direction row
        XSSFDataValidation validation = (XSSFDataValidation) 
            validationHelper.createValidation(constraint, addressList);
        validation.setShowErrorBox(true);
        sheet.addValidationData(validation);
        
        // Auto-size columns
        sheet.autoSizeColumn(0);
        sheet.setColumnWidth(1, 8000);
        sheet.setColumnWidth(2, 12000);
    }
    
    /**
     * Create Field Mappings sheet with examples
     */
    private void createFieldMappingsSheet(XSSFWorkbook workbook) {
        XSSFSheet sheet = workbook.createSheet("Field Mappings");
        
        // Create styles
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle instructionStyle = createInstructionStyle(workbook);
        CellStyle exampleStyle = createExampleStyle(workbook);
        
        // Add instructions
        Row instructionRow = sheet.createRow(0);
        Cell instructionCell = instructionRow.createCell(0);
        instructionCell.setCellValue("Define field mappings below. Each row represents one field mapping. See 'Examples' sheet for samples.");
        instructionCell.setCellStyle(instructionStyle);
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 10));
        
        // Create header row
        Row headerRow = sheet.createRow(2);
        String[] headers = {
            "id", "sourcePath", "targetPath", "dataType", "transformExpression",
            "condition", "validator", "required", "defaultValue", "lookupTable", "description"
        };
        
        for (int i = 0; i < headers.length; i++) {
            createCell(headerRow, i, headers[i], headerStyle);
        }
        
        // Add example rows
        int rowNum = 3;
        createExampleFieldRow(sheet, exampleStyle, rowNum++,
            "patient-id", "patientId", "identifier[0].value", "string", "",
            "", "", "TRUE", "", "", "Patient MRN");
        
        createExampleFieldRow(sheet, exampleStyle, rowNum++,
            "patient-id-system", "", "identifier[0].system", "uri", "",
            "", "", "TRUE", "$ctx.settings['identifierSystem']", "", "MRN system");
        
        createExampleFieldRow(sheet, exampleStyle, rowNum++,
            "patient-gender", "gender", "gender", "code", "",
            "", "", "TRUE", "", "gender-lookup", "Gender code");
        
        createExampleFieldRow(sheet, exampleStyle, rowNum++,
            "patient-ssn", "ssn", "identifier[1].value", "string", "fn.replace(value, '-', '')",
            "ssn != null && ssn != ''", "", "FALSE", "", "", "SSN without dashes");
        
        // Add dropdown for dataType
        XSSFDataValidationHelper validationHelper = new XSSFDataValidationHelper(sheet);
        XSSFDataValidationConstraint constraint = (XSSFDataValidationConstraint) 
            validationHelper.createExplicitListConstraint(new String[]{
                "string", "integer", "decimal", "boolean", "date", "dateTime", 
                "code", "uri", "url", "id", "array"
            });
        CellRangeAddressList addressList = new CellRangeAddressList(3, 1000, 3, 3);
        XSSFDataValidation validation = (XSSFDataValidation) 
            validationHelper.createValidation(constraint, addressList);
        sheet.addValidationData(validation);
        
        // Add dropdown for required
        constraint = (XSSFDataValidationConstraint) 
            validationHelper.createExplicitListConstraint(new String[]{"TRUE", "FALSE"});
        addressList = new CellRangeAddressList(3, 1000, 7, 7);
        validation = (XSSFDataValidation) validationHelper.createValidation(constraint, addressList);
        sheet.addValidationData(validation);
        
        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
            if (sheet.getColumnWidth(i) < 3000) {
                sheet.setColumnWidth(i, 3000);
            }
        }
    }
    
    /**
     * Create Reference sheet with documentation
     */
    private void createReferenceSheet(XSSFWorkbook workbook) {
        XSSFSheet sheet = workbook.createSheet("Reference");
        
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle titleStyle = createTitleStyle(workbook);
        
        int rowNum = 0;
        
        // Title
        Row titleRow = sheet.createRow(rowNum++);
        createCell(titleRow, 0, "FHIR Mapper Reference Guide", titleStyle);
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(rowNum-1, rowNum-1, 0, 2));
        rowNum++;
        
        // Data Types section
        Row dataTypeTitle = sheet.createRow(rowNum++);
        createCell(dataTypeTitle, 0, "Valid Data Types", headerStyle);
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(rowNum-1, rowNum-1, 0, 2));
        
        String[][] dataTypes = {
            {"string", "Text values", "\"John Doe\""},
            {"integer", "Whole numbers", "42"},
            {"decimal", "Decimal numbers", "3.14"},
            {"boolean", "True/False values", "true"},
            {"date", "Date values", "2024-01-15"},
            {"dateTime", "Date with time", "2024-01-15T10:30:00"},
            {"code", "Coded values", "male"},
            {"uri", "URIs", "http://example.com"},
            {"id", "Identifiers", "patient-123"},
            {"array", "Array of values", "[...]"}
        };
        
        Row dataTypeHeader = sheet.createRow(rowNum++);
        createCell(dataTypeHeader, 0, "Type", headerStyle);
        createCell(dataTypeHeader, 1, "Description", headerStyle);
        createCell(dataTypeHeader, 2, "Example", headerStyle);
        
        for (String[] dt : dataTypes) {
            Row row = sheet.createRow(rowNum++);
            createCell(row, 0, dt[0]);
            createCell(row, 1, dt[1]);
            createCell(row, 2, dt[2]);
        }
        
        rowNum++;
        
        // Transform Functions section
        Row funcTitle = sheet.createRow(rowNum++);
        createCell(funcTitle, 0, "Transform Functions", headerStyle);
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(rowNum-1, rowNum-1, 0, 2));
        
        String[][] functions = {
            {"fn.uppercase(value)", "Convert to uppercase", "JOHN"},
            {"fn.lowercase(value)", "Convert to lowercase", "john"},
            {"fn.replace(value, '-', '')", "Replace characters", "123456789"},
            {"fn.concat('Org/', ctx.orgId)", "Concatenate strings", "Org/123"},
            {"fn.formatDate(value, 'yyyy-MM-dd')", "Format date", "2024-01-15"},
            {"fn.toInt(value)", "Convert to integer", "42"},
            {"fn.defaultIfNull(value, 'default')", "Fallback value", "default"}
        };
        
        Row funcHeader = sheet.createRow(rowNum++);
        createCell(funcHeader, 0, "Function", headerStyle);
        createCell(funcHeader, 1, "Description", headerStyle);
        createCell(funcHeader, 2, "Example Output", headerStyle);
        
        for (String[] func : functions) {
            Row row = sheet.createRow(rowNum++);
            createCell(row, 0, func[0]);
            createCell(row, 1, func[1]);
            createCell(row, 2, func[2]);
        }
        
        rowNum++;
        
        // Context Variables section
        Row ctxTitle = sheet.createRow(rowNum++);
        createCell(ctxTitle, 0, "Context Variables", headerStyle);
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(rowNum-1, rowNum-1, 0, 2));
        
        String[][] contextVars = {
            {"ctx.organizationId", "Current organization ID"},
            {"ctx.facilityId", "Current facility ID"},
            {"ctx.tenantId", "Current tenant ID"},
            {"ctx.settings.key", "Setting from context"},
            {"$ctx.settings['key']", "Setting (for defaultValue field)"}
        };
        
        Row ctxHeader = sheet.createRow(rowNum++);
        createCell(ctxHeader, 0, "Variable", headerStyle);
        createCell(ctxHeader, 1, "Description", headerStyle);
        
        for (String[] cv : contextVars) {
            Row row = sheet.createRow(rowNum++);
            createCell(row, 0, cv[0]);
            createCell(row, 1, cv[1]);
        }
        
        // Auto-size columns
        for (int i = 0; i < 3; i++) {
            sheet.autoSizeColumn(i);
            if (sheet.getColumnWidth(i) < 5000) {
                sheet.setColumnWidth(i, 5000);
            }
        }
    }
    
    /**
     * Create Examples sheet
     */
    private void createExampleSheet(XSSFWorkbook workbook) {
        XSSFSheet sheet = workbook.createSheet("Examples");
        
        CellStyle titleStyle = createTitleStyle(workbook);
        CellStyle codeStyle = createCodeStyle(workbook);
        
        int rowNum = 0;
        
        // Title
        Row titleRow = sheet.createRow(rowNum++);
        createCell(titleRow, 0, "Mapping Examples", titleStyle);
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(rowNum-1, rowNum-1, 0, 1));
        rowNum++;
        
        // Example 1
        createExampleSection(sheet, rowNum, "Example 1: Simple Field Mapping",
            "sourcePath: firstName\n" +
            "targetPath: name[0].given[0]\n" +
            "dataType: string\n" +
            "required: TRUE");
        rowNum += 8;
        
        // Example 2
        createExampleSection(sheet, rowNum, "Example 2: Field with Transform",
            "sourcePath: ssn\n" +
            "targetPath: identifier[1].value\n" +
            "dataType: string\n" +
            "transformExpression: fn.replace(value, '-', '')\n" +
            "required: FALSE");
        rowNum += 10;
        
        // Example 3
        createExampleSection(sheet, rowNum, "Example 3: Conditional Mapping",
            "sourcePath: middleName\n" +
            "targetPath: name[0].given[1]\n" +
            "dataType: string\n" +
            "condition: middleName != null && middleName != ''\n" +
            "required: FALSE");
        rowNum += 10;
        
        // Example 4
        createExampleSection(sheet, rowNum, "Example 4: Default Value from Context",
            "targetPath: identifier[0].system\n" +
            "dataType: uri\n" +
            "defaultValue: $ctx.settings['identifierSystem']\n" +
            "required: TRUE");
        
        sheet.setColumnWidth(0, 15000);
        sheet.setColumnWidth(1, 15000);
    }
    
    private void createExampleSection(XSSFSheet sheet, int startRow, String title, String content) {
        CellStyle headerStyle = createHeaderStyle(sheet.getWorkbook());
        CellStyle codeStyle = createCodeStyle(sheet.getWorkbook());
        
        Row titleRow = sheet.createRow(startRow);
        createCell(titleRow, 0, title, headerStyle);
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(startRow, startRow, 0, 1));
        
        Row contentRow = sheet.createRow(startRow + 1);
        Cell contentCell = contentRow.createCell(0);
        contentCell.setCellValue(content);
        contentCell.setCellStyle(codeStyle);
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(startRow + 1, startRow + 5, 0, 1));
    }
    
    // ========================================================================
    // Helper Methods
    // ========================================================================
    
    private void createTemplateRow(Sheet sheet, int rowNum, String property, String value, String description) {
        Row row = sheet.createRow(rowNum);
        createCell(row, 0, property);
        createCell(row, 1, value);
        createCell(row, 2, description);
    }
    
    private void createExampleFieldRow(Sheet sheet, CellStyle style, int rowNum,
                                      String id, String sourcePath, String targetPath, 
                                      String dataType, String transform,
                                      String condition, String validator, String required,
                                      String defaultValue, String lookupTable, String description) {
        Row row = sheet.createRow(rowNum);
        
        int col = 0;
        createCell(row, col++, id, style);
        createCell(row, col++, sourcePath, style);
        createCell(row, col++, targetPath, style);
        createCell(row, col++, dataType, style);
        createCell(row, col++, transform, style);
        createCell(row, col++, condition, style);
        createCell(row, col++, validator, style);
        createCell(row, col++, required, style);
        createCell(row, col++, defaultValue, style);
        createCell(row, col++, lookupTable, style);
        createCell(row, col++, description, style);
    }
    
    private void createCell(Row row, int column, String value) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value != null ? value : "");
    }
    
    private void createCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value != null ? value : "");
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
        style.setAlignment(HorizontalAlignment.CENTER);
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
        style.setBorderBottom(BorderStyle.THIN);
        style.setWrapText(true);
        return style;
    }
    
    private CellStyle createExampleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        font.setItalic(true);
        style.setFont(font);
        return style;
    }
    
    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        font.setColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }
    
    private CellStyle createCodeStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName("Courier New");
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setWrapText(true);
        style.setVerticalAlignment(VerticalAlignment.TOP);
        return style;
    }
}