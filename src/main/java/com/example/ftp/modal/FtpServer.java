package com.example.ftp.modal;

import lombok.Data;

/**
 * FTP服务器配置表
 */
@Data
public class FtpServer {
    private Integer id;         //主键ID
    private String host;        //FTP服务器主机地址/IP
    private Integer port;       //FTP服务器端口号（默认21）
    private String userName;    //FTP登录用户名
    private String password;    //FTP登录密码
    private String remoteDir;   //远程服务器上的目标目录
    private Boolean isActive;   //是否启用该FTP配置：true - 启用、false - 禁用
    private String description; //配置描述信息: "生产环境FTP"、"测试服务器"
}
