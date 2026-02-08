package com.example.utils;

import org.apache.poi.ss.usermodel.*;

public class FileUtils {
    public static CellStyle getCellStyle(Workbook wb, int styleType){
        CellStyle cellStyle = wb.createCellStyle();
        Font font = wb.createFont();//获取单元格字体对象
        switch (styleType){
            case 0://标题样式
                font.setBold(true);//设置字体加粗
                font.setFontName("宋体");//设置字体风格
                font.setFontHeightInPoints((short) 10);//设置字体大小
                cellStyle.setFont(font);//设置字体样式
                //设置对齐方式：水平垂直居中
                cellStyle.setAlignment(HorizontalAlignment.CENTER);
                cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
                // 设置背景颜色为灰色
                cellStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
                cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                // 设置黑色边框
                cellStyle.setBorderTop(BorderStyle.THIN);
                cellStyle.setBorderBottom(BorderStyle.THIN);
                cellStyle.setBorderLeft(BorderStyle.THIN);
                cellStyle.setBorderRight(BorderStyle.THIN);

                break;
            case 1://普通内容样式
                // 设置字体：宋体、10号
                font.setFontName("宋体");
                font.setFontHeightInPoints((short) 10);
                cellStyle.setFont(font);
                break;
        }
        return cellStyle;
    }
}
