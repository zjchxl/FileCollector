package com.example.fileMonitor.dao;

import com.example.fileMonitor.modal.FileMonitor;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.sql.SQLException;
import java.util.List;

@Mapper
public interface FileMonitorDao {
    int saveFileMonitor(FileMonitor fileMonitor) throws SQLException;

    int markAsDeleted(@Param("filePath") String filePath) throws SQLException;

    int updateFileContent(FileMonitor fileMonitor) throws SQLException;

    List<FileMonitor> selectNotUploaded() throws SQLException;

    int markAsUploaded(@Param("id") Long id) throws SQLException;

    List<FileMonitor> selectNotExported() throws SQLException;

    int markAsExported(@Param("id") Long id) throws SQLException;

    List<FileMonitor> selectAllActive() throws SQLException;

    List<FileMonitor> selectByFilePath(@Param("filePath") String filePath) throws SQLException;

    int countActiveFiles() throws SQLException;

    int countUploadedFiles() throws SQLException;
}
