package com.example.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ExcelValidator {
    /**
     * 通过文件扩展名判断
     * @param file 文件对象
     * @return "xls", "xlsx", 或 null
     */
    public static String getExcelTypeByExtension(File file) {
        if (null == file || !file.exists() || file.length() < 8) {
            return null;
        }
        String fileName = file.getName().toLowerCase();
        if (fileName.endsWith(".xls")) {
            return "xls";
        } else if (fileName.endsWith(".xlsx")) {
            return "xlsx";
        } else if (fileName.endsWith(".xlsm")) {
            return "xlsm";  // Excel 宏文件
        }

        return null;
    }

    /**
     * 检查是否为Excel文件（基于扩展名）
     */
    public static boolean isExcelFile(File file) {
        String type = getExcelTypeByExtension(file);
        return "xls".equals(type) || "xlsx".equals(type) || "xlsm".equals(type);
    }

    //Excel文件魔数（xls文件头部签名）
    private static final byte[] XLS_SIGNATURE = {
            (byte) 0xD0, (byte) 0xCF, (byte) 0x11, (byte) 0xE0,
            (byte) 0xA1, (byte) 0xB1, (byte) 0x1A, (byte) 0xE1
    };

    //Excel文件魔数（xlsx文件头部签名，实际上是ZIP格式）
    private static final byte[] XLSX_SIGNATURE = {
            (byte) 0x50, (byte) 0x4B, (byte) 0x03, (byte) 0x04
    };

    /**
     * 通过文件魔数判断Excel类型
     */
    public static String getExcelTypeByMagicNumber(InputStream inputStream) throws IOException {
        if (null == inputStream) {
            return null;
        }
        byte[] header = new byte[8];
        int bytesRead = inputStream.read(header);
        if (bytesRead < 8) {
            return null;
        }
        // 检查是否为 .xls
        if (startsWith(header, XLS_SIGNATURE)) {
            return "xls";
        }
        // 检查是否为 .xlsx (.xlsx实际上是ZIP格式)
        if (startsWith(header, XLSX_SIGNATURE)) {
            return "xlsx";
        }
        // 检查是否为 .xlsm (也是ZIP格式，需要进一步检查)
        if (startsWith(header, XLSX_SIGNATURE)) {
            // .xlsm 也是ZIP格式，需要通过文件内容进一步判断
            // 这里简单返回 xlsx，如果需要精确区分可以继续检查
            return "xlsx";
        }
        return null;
    }

    private static boolean startsWith(byte[] source, byte[] prefix) {
        if (source.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (source[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

}
