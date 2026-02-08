package com.example.excel;

import com.example.fileMonitor.modal.FileMonitor;
import com.example.fileMonitor.service.FileMonitorService;
import com.example.ftp.service.FtpService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * excel导出
 */
@Service
@Slf4j
public class ExcelExportService {
    @Autowired
    private FtpService ftpService;
    @Autowired
    private FileMonitorService fileMonitorService;

    @Value("${excel.export.path}")
    private String exportPath;

    public File exportToExcel() throws IOException, SQLException {
        List<FileMonitor> files = new ArrayList<>();

        // 创建Excel工作簿
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("文件监控数据");

        // 创建标题行
        Row headerRow = sheet.createRow(0);
        String[] headers = {"ID", "文件名", "文件路径", "文件大小(B)", "文件类型",
                "最后修改时间", "创建时间", "状态"};

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
        }

        // 填充数据
        int rowNum = 1;
        for (FileMonitor file : files) {
            Row row = sheet.createRow(rowNum++);

            row.createCell(0).setCellValue(file.getId());
            row.createCell(1).setCellValue(file.getFileName());
            row.createCell(2).setCellValue(file.getFilePath());
            row.createCell(3).setCellValue(file.getFileSize());
            row.createCell(4).setCellValue(file.getFileType());

            if (file.getLastModified() != null) {
                row.createCell(5).setCellValue(file.getLastModified());
            }

            if (file.getCreateTime() != null) {
                row.createCell(6).setCellValue(file.getCreateTime());
            }

            row.createCell(7).setCellValue(file.getStatus());
        }

        // 自动调整列宽
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        // 保存Excel文件
        File exportDir = new File(exportPath);
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }

        String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String fileName = "file_monitor_" + timestamp + ".xlsx";
        File excelFile = new File(exportDir, fileName);

        try (FileOutputStream outputStream = new FileOutputStream(excelFile)) {
            workbook.write(outputStream);
        }

        workbook.close();

        log.info("Excel文件已生成: {}", excelFile.getAbsolutePath());

        // 上传到FTP
        ftpService.uploadFile(excelFile);

        // 标记为已导出
        for (FileMonitor file : files) {
            fileMonitorService.markAsExported(file.getId());
        }
        return excelFile;
    }
}
