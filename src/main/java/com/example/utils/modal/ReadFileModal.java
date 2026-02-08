package com.example.utils.modal;

import lombok.Data;

@Data
public class ReadFileModal {
    private String readInstructions;        //读取说明
    private ExcelReadModal excelReadModal;  //读取Excel内容
    private String otherContent;            //读取其他文件内容
}
