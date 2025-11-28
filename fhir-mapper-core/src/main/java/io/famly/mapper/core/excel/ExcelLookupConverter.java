package com.fhir.mapper.excel;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.fhir.mapper.model.CodeLookupTable;
import com.fhir.mapper.model.CodeMapping;

/**
 * 
 * 
Sheet: "gender-lookup"

Row 1: ID:            | gender-lookup
Row 2: Name:          | Gender Code Mapping
Row 3: Source System: | internal
Row 4: Bidirectional: | false
Row 5: (blank)
Row 6: sourceCode | targetCode | targetSystem | display
Row 7: M          | male       | http://...   | Male
Row 8: F          | female     | http://...   | Female

 * 
 */

public class ExcelLookupConverter {
    
    /**
     * Convert Excel workbook where each sheet is a lookup table
     */
    public List<CodeLookupTable> excelToLookupTables(String excelPath) throws IOException {
        List<CodeLookupTable> lookups = new ArrayList<>();
        
        try (FileInputStream fis = new FileInputStream(excelPath);
             Workbook workbook = new XSSFWorkbook(fis)) {
            
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                String sheetName = sheet.getSheetName();
                
                // Skip metadata sheets
                if ("Config".equalsIgnoreCase(sheetName) || 
                    "README".equalsIgnoreCase(sheetName)) {
                    continue;
                }
                
                CodeLookupTable lookup = readLookupFromSheet(sheet);
                lookups.add(lookup);
            }
        }
        
        return lookups;
    }
    
    private CodeLookupTable readLookupFromSheet(Sheet sheet) {
        CodeLookupTable lookup = new CodeLookupTable();
        
        // Read metadata from first rows
        String id = ExcelUtils.getCellValueAsString(sheet.getRow(0).getCell(1));
        String name = ExcelUtils.getCellValueAsString(sheet.getRow(1).getCell(1));
        String sourceSystem = ExcelUtils.getCellValueAsString(sheet.getRow(2).getCell(1));
        String defaultTargetSystem = ExcelUtils.getCellValueAsString(sheet.getRow(3).getCell(1));
        String bidirectional = ExcelUtils.getCellValueAsString(sheet.getRow(4).getCell(1));
        
        lookup.setId(id != null ? id : sheet.getSheetName().toLowerCase().replace(" ", "-"));
        lookup.setName(name != null ? name : sheet.getSheetName());
        lookup.setSourceSystem(sourceSystem);
        lookup.setDefaultTargetSystem(defaultTargetSystem);
        lookup.setBidirectional("true".equalsIgnoreCase(bidirectional));
        
        // Read mappings starting from row 7 (after header at row 6)
        List<CodeMapping> mappings = new ArrayList<>();
        Row headerRow = sheet.getRow(7);
        
        for (int i = 7; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null || ExcelUtils.isRowEmpty(row)) continue;
            
            CodeMapping mapping = new CodeMapping();
            mapping.setSourceCode(ExcelUtils.getCellValueAsString(row.getCell(0)));
            mapping.setTargetCode(ExcelUtils.getCellValueAsString(row.getCell(1)));
            mapping.setTargetSystem(ExcelUtils.getCellValueAsString(row.getCell(2))); // NEW
            mapping.setDisplay(ExcelUtils.getCellValueAsString(row.getCell(3)));
            
            mappings.add(mapping);
        }
        
        lookup.setMappings(mappings);
        return lookup;
    }
}
