package com.example.fileMonitor.modal;

import lombok.Data;

import java.util.Date;

/**
 * 文件监控表
 */
@Data
public class FileMonitor {
    private Long id;                //主键ID
    private String fileName;        //文件名（不包含路径）
    private String filePath;        //文件的完整绝对路径
    private Long fileSize;          //文件大小（字节）
    private String fileContent;     //文件内容（文本文件）或Base64编码
    private String fileType;        //文件扩展名/类型
    private Date lastModified;      //文件最后修改时间
    private String status;          //文件状态：ACTIVE - 活跃（文件存在）、 DELETED - 已删除（文件已被删除）
    private Date createTime;        //记录创建时间（自动）
    private Date updateTime;        //记录更新时间（自动）
    private Boolean isExported;     //是否已导出到Excel：true - 已导出、false - 未导出
    private Boolean ftpUploaded;    //是否已上传到FTP服务器： true - 已上传、false - 未上传
}
