package io.famly.mapper.core.excel;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import io.famly.mapper.core.model.MappingDirection;
import io.famly.mapper.core.model.ResourceMapping;

public class ExcelUtils {
	
    public static String getCellValueAsString(Cell cell) {
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
    
    public static boolean isRowEmpty(Row row) {
        if (row == null) return true;
        
        for (int i = row.getFirstCellNum(); i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String value = ExcelUtils.getCellValueAsString(cell);
                if (value != null && !value.trim().isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }
    
    public static void createCell(Row row, int column, String value) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value != null ? value : "");
    }
    
    public static void createCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }
    
    public static CellStyle createHeaderStyle(Workbook workbook) {
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
    
    public static CellStyle createMetaStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }    private String constructSheetName(ResourceMapping mapping) {
        String name = mapping.getSourceType();
        if (mapping.getDirection() == MappingDirection.JSON_TO_FHIR) {
            name += " - JSON to FHIR";
        } else if (mapping.getDirection() == MappingDirection.FHIR_TO_JSON) {
            name += " - FHIR to JSON";
        }
        return name;
    }
    
    public static Map<String, Integer> buildColumnMap(Row headerRow) {
        Map<String, Integer> map = new HashMap<>();
        
        if (headerRow == null) return map;
        
        Iterator<Cell> cellIterator = headerRow.cellIterator();
        while (cellIterator.hasNext()) {
            Cell cell = cellIterator.next();
            String header = ExcelUtils.getCellValueAsString(cell);
            if (header != null && !header.isEmpty()) {
                map.put(header.toLowerCase().trim(), cell.getColumnIndex());
            }
        }
        
        return map;
    }
    
    public static String getCellValue(Row row, Map<String, Integer> columnMap, String columnName) {
        Integer colIndex = columnMap.get(columnName.toLowerCase());
        if (colIndex == null) return null;
        
        Cell cell = row.getCell(colIndex);
        return ExcelUtils.getCellValueAsString(cell);
    }
    
    public static void createConfigRow(Sheet sheet, int rowNum, String property, String value, String description) {
        Row row = sheet.createRow(rowNum);
        ExcelUtils.createCell(row, 0, property);
        ExcelUtils.createCell(row, 1, value != null ? value : "");
        ExcelUtils.createCell(row, 2, description);
    }
    
    public static CellStyle createInstructionStyle(Workbook workbook) {
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

}
