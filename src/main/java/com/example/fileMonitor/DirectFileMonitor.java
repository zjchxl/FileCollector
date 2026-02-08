package com.example.fileMonitor;

import com.example.fileMonitor.service.FileMonitorService;
import com.example.utils.RegexExample;
import com.example.utils.modal.ExcelReadModal;
import com.example.fileMonitor.modal.FileMonitor;
import com.example.utils.modal.ReadFileModal;
import com.example.excel.ExcelExportService;
import com.example.ftp.service.FtpService;
import com.example.utils.ReadFileUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.support.CronSequenceGenerator;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 文件监控
 * 后台自动执行
 */
@Component
@Slf4j
public class DirectFileMonitor {

    @Autowired
    private FtpService ftpService;

    @Autowired
    private FileMonitorService fileMonitorService;

    @Autowired
    private ExcelExportService excelExportService;

    @Autowired
    ReadFileUtil readFileUtil;

    @Value("${file.monitor.path}")
    private String monitorPath;

    @Value("${file.monitor.auto-start}")
    private boolean autoStart;

    @Value("${excel.export.schedule.enabled}")
    private boolean excelScheduleEnabled;

    @Value("${excel.export.schedule.cron}")
    private String excelExportCron;

    //文件系统监控服务
    private WatchService watchService;
    private volatile boolean running = false;

    @PostConstruct
    public void initialize() {
        if (autoStart) {
            log.info("initialize === 文件监控系统启动 ===");
            log.info("监控目录: {}", monitorPath);
            try {
                // 1. 启动文件监控
                startFileMonitoring();
                // 2. 启动定时任务
                if (excelScheduleEnabled) {
//                    scheduleExcelExport();
                }
                // 3. 启动FTP自动上传任务
//                startFtpUploadTask();
                log.info("initialize === 文件监控系统启动完成 ===");
            } catch (Exception e) {
                log.error("initialize 系统启动失败", e);
            }
        }
    }

    @PreDestroy
    public void shutdown() {
        stopFileMonitoring();
        log.info("=== 文件监控系统已停止 ===");
    }

    private void startFileMonitoring() {
        try {
            /**
             * 使用Path获取文件系统路径
             */
            Path monitoredPath = Paths.get(monitorPath);

            // 创建监控目录（如果不存在）
            if (!Files.exists(monitoredPath)) {
                Files.createDirectories(monitoredPath);
                log.info("startFileMonitoring 已创建监控目录: {}", monitorPath);
            }

            //获取文件系统监控服务，用于实时监听目录中文件和子目录的变化
            watchService = FileSystems.getDefault().newWatchService();

            /**
             * ENTRY_CREATE 新文件/目录创建
             * ENTRY_DELETE 文件/目录删除
             * ENTRY_MODIFY 文件/目录修改
             * OVERFLOW     事件溢出(文件丢失)
             */
            monitoredPath.register(watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE,
                    StandardWatchEventKinds.ENTRY_MODIFY);

            running = true;

            // 启动监控线程
            Thread monitorThread = new Thread(() -> {
                log.info("startFileMonitoring 文件监控线程启动...");
                monitorDirectory(monitoredPath);
            });

            monitorThread.setName("File-Monitor-Thread");
            monitorThread.setDaemon(false); //设为非守护线程，保持程序运行
            monitorThread.start();

            // 初始扫描目录中的文件
            scanExistingFiles(monitoredPath);
        } catch (Exception e) {
            log.error("startFileMonitoring 文件监控启动失败", e);
            throw new RuntimeException("文件监控启动失败", e);
        }
    }

    private void monitorDirectory(Path monitoredPath) {
        while (running) {
            try {
                //创建轮询，设置超时时间1秒
                WatchKey key = watchService.poll(1, TimeUnit.SECONDS);
                if (key == null) {
                    continue;
                }
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    Path fileName = (Path) event.context();
                    Path fullPath = monitoredPath.resolve(fileName);

                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        log.warn("monitorDirectory 监控事件溢出");
                        continue;
                    }

                    if (Files.isDirectory(fullPath)) {
                        log.debug("monitorDirectory 跳过目录: {}", fileName);
                        continue;
                    }
                    //将文件内容写入数据库中，且同步将文件通过FTP上传至目标服务器中
                    processFileEvent(kind, fullPath);
                }
                //重置key，继续监控
                boolean valid = key.reset();
                if (!valid) {
                    log.error("monitorDirectory 监控Key失效，尝试重新注册...");
                    reRegisterWatchService(monitoredPath);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("monitorDirectory 文件监控被中断");
                break;
            } catch (Exception e) {
                log.error("monitorDirectory 文件监控异常", e);
            }
        }
    }

    /**
     * 将文件内容写入数据库中，且同步将文件通过FTP上传至目标服务器中
     * @param kind
     * @param filePath
     */
    private void processFileEvent(WatchEvent.Kind<?> kind, Path filePath) {
        String action = getEventAction(kind);
        log.info("[{}] 文件: {}", action, filePath.getFileName());
        try {
            switch (kind.name()) {
                case "ENTRY_CREATE"://判断目标文件夹传入新文件
                    handleFileCreate(filePath, "processFileEvent", 0);
                    break;
                case "ENTRY_MODIFY"://判断修改文件
                    handleFileModify(filePath);
                    break;
                case "ENTRY_DELETE"://判断删除文件
                    handleFileDelete(filePath);
                    break;
            }
        } catch (IOException | SQLException e) {
            log.error("processFileEvent 处理文件事件失败: {}", filePath, e);
        }
    }

    private void handleFileCreate(Path filePath, String type, int handleCount) throws IOException {
        log.info("run handleFileCreate  type：{}", type);
        try {
            if(matches(filePath)){
                // 等待文件写入完成（根据文件大小动态调整等待时间）
                long fileSize = Files.size(filePath);
                long waitTime = Math.min(Math.max(fileSize / 1024, 100), 5000); // 100ms到5秒
                log.info("handleFileCreate"+handleCount+" 文件写入时长 waitTime = "+waitTime);
                Thread.sleep(waitTime);
                ReadFileModal readFileModal = readFileContent(filePath);
                String content = readFileModal.getReadInstructions();
                log.info("handleFileCreate"+handleCount+" content = "+content);
                FileMonitor fileMonitor = new FileMonitor();
                fileMonitor.setFileName(filePath.getFileName().toString());
                fileMonitor.setFilePath(filePath.toAbsolutePath().toString());
                fileMonitor.setFileSize(Files.size(filePath));
                fileMonitor.setFileContent(content);
                fileMonitor.setFileType(getFileExtension(filePath));
                fileMonitor.setLastModified(new Date(Files.getLastModifiedTime(filePath).toMillis()));
                fileMonitor.setStatus("ACTIVE");
                fileMonitor.setIsExported(false);
                fileMonitor.setFtpUploaded(false);
                log.info("handleFileCreate handleCount[{}] fileMonitor fileName: {} | FilePath: {} | FileSize: {} | fileType: {} | LastModified: {}", handleCount, fileMonitor.getFileName(), fileMonitor.getFilePath(), fileMonitor.getFileSize(), fileMonitor.getFileType(), fileMonitor.getLastModified());
                fileMonitorService.saveFileMonitor(fileMonitor);
                log.info("handleFileCreate"+handleCount+" 已保存到数据库，ID: {}", fileMonitor.getId());
                // 自动上传到FTP
                boolean uploaded = ftpService.uploadFile(filePath.toFile());
                if (uploaded) {
                    fileMonitorService.markAsUploaded(fileMonitor.getId());
                    log.info("文件已上传到FTP服务器");
                }
            }
        } catch (Exception e) {
            log.error("handleFileCreate"+handleCount+" 处理新增文件失败: {}", filePath, e);
        }
    }

    private void handleFileModify(Path filePath) throws IOException, SQLException {
        log.info("run handleFileModify");
        if(matches(filePath)){
            ReadFileModal readFileModal = readFileContent(filePath);
            String content = readFileModal.getReadInstructions();
            log.info("handleFileModify content = "+content);
            FileMonitor fileMonitor = new FileMonitor();
            fileMonitor.setFilePath(filePath.toAbsolutePath().toString());
            fileMonitor.setFileSize(Files.size(filePath));
            fileMonitor.setFileContent(content);
            fileMonitor.setLastModified(new Date(Files.getLastModifiedTime(filePath).toMillis()));
            log.info("handleFileModify fileMonitor FilePath: {} | FileSize: {} | LastModified: {}", fileMonitor.getFilePath(), fileMonitor.getFileSize(), fileMonitor.getLastModified());
            int updated = fileMonitorService.updateFileContent(fileMonitor);
            if (updated > 0) {
                log.info("文件内容已更新");
                // 重新上传到FTP
                boolean uploaded = ftpService.uploadFile(filePath.toFile());
                if (uploaded) {
                    // 更新上传状态（需要先查询ID）
                    List<FileMonitor> files = fileMonitorService.selectByFilePath(filePath.toString());
                    if (!files.isEmpty()) {
                        fileMonitorService.markAsUploaded(files.get(0).getId());
                    }
                }
            }
        }
    }

    private void handleFileDelete(Path filePath) throws SQLException{
        log.info("run handleFileDelete fileName: {}", filePath.getFileName());
        if(matches(filePath)){
            int deleted = fileMonitorService.markAsDeleted(filePath.toAbsolutePath().toString());
            if (deleted > 0) {
                log.info("handleFileDelete 文件标记为已删除");
            }
        }
    }

    private void scanExistingFiles(Path monitoredPath) {
        log.info("scanExistingFiles 开始扫描目录中的现有文件...");
        int count = 0;
        try {
            Files.walk(monitoredPath, 1)
                    .filter(Files::isRegularFile)
                    .forEach(file -> {
                        try {
                            handleFileCreate(file, "scanExistingFiles", 1);
                        } catch (IOException e) {
                            log.error("scanExistingFiles 扫描文件失败: {}", file, e);
                        }
                        try {
                            // 检查是否已记录
                            List<FileMonitor> existing = fileMonitorService.selectByFilePath(file.toString());
                            if (existing.isEmpty()) {
                                handleFileCreate(file, "scanExistingFiles", 1);
                            }
                        } catch (Exception e) {
                            log.error("scanExistingFiles 扫描文件失败: {}", file, e);
                        }
                    });

            log.info("scanExistingFiles 目录扫描完成，共处理 {} 个文件", count);
        } catch (Exception e) {
            log.error("scanExistingFiles 扫描目录失败", e);
        }
    }

    private void scheduleExcelExport() {
        log.info("scheduleExcelExport 配置Excel定时导出，计划: {}", excelExportCron);

        Thread exportThread = new Thread(() -> {
            while (running) {
                try {
                    // 解析cron表达式，计算下一次执行时间
                    CronSequenceGenerator cron = new CronSequenceGenerator(excelExportCron);
                    Date nextRun = cron.next(new Date());
                    long delay = nextRun.getTime() - System.currentTimeMillis();

                    if (delay > 0) {
                        log.info("scheduleExcelExport 下一次Excel导出计划在: {}", nextRun);
                        Thread.sleep(delay);
                    }

                    // 执行导出
                    performExcelExport();

                } catch (Exception e) {
                    log.error("scheduleExcelExport Excel导出调度异常", e);
                    try {
                        Thread.sleep(60000); // 出错后等待1分钟
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        });

        exportThread.setName("Excel-Export-Scheduler");
        exportThread.setDaemon(true);
        exportThread.start();
    }

    private void startFtpUploadTask() {
        log.info("startFtpUploadTask 启动FTP自动上传任务...");
        Thread uploadThread = new Thread(() -> {
            while (running) {
                try {
                    // 每5分钟检查一次未上传的文件
                    Thread.sleep(5 * 60 * 1000);

                    List<FileMonitor> notUploaded = fileMonitorService.selectNotUploaded();
                    if (!notUploaded.isEmpty()) {
                        log.info("startFtpUploadTask 发现 {} 个未上传的文件，开始上传...", notUploaded.size());

                        for (FileMonitor file : notUploaded) {
                            try {
                                File fileToUpload = new File(file.getFilePath());
                                if (fileToUpload.exists()) {
                                    boolean uploaded = ftpService.uploadFile(fileToUpload);
                                    if (uploaded) {
                                        fileMonitorService.markAsUploaded(file.getId());
                                        log.info("已上传: {}", file.getFileName());
                                    }
                                } else {
                                    log.warn("文件不存在: {}", file.getFilePath());
                                    fileMonitorService.markAsDeleted(file.getFilePath());
                                }
                            } catch (Exception e) {
                                log.error("上传文件失败: {}", file.getFileName(), e);
                            }
                        }
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("startFtpUploadTask FTP上传任务异常", e);
                }
            }
        });

        uploadThread.setName("FTP-Upload-Task");
        uploadThread.setDaemon(true);
        uploadThread.start();
    }

    private void performExcelExport() {
        try {
            log.info("performExcelExport 开始执行Excel导出...");
            File excelFile = excelExportService.exportToExcel();
            log.info("performExcelExport Excel导出完成: {}", excelFile.getAbsolutePath());
        } catch (IOException | SQLException e) {
            log.error("performExcelExport Excel导出失败", e);
        }
    }

    public void manualExportExcel() {
        performExcelExport();
    }

    public void stopFileMonitoring() {
        running = false;
        try {
            if (watchService != null) {
                watchService.close();
            }
        } catch (Exception e) {
            log.error("stopFileMonitoring 关闭监控服务失败", e);
        }
    }

    private void reRegisterWatchService(Path monitoredPath) {
        try {
            if (watchService != null) {
                watchService.close();
            }
            watchService = FileSystems.getDefault().newWatchService();
            monitoredPath.register(watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE,
                    StandardWatchEventKinds.ENTRY_MODIFY);
            log.info("reRegisterWatchService 监控服务重新注册成功");
        } catch (Exception e) {
            log.error("reRegisterWatchService 重新注册监控服务失败", e);
        }
    }

    /**
     * 读取文件内容
     * @param filePath
     * @return
     * @throws IOException
     */
    private ReadFileModal readFileContent(Path filePath) throws IOException {
        ReadFileModal readFileModal = new ReadFileModal();
        String fileName = filePath.getFileName().toString().toLowerCase();
        if (fileName.endsWith(".txt") || fileName.endsWith(".log") ||
                fileName.endsWith(".csv") || fileName.endsWith(".xml") ||
                fileName.endsWith(".json") || fileName.endsWith(".properties") ||
                fileName.endsWith(".java") || fileName.endsWith(".sql") ||
                fileName.endsWith(".html") || fileName.endsWith(".htm") ||
                fileName.endsWith(".js") || fileName.endsWith(".css")) {// 文本文件直接读取
            readFileModal.setReadInstructions(new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8));
            return readFileModal;
        }else if (fileName.endsWith(".xlsx") || fileName.endsWith(".xls")) {// Excel文件特殊处理
            ExcelReadModal excelReadModal = new ExcelReadModal();
            excelReadModal = readFileUtil.readFile(filePath.toFile(), excelReadModal);
            log.info("readFileContent excelReadModal.toString() {}", excelReadModal.toString());
            readFileModal.setReadInstructions("读取Excel内容");
            readFileModal.setExcelReadModal(excelReadModal);
            return readFileModal;
        }else if (fileName.endsWith(".doc") || fileName.endsWith(".docx")) {// Word文件特殊处理
            readFileModal.setReadInstructions("[Word文件内容无法直接显示]");
            return readFileModal;
        }else if (fileName.endsWith(".pdf")) {// PDF文件特殊处理
            readFileModal.setReadInstructions("[PDF文件内容无法直接显示]");
            return readFileModal;
        }else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") ||
                fileName.endsWith(".png") || fileName.endsWith(".gif") ||
                fileName.endsWith(".bmp")) {// 图片文件
            readFileModal.setReadInstructions("[图片文件]");
            return readFileModal;
        }else {// 其他文件作为二进制处理
            byte[] bytes = Files.readAllBytes(filePath);
            if (bytes.length > 10 * 1024 * 1024) { // 大于10MB不保存完整内容
                readFileModal.setReadInstructions("[文件过大，仅保存基本信息]");
                return readFileModal;
            }
            readFileModal.setReadInstructions(Base64.getEncoder().encodeToString(bytes));
            return readFileModal;
        }
    }

    private String getFileExtension(Path filePath) {
        String fileName = filePath.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex > 0 ? fileName.substring(dotIndex + 1).toLowerCase() : "";
    }

    private String getEventAction(WatchEvent.Kind<?> kind) {
        if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
            return "新增";
        } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
            return "修改";
        } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
            return "删除";
        }
        return "未知";
    }

    /**
     * 设置文档名称匹配规则
     * @param filePath
     * @return
     */
    private boolean matches(Path filePath){
        String fileName = filePath.getFileName().toString();
        log.info("matches fileName:{}", fileName);
        if(null != fileName && fileName != "" && !"".equals(fileName)){
            //匹配文件名称符合规则的文件
            List<String> patterns = new ArrayList<>();
            patterns.add(".*统计表\\.xlsx");
            RegexExample regexExample = new RegexExample();
            regexExample.setPatterns(patterns);
            return regexExample.matches(fileName, 0);
        }else {
            return false;
        }
    }
}
