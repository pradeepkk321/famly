package io.famly.mapper.core.excel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.famly.mapper.core.model.CodeLookupTable;
import io.famly.mapper.core.model.CodeMapping;

/**
 * Converts JSON lookup files to single Excel workbook with one sheet per lookup
 */
public class LookupJsonToExcelConverter {
    
    private final ObjectMapper objectMapper;
    
    public LookupJsonToExcelConverter() {
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Convert all JSON lookups in directory to single Excel workbook
     */
    public void convertDirectoryToExcel(String jsonDir, String excelPath) throws IOException {
        Path jsonPath = Paths.get(jsonDir);
        
        if (!Files.exists(jsonPath)) {
            throw new IOException("Directory not found: " + jsonDir);
        }
        
        // Load all JSON lookup files
        List<Path> jsonFiles = Files.walk(jsonPath, 1)
            .filter(p -> p.toString().endsWith(".json"))
            .collect(Collectors.toList());
        
        if (jsonFiles.isEmpty()) {
            throw new IOException("No JSON lookup files found in: " + jsonDir);
        }
        
        List<CodeLookupTable> lookups = jsonFiles.stream()
            .map(file -> {
                try {
                    return objectMapper.readValue(file.toFile(), CodeLookupTable.class);
                } catch (IOException e) {
                    System.err.println("Failed to load: " + file.getFileName() + " - " + e.getMessage());
                    return null;
                }
            })
            .filter(lookup -> lookup != null)
            .collect(Collectors.toList());
        
        // Convert to Excel
        lookupsToExcel(lookups, excelPath);
        
        System.out.println("✓ Converted " + lookups.size() + " lookups to: " + excelPath);
    }
    
    /**
     * Convert list of lookups to Excel workbook
     */
    public void lookupsToExcel(List<CodeLookupTable> lookups, String excelPath) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            
            for (CodeLookupTable lookup : lookups) {
                createLookupSheet(workbook, lookup);
            }
            
            // Write to file
            try (FileOutputStream fos = new FileOutputStream(excelPath)) {
                workbook.write(fos);
            }
        }
    }
    
    /**
     * Create sheet for a single lookup table
     */
    private void createLookupSheet(Workbook workbook, CodeLookupTable lookup) {
        // Use lookup name or ID as sheet name (max 31 chars for Excel)
        String sheetName = lookup.getName() != null ? lookup.getName() : lookup.getId();
        if (sheetName.length() > 31) {
            sheetName = sheetName.substring(0, 31);
        }
        
        Sheet sheet = workbook.createSheet(sheetName);
        
        CellStyle metaStyle = ExcelUtils.createMetaStyle(workbook);
        CellStyle headerStyle = ExcelUtils.createHeaderStyle(workbook);
        
        int rowNum = 0;
        
        // Row 1: ID
        Row row1 = sheet.createRow(rowNum++);
        ExcelUtils.createCell(row1, 0, "ID:", metaStyle);
        ExcelUtils.createCell(row1, 1, lookup.getId());
        
        // Row 2: Name
        Row row2 = sheet.createRow(rowNum++);
        ExcelUtils.createCell(row2, 0, "Name:", metaStyle);
        ExcelUtils.createCell(row2, 1, lookup.getName());
        
        // Row 3: Source System
        Row row3 = sheet.createRow(rowNum++);
        ExcelUtils.createCell(row3, 0, "Source System:", metaStyle);
        ExcelUtils.createCell(row3, 1, lookup.getSourceSystem());
        
        // Row 4: Default Target System
        Row row4 = sheet.createRow(rowNum++);
        ExcelUtils.createCell(row4, 0, "Default Target System:", metaStyle);
        ExcelUtils.createCell(row4, 1, lookup.getDefaultTargetSystem());
        
        // Row 5: Bidirectional
        Row row5 = sheet.createRow(rowNum++);
        ExcelUtils.createCell(row5, 0, "Bidirectional:", metaStyle);
        ExcelUtils.createCell(row5, 1, String.valueOf(lookup.isBidirectional()));
        
        // Row 6: Blank
        rowNum++;
        
        // Row 7: Header
        Row headerRow = sheet.createRow(rowNum++);
        ExcelUtils.createCell(headerRow, 0, "sourceCode", headerStyle);
        ExcelUtils.createCell(headerRow, 1, "targetCode", headerStyle);
        ExcelUtils.createCell(headerRow, 2, "targetSystem", headerStyle);
        ExcelUtils.createCell(headerRow, 3, "display", headerStyle);
        
        // Data rows
        for (CodeMapping mapping : lookup.getMappings()) {
            Row dataRow = sheet.createRow(rowNum++);
            ExcelUtils.createCell(dataRow, 0, mapping.getSourceCode());
            ExcelUtils.createCell(dataRow, 1, mapping.getTargetCode());
            ExcelUtils.createCell(dataRow, 2, mapping.getTargetSystem());
            ExcelUtils.createCell(dataRow, 3, mapping.getDisplay());
        }
        
        // Auto-size columns
        for (int i = 0; i < 4; i++) {
            sheet.autoSizeColumn(i);
            if (sheet.getColumnWidth(i) < 3000) {
                sheet.setColumnWidth(i, 3000);
            }
        }
    }
    
    /**
     * CLI usage
     */
    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.println("Usage: java LookupJsonToExcelConverter <json-dir> <output.xlsx>");
            System.out.println("Example: java LookupJsonToExcelConverter ./mappings/lookups ./mappings/lookups-excel/all-lookups.xlsx");
            System.exit(1);
        }
        
        String jsonDir = args[0];
        String excelPath = args[1];
        
        // Create parent directory if needed
        File excelFile = new File(excelPath);
        File parentDir = excelFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        
        LookupJsonToExcelConverter converter = new LookupJsonToExcelConverter();
        converter.convertDirectoryToExcel(jsonDir, excelPath);
    }
}