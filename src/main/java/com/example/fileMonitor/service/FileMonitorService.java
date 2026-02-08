package com.example.fileMonitor.service;

import com.example.fileMonitor.modal.FileMonitor;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;

@Service
public interface FileMonitorService {
    int saveFileMonitor(FileMonitor fileMonitor) throws SQLException;

    int markAsDeleted(String filePath) throws SQLException;

    int updateFileContent(FileMonitor fileMonitor) throws SQLException;

    List<FileMonitor> selectNotUploaded() throws SQLException;

    int markAsUploaded(Long id) throws SQLException;

    List<FileMonitor> selectNotExported() throws SQLException;

    int markAsExported(Long id) throws SQLException;

    List<FileMonitor> selectAllActive() throws SQLException;

    List<FileMonitor> selectByFilePath(String filePath) throws SQLException;

    int countActiveFiles() throws SQLException;

    int countUploadedFiles() throws SQLException;
}
