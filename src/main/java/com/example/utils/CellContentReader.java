package com.example.utils;

import org.apache.poi.ss.usermodel.*;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Excel单元格内容读取类
 */
public class CellContentReader {
    /**
     * 单元格内容类型枚举
     */
    public enum CellContentType {
        BLANK,           // 空单元格（无内容）
        EMPTY_STRING,    // 空字符串（有样式但内容为空）
        STRING,          // 字符串类型
        NUMERIC,         // 数字类型（整数、小数）
        BOOLEAN,         // 布尔类型
        FORMULA,         // 公式类型
        DATE,            // 日期类型
        TIME,            // 时间类型
        DATETIME,        // 日期时间类型
        ERROR,           // 错误类型
        RICH_TEXT,       // 富文本
        UNKNOWN          // 未知类型
    }

    /**
     * 单元格内容详细信息的封装类
     */
    public static class CellContentInfo {
        private Object value;            // 原始值
        private Object formattedValue;   // 格式化后的值
        private CellContentType type;    // 内容类型
        private String cellAddress;      // 单元格地址
        private String formatString;     // 格式字符串
        private CellStyle cellStyle;     // 单元格样式

        public CellContentInfo(Object value, Object formattedValue,
                               CellContentType type, String cellAddress,
                               String formatString, CellStyle cellStyle) {
            this.value = value;
            this.formattedValue = formattedValue;
            this.type = type;
            this.cellAddress = cellAddress;
            this.formatString = formatString;
            this.cellStyle = cellStyle;
        }

        // Getters
        public Object getValue() { return value; }
        public Object getFormattedValue() { return formattedValue; }
        public CellContentType getType() { return type; }
        public String getCellAddress() { return cellAddress; }
        public String getFormatString() { return formatString; }
        public CellStyle getCellStyle() { return cellStyle; }

        @Override
        public String toString() {
            return String.format("地址: %s | 类型: %s | 原始值: %s | 格式化值: %s | 格式: %s",
                    cellAddress, type, value, formattedValue, formatString != null ? formatString : "默认");
        }
    }

    /**
     * 获取单元格的详细信息
     */
    public static CellContentInfo getCellContentInfo(Cell cell) {
        if (cell == null) {
            return new CellContentInfo(null, null, CellContentType.BLANK,
                    "未知", null, null);
        }
        String cellAddress = cell.getAddress().formatAsString();
        CellStyle cellStyle = cell.getCellStyle();
        String formatString = cellStyle != null ? cellStyle.getDataFormatString() : null;
        switch (cell.getCellType()) {
            case BLANK:
                return handleBlankCell(cell, cellAddress, cellStyle, formatString);
            case STRING:
                return handleStringCell(cell, cellAddress, cellStyle, formatString);
            case NUMERIC:
                return handleNumericCell(cell, cellAddress, cellStyle, formatString);
            case BOOLEAN:
                return handleBooleanCell(cell, cellAddress, cellStyle, formatString);
            case FORMULA:
                return handleFormulaCell(cell, cellAddress, cellStyle, formatString);
            case ERROR:
                return handleErrorCell(cell, cellAddress, cellStyle, formatString);
            default:
                return new CellContentInfo(null, null, CellContentType.UNKNOWN,
                        cellAddress, formatString, cellStyle);
        }
    }

    /**
     * 处理空单元格
     */
    private static CellContentInfo handleBlankCell(Cell cell, String cellAddress,
                                                   CellStyle cellStyle, String formatString) {
        // 检查是否是真的空单元格还是有样式的空单元格
        boolean hasStyle = cellStyle != null && (
                cellStyle.getFillPattern() != FillPatternType.NO_FILL ||
                        cellStyle.getBorderTop() != BorderStyle.NONE ||
                        cellStyle.getFontIndex() != 0
        );

        if (hasStyle) {
            return new CellContentInfo("", "", CellContentType.EMPTY_STRING,
                    cellAddress, formatString, cellStyle);
        } else {
            return new CellContentInfo(null, null, CellContentType.BLANK,
                    cellAddress, formatString, cellStyle);
        }
    }

    /**
     * 处理字符串单元格
     */
    private static CellContentInfo handleStringCell(Cell cell, String cellAddress,
                                                    CellStyle cellStyle, String formatString) {
        String cellValue = cell.getStringCellValue();

        // 检查是否是富文本
        RichTextString richText = cell.getRichStringCellValue();
        boolean isRichText = richText != null && richText.numFormattingRuns() > 1;

        if (cellValue == null || cellValue.isEmpty()) {
            return new CellContentInfo("", "", CellContentType.EMPTY_STRING,
                    cellAddress, formatString, cellStyle);
        } else if (isRichText) {
            return new CellContentInfo(richText, cellValue, CellContentType.RICH_TEXT,
                    cellAddress, formatString, cellStyle);
        } else {
            return new CellContentInfo(cellValue, cellValue, CellContentType.STRING,
                    cellAddress, formatString, cellStyle);
        }
    }

    /**
     * 处理数字单元格（包括日期）
     */
    private static CellContentInfo handleNumericCell(Cell cell, String cellAddress,
                                                     CellStyle cellStyle, String formatString) {
        double numericValue = cell.getNumericCellValue();

        // 检查是否是日期格式
        if (DateUtil.isCellDateFormatted(cell)) {
            Date dateValue = cell.getDateCellValue();
            CellContentType dateType = determineDateTimeType(cellStyle, formatString);
            String formattedDate = formatDateValue(dateValue, cellStyle);

            return new CellContentInfo(dateValue, formattedDate, dateType,
                    cellAddress, formatString, cellStyle);
        } else {
            // 普通数字
            // 检查是否是整数
            boolean isInteger = Math.floor(numericValue) == numericValue;
            String formattedNumber = formatNumericValue(numericValue, cellStyle);

            return new CellContentInfo(numericValue, formattedNumber, CellContentType.NUMERIC,
                    cellAddress, formatString, cellStyle);
        }
    }

    /**
     * 判断日期时间的具体类型
     */
    private static CellContentType determineDateTimeType(CellStyle style, String format) {
        if (format == null) return CellContentType.DATE;

        format = format.toLowerCase();
        boolean hasDatePart = format.contains("yy") || format.contains("mm") || format.contains("dd");
        boolean hasTimePart = format.contains("hh") || format.contains("ss") || format.contains("mm")
                && !format.contains("mmm");

        if (hasDatePart && hasTimePart) {
            return CellContentType.DATETIME;
        } else if (hasDatePart) {
            return CellContentType.DATE;
        } else if (hasTimePart) {
            return CellContentType.TIME;
        } else {
            return CellContentType.DATE;
        }
    }

    /**
     * 格式化日期值
     */
    private static String formatDateValue(Date date, CellStyle style) {
        if (date == null) return "";

        if (style != null && style.getDataFormatString() != null) {
            // 可以在这里实现自定义格式，这里简单返回默认格式
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return sdf.format(date);
        } else {
            return date.toString();
        }
    }

    /**
     * 格式化数字值
     */
    private static String formatNumericValue(double value, CellStyle style) {
        // 根据样式格式化数字
        if (style != null && style.getDataFormatString() != null) {
            String format = style.getDataFormatString();
            // 这里可以添加复杂的格式化逻辑
            if (format.contains("#,##0") || format.contains("0.00")) {
                // 使用DecimalFormat进行格式化
                java.text.DecimalFormat df = new java.text.DecimalFormat(format);
                return df.format(value);
            }
        }

        // 默认格式化：如果是整数，显示为整数形式
        if (Math.floor(value) == value) {
            return String.valueOf((long) value);
        } else {
            return String.valueOf(value);
        }
    }

    /**
     * 处理布尔单元格
     */
    private static CellContentInfo handleBooleanCell(Cell cell, String cellAddress,
                                                     CellStyle cellStyle, String formatString) {
        boolean boolValue = cell.getBooleanCellValue();
        String formattedValue = boolValue ? "TRUE" : "FALSE";

        return new CellContentInfo(boolValue, formattedValue, CellContentType.BOOLEAN,
                cellAddress, formatString, cellStyle);
    }

    /**
     * 处理公式单元格
     */
    private static CellContentInfo handleFormulaCell(Cell cell, String cellAddress,
                                                     CellStyle cellStyle, String formatString) {
        String formula = cell.getCellFormula();

        try {
            // 获取公式计算后的值
            FormulaEvaluator evaluator = cell.getSheet().getWorkbook().getCreationHelper()
                    .createFormulaEvaluator();
            CellValue cellValue = evaluator.evaluate(cell);

            // 根据计算结果的类型创建信息
            switch (cellValue.getCellType()) {
                case NUMERIC:
                    if (DateUtil.isCellDateFormatted(cell)) {
                        Date date = DateUtil.getJavaDate(cellValue.getNumberValue());
                        String formattedDate = formatDateValue(date, cellStyle);
                        return new CellContentInfo(date, formattedDate, CellContentType.DATE,
                                cellAddress, formatString, cellStyle);
                    } else {
                        double numericValue = cellValue.getNumberValue();
                        String formattedNumber = formatNumericValue(numericValue, cellStyle);
                        return new CellContentInfo(numericValue, formattedNumber, CellContentType.NUMERIC,
                                cellAddress, formatString, cellStyle);
                    }

                case STRING:
                    String stringValue = cellValue.getStringValue();
                    return new CellContentInfo(stringValue, stringValue, CellContentType.STRING,
                            cellAddress, formatString, cellStyle);

                case BOOLEAN:
                    boolean boolValue = cellValue.getBooleanValue();
                    String formattedBool = boolValue ? "TRUE" : "FALSE";
                    return new CellContentInfo(boolValue, formattedBool, CellContentType.BOOLEAN,
                            cellAddress, formatString, cellStyle);

                case ERROR:
                    byte errorCode = cellValue.getErrorValue();
                    return new CellContentInfo(errorCode, "公式错误#" + errorCode, CellContentType.ERROR,
                            cellAddress, formatString, cellStyle);

                default:
                    return new CellContentInfo(formula, formula, CellContentType.FORMULA,
                            cellAddress, formatString, cellStyle);
            }
        } catch (Exception e) {
            // 公式计算失败，返回公式本身
            return new CellContentInfo(formula, formula, CellContentType.FORMULA,
                    cellAddress, formatString, cellStyle);
        }
    }

    /**
     * 处理错误单元格
     */
    private static CellContentInfo handleErrorCell(Cell cell, String cellAddress,
                                                   CellStyle cellStyle, String formatString) {
        byte errorCode = cell.getErrorCellValue();
        String errorMessage = getErrorMessage(errorCode);

        return new CellContentInfo(errorCode, errorMessage, CellContentType.ERROR,
                cellAddress, formatString, cellStyle);
    }

    /**
     * 获取错误码对应的错误信息
     */
    private static String getErrorMessage(byte errorCode) {
        switch (errorCode) {
            case 0x00: return "#NULL!";   // 0x00
            case 0x07: return "#DIV/0!";  // 0x07
            case 0x0F: return "#VALUE!";  // 0x0F
            case 0x17: return "#REF!";    // 0x17
            case 0x1D: return "#NAME?";   // 0x1D
            case 0x24: return "#NUM!";    // 0x24
            case 0x2A: return "#N/A";     // 0x2A
            default: return "#ERR" + errorCode;
        }
    }
}
