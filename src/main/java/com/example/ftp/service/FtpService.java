package com.example.ftp.service;

import org.springframework.stereotype.Service;

import java.io.File;
import java.sql.SQLException;

@Service
public interface FtpService {
    boolean uploadFile(File file) throws SQLException;

    void uploadAllNotUploaded();
}
