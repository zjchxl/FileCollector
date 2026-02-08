package com.example.fileMonitor.service.impl;

import com.example.fileMonitor.dao.FileMonitorDao;
import com.example.fileMonitor.modal.FileMonitor;
import com.example.fileMonitor.service.FileMonitorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;

@Service
@Slf4j
public class FileMonitorServiceImpl implements FileMonitorService {
    @Autowired
    FileMonitorDao fileMonitorDao;

    @Override
    public int saveFileMonitor(FileMonitor fileMonitor) throws SQLException {
        return fileMonitorDao.saveFileMonitor(fileMonitor);
    }

    @Override
    public int markAsDeleted(String filePath) throws SQLException{
        return fileMonitorDao.markAsDeleted(filePath);
    }

    @Override
    public int updateFileContent(FileMonitor fileMonitor) throws SQLException{
        return fileMonitorDao.updateFileContent(fileMonitor);
    }

    @Override
    public List<FileMonitor> selectNotUploaded() throws SQLException{
        return fileMonitorDao.selectNotUploaded();
    }

    @Override
    public int markAsUploaded(Long id) throws SQLException{
        return fileMonitorDao.markAsUploaded(id);
    }

    @Override
    public List<FileMonitor> selectNotExported() throws SQLException{
        return fileMonitorDao.selectNotExported();
    }

    @Override
    public int markAsExported(Long id) throws SQLException{
        return fileMonitorDao.markAsExported(id);
    }

    @Override
    public List<FileMonitor> selectAllActive() throws SQLException{
        return fileMonitorDao.selectAllActive();
    }

    @Override
    public List<FileMonitor> selectByFilePath(String filePath) throws SQLException {
        return fileMonitorDao.selectByFilePath(filePath);
    }

    @Override
    public int countActiveFiles() throws SQLException {
        return fileMonitorDao.countActiveFiles();
    }

    @Override
    public int countUploadedFiles() throws SQLException {
        return fileMonitorDao.countUploadedFiles();
    }
}
