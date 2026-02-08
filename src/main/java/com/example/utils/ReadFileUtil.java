package com.example.utils;

import com.example.utils.modal.ExcelReadModal;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 读取文件工具类
 */
@Component
@Slf4j
public class ReadFileUtil{
    /**
     * 读取文件内容
     * @param file
     * @return
     * @throws IOException
     */
    public <T> T readFile(File file, T t) throws IOException{
        if(null == file || !file.exists() || file.length() < 8){
            return null;
        }
        if(null == t){
            return null;
        }
        if(ExcelValidator.isExcelFile(file)){//判断为Excel文件
            log.info("readFile 判断为Excel类型文件");
            return readExcelFile(file, t);
        }else {//判断为其他类型文件
            log.info("readFile 判断为其它类型文件");
        }
        return t;
    }

    /**
     * 读取Excel文件内容
     * @param file
     * @return
     * @throws IOException
     */
    private <T> T readExcelFile(File file, T t) throws IOException{
        if (null == file || !file.exists() || file.length() < 8) {
            return null;
        }
        if(null == t){
            return null;
        }
        try(InputStream inputStream = new FileInputStream(file)){
            //此处是为了防止使用文件魔数判断Excel类型后，无法再次读取文件内容。
            byte[] byteArray = StreamConverter.toByteArray(inputStream);
            if(null == byteArray || byteArray.length == 0){
                return null;
            }
            InputStream suffixInputStream = StreamConverter.toInputStream(byteArray);
            InputStream bodyInputStream = StreamConverter.toInputStream(byteArray);
            String suffix = ExcelValidator.getExcelTypeByMagicNumber(suffixInputStream);
            if("xls".equals(suffix)){
                log.info("判断为xls类型文件");
                t = readXlsFile(bodyInputStream, t);
            }else if("xlsx".equals(suffix)){
                log.info("判断为xlsx类型文件");
                t = readXlsxFile(bodyInputStream, t);
            }else if("xlsm".equals(suffix)){//Excel 宏文件
                //暂时不处理此类型文档
                return null;
            }else
                return null;
        }
        return t;
    }

    /**
     * 读取.xlsx文件内容
     * 暂未处理合并单元格
     * @param inputStream
     */
    private  <T> T readXlsxFile(InputStream inputStream, T t){
        log.info("readXlsFile 读取.xlsx文件内容 start");
        if(null == inputStream){
            return null;
        }
        if(null == t){
            return null;
        }
        Class<?> clazz = t.getClass();
        try (XSSFWorkbook workbook = new XSSFWorkbook(inputStream)){
            XSSFSheet sheet = workbook.getSheetAt(0);//读取Excel内第一个sheet页
            if(null != sheet){
                String sheetName = sheet.getSheetName();//sheet页名称
                int lastRowNum = sheet.getLastRowNum();//获取Excel-Sheet页中的行数下标
                if(lastRowNum != 0){
                    //创建标题集合
                    List<String> titles = new ArrayList<>();
                    //创建内容集合
                    List<List<Map<String, Object>>> dataList = new ArrayList<>();
                    //创建标题Map
                    Map<Integer, Object> titleMap = new HashMap<>();
                    //遍历sheet页内的行元素
                    for (int i = 0; i <= lastRowNum; i++) {
                        XSSFRow row = sheet.getRow(i);//获取行对象
                        if(null != row){
                            int lastCellNum = row.getLastCellNum();//获取一行中的列数长度
                            if(lastCellNum != 0){
                                //创建存储行数据集合
                                List<Map<String, Object>> rowList = new ArrayList<>();
                                //遍历每一行内的单元格元素
                                for (int j = 0; j < lastCellNum; j++) {
                                    XSSFCell cell = row.getCell(j);//获取单元格对象
                                    if(null != cell){
                                        setCellContent(sheetName, cell, i, j, titleMap, titles, rowList);
                                    }
                                }
                                if(i != 0){
                                    dataList.add(rowList);
                                }
                            }
                        }
                    }
                    if (ExcelReadModal.class.isAssignableFrom(clazz)) {
                        t = (T) processToExcelModal((ExcelReadModal) t, titles, dataList);
                    }
                }
            }
        } catch (IOException e) {
            log.error("readXlsxFile 读取xlsx文档IO异常：", e);
        }
        log.info("readXlsFile 读取.xlsx文件内容 end");
        return t;
    }

    /**
     * 读取.xls文件内容
     * 暂未处理合并单元格
     * @param inputStream
     */
    private  <T> T readXlsFile(InputStream inputStream, T t){
        log.info("readXlsFile 读取.xls文件内容 start");
        if(null == inputStream){
            return null;
        }
        if(null == t){
            return null;
        }
        Class<?> clazz = t.getClass();
        try (HSSFWorkbook workbook = new HSSFWorkbook(inputStream)){
            HSSFSheet sheet = workbook.getSheetAt(0);//读取Excel内第一个sheet页
            if(null != sheet){
                String sheetName = sheet.getSheetName();//sheet页名称
                int lastRowNum = sheet.getLastRowNum();//获取Excel-Sheet页中的行数
                if(lastRowNum != 0){
                    //创建标题集合
                    List<String> titles = new ArrayList<>();
                    //创建内容集合
                    List<List<Map<String, Object>>> dataList = new ArrayList<>();
                    //创建标题Map
                    Map<Integer, Object> titleMap = new HashMap<>();
                    //遍历sheet页内的行元素
                    for (int i = 0; i < lastRowNum; i++) {
                        HSSFRow row = sheet.getRow(i);//获取行对象
                        if(null != row){
                            int lastCellNum = row.getLastCellNum();//获取一行中的列数
                            if(lastCellNum != 0){
                                //创建存储行数据集合
                                List<Map<String, Object>> rowList = new ArrayList<>();
                                //遍历每一行内的单元格元素
                                for (int j = 0; j < lastCellNum; j++) {
                                    HSSFCell cell = row.getCell(j);//获取单元格对象
                                    if(null != cell){
                                        setCellContent(sheetName, cell, i, j, titleMap, titles, rowList);
                                    }
                                }
                                dataList.add(rowList);
                            }
                        }
                    }
                    if (ExcelReadModal.class.isAssignableFrom(clazz)) {
                        t = (T) processToExcelModal((ExcelReadModal) t, titles, dataList);
                    }
                }
            }
        } catch (IOException e) {
            log.error("readXlsFile 读取xlsx文档IO异常：", e);
        }
        log.info("readXlsFile 读取.xls文件内容 end");
        return t;
    }

    private void setCellContent(String sheetName, Cell cell, int i, int j, Map<Integer, Object> titleMap, List<String> titles, List<Map<String, Object>> rowList){
        CellContentReader.CellContentInfo cellInfo = CellContentReader.getCellContentInfo(cell);
        if(null != cellInfo){
            //创建存储单元格数据数组
            Map<String, Object> cellMap = new HashMap<>();
            String cellAddress = cellInfo.getCellAddress();//单元格地址
            String fromString = cellInfo.getFormatString();//格式字符串
            Object formattedValue = cellInfo.getFormattedValue();//格式化后的值
            Object value = cellInfo.getValue();//原始值
            log.info("{}：第{}行, 第{}列, {}(value) {}(formattedValue), {}(formattedValue.toString), {}(fromString), {}(cellAddress)", sheetName, i, j, value, formattedValue, null == formattedValue ? "formattedValue is null" : formattedValue.toString(), fromString, cellAddress);
            if(null != formattedValue && formattedValue != "" && !"".equals(formattedValue)) {
                if(i == 0){//判断为第一行标题行（默认第一行为标题行，如标题行占用多行须另外处理）
                    titles.add(formattedValue.toString());
                    titleMap.put(Integer.valueOf(j), formattedValue);
                }else {//遍历其他内容行
                    Object cellValue = titleMap.get(Integer.valueOf(j));
                    if(null != cellValue){
                        cellMap.put(cellValue.toString(), formattedValue);
                    }
                }
                rowList.add(cellMap);
            }
        }
    }

    private ExcelReadModal processToExcelModal(ExcelReadModal modal, List<String> titles, List<List<Map<String, Object>>> dataList) {
        modal.setTitles(titles);
        modal.setDataList(dataList);
        return modal;
    }

}
