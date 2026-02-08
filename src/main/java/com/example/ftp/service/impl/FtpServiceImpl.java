package com.example.ftp.service.impl;

import com.example.ftp.modal.FtpServer;
import com.example.ftp.dao.FtpServiceDao;
import com.example.ftp.service.FtpService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;

@Service
@Slf4j
public class FtpServiceImpl implements FtpService {
    @Autowired
    private FtpServiceDao ftpServerMapper;

    @Override
    public boolean uploadFile(File file) throws SQLException {
        FtpServer ftpServer = ftpServerMapper.selectActiveFtpServer();
        if (ftpServer == null) {
            log.error("没有可用的FTP服务器配置");
            return false;
        }
        FTPClient ftpClient = new FTPClient();
        try {
            // 连接FTP服务器
            ftpClient.connect(ftpServer.getHost(), ftpServer.getPort());
            int reply = ftpClient.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftpClient.disconnect();
                log.error("FTP服务器拒绝连接");
                return false;
            }

            // 登录
            if (!ftpClient.login(ftpServer.getUserName(), ftpServer.getPassword())) {
                log.error("FTP登录失败");
                return false;
            }

            // 设置传输模式
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            ftpClient.enterLocalPassiveMode();

            // 创建远程目录（如果不存在）
            String remoteDir = ftpServer.getRemoteDir();
            if (!ftpClient.changeWorkingDirectory(remoteDir)) {
                String[] directories = remoteDir.split("/");
                StringBuilder path = new StringBuilder();
                for (String dir : directories) {
                    if (!dir.isEmpty()) {
                        path.append("/").append(dir);
                        if (!ftpClient.changeWorkingDirectory(path.toString())) {
                            if (ftpClient.makeDirectory(path.toString())) {
                                ftpClient.changeWorkingDirectory(path.toString());
                            } else {
                                log.error("创建远程目录失败: {}", path);
                                return false;
                            }
                        }
                    }
                }
            }

            // 上传文件
            try (InputStream inputStream = new FileInputStream(file)) {
                boolean uploaded = ftpClient.storeFile(file.getName(), inputStream);
                if (uploaded) {
                    log.info("文件上传成功: {}", file.getName());
                    return true;
                } else {
                    log.error("文件上传失败: {}", file.getName());
                    return false;
                }
            }

        } catch (Exception e) {
            log.error("FTP上传异常", e);
            return false;
        } finally {
            try {
                if (ftpClient.isConnected()) {
                    ftpClient.logout();
                    ftpClient.disconnect();
                }
            } catch (IOException e) {
                log.error("关闭FTP连接异常", e);
            }
        }
    }

    @Override
    public void uploadAllNotUploaded() {
        // 可以定时调用，上传所有未上传的文件
    }
}
