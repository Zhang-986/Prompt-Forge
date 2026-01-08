package com.zzk.domain.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * 存储服务接口
 *
 * @author zzk
 * @since 1.0.0
 */
public interface StorageService {

    /**
     * 上传文件
     *
     * @param key         文件键（路径+文件名）
     * @param inputStream 文件输入流
     * @param contentType 文件类型
     * @param length      文件大小
     * @return 文件访问 URL
     */
    String upload(String key, InputStream inputStream, String contentType, long length);

    /**
     * 上传 MultipartFile
     *
     * @param key  文件键
     * @param file 文件
     * @return 文件访问 URL
     */
    String upload(String key, MultipartFile file);

    /**
     * 获取文件 URL
     *
     * @param key 文件键
     * @return 文件访问 URL
     */
    String getUrl(String key);
}
