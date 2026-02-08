package com.example.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;

/**
 * 流转换工具类
 */
@Component
@Slf4j
public class StreamConverter {
    private static final int BUFFER_SIZE = 1024 * 1024; // 1MB

    /**
     * InputStream 转 byte[]
     * @param inputStream 输入流
     * @param closeStream 是否关闭输入流
     * @return byte数组
     */
    public static byte[] toByteArray(InputStream inputStream, boolean closeStream) throws IOException {
        if (inputStream == null) {
            return new byte[0];
        }
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            return outputStream.toByteArray();
        } finally {
            if (closeStream && inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    log.warn("关闭输入流失败", e);
                }
            }
        }
    }

    /**
     * InputStream 转 byte[]（自动关闭流）
     */
    public static byte[] toByteArray(InputStream inputStream) throws IOException {
        return toByteArray(inputStream, true);
    }

    /**
     * byte[] 转 InputStream
     */
    public static InputStream toInputStream(byte[] bytes) {
        if (bytes == null) {
            bytes = new byte[0];
        }
        return new ByteArrayInputStream(bytes);
    }

    /**
     * byte[] 转 BufferedInputStream
     */
    public static BufferedInputStream toBufferedInputStream(byte[] bytes) {
        return new BufferedInputStream(toInputStream(bytes));
    }

    /**
     * InputStream 转字符串（UTF-8）
     */
    public static String toString(InputStream inputStream) throws IOException {
        return toString(inputStream, "UTF-8");
    }

    /**
     * InputStream 转字符串（指定编码）
     */
    public static String toString(InputStream inputStream, String charset) throws IOException {
        byte[] bytes = toByteArray(inputStream);
        return new String(bytes, charset);
    }

    /**
     * 字符串转 InputStream（UTF-8）
     */
    public static InputStream fromString(String content) {
        return fromString(content, "UTF-8");
    }

    /**
     * 字符串转 InputStream（指定编码）
     */
    public static InputStream fromString(String content, String charset) {
        try {
            return new ByteArrayInputStream(content.getBytes(charset));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("不支持的编码格式: " + charset, e);
        }
    }

    /**
     * 复制 InputStream（用于需要多次读取的情况）
     */
    public static byte[] copyInputStream(InputStream inputStream) throws IOException {
        return toByteArray(inputStream, false); // 不关闭原流
    }

    /**
     * 安全地读取 InputStream（限制最大大小）
     */
    public static byte[] toByteArraySafely(InputStream inputStream, long maxSize) throws IOException {
        if (inputStream == null) {
            return new byte[0];
        }
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            long totalBytes = 0;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                totalBytes += bytesRead;

                // 检查是否超过限制
                if (totalBytes > maxSize) {
                    throw new IOException("文件大小超过限制: " + maxSize + " bytes");
                }

                outputStream.write(buffer, 0, bytesRead);
            }

            return outputStream.toByteArray();
        }
    }
}
