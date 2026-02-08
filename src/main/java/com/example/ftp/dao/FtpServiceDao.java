package com.example.ftp.dao;

import com.example.ftp.modal.FtpServer;
import org.apache.ibatis.annotations.Mapper;

import java.sql.SQLException;

@Mapper
public interface FtpServiceDao {
    FtpServer selectActiveFtpServer() throws SQLException;
}
