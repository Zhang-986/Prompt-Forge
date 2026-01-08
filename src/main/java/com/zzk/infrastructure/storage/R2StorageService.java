package com.zzk.infrastructure.storage;

import com.zzk.domain.service.StorageService;
import com.zzk.infrastructure.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/**
 * R2 对象存储实现
 *
 * @author zzk
 * @since 1.0.0
 */
@Slf4j
@Service
public class R2StorageService implements StorageService {

    @Value("${storage.r2.endpoint}")
    private String endpoint;

    @Value("${storage.r2.access-key}")
    private String accessKey;

    @Value("${storage.r2.secret-key}")
    private String secretKey;

    @Value("${storage.r2.bucket-name}")
    private String bucketName;

    @Value("${storage.r2.region:auto}")
    private String region;

    @Value("${storage.r2.public-domain:}")
    private String publicDomain;

    private S3Client s3Client;

    @PostConstruct
    public void init() {
        try {
            s3Client = S3Client.builder()
                    .endpointOverride(URI.create(endpoint))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKey, secretKey)))
                    .region(Region.of("us-east-1")) // R2 兼容 S3，通常使用 us-east-1
                    .build();
            log.info("R2 storage service initialized. Endpoint: {}, Bucket: {}", endpoint, bucketName);
        } catch (Exception e) {
            log.error("Failed to initialize R2 storage service", e);
            throw new BusinessException("存储服务初始化失败");
        }
    }

    @Override
    public String upload(String key, InputStream inputStream, String contentType, long length) {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(inputStream, length));

            return getUrl(key);
        } catch (S3Exception e) {
            log.error("Failed to upload file to R2: {}", key, e);
            throw new BusinessException("文件上传失败");
        }
    }

    @Override
    public String upload(String key, MultipartFile file) {
        try {
            return upload(key, file.getInputStream(), file.getContentType(), file.getSize());
        } catch (IOException e) {
            log.error("Failed to read file input stream", e);
            throw new BusinessException("文件读取失败");
        }
    }

    @Override
    public String getUrl(String key) {
        if (publicDomain != null && !publicDomain.isEmpty()) {
            // 确保域名不以 / 结尾，key 不以 / 开头（或者处理双斜杠）
            String domain = publicDomain.endsWith("/") ? publicDomain.substring(0, publicDomain.length() - 1)
                    : publicDomain;
            String path = key.startsWith("/") ? key : "/" + key;
            return domain + path;
        }
        // 如果没有配置 public domain，仅返回 key，前端自行处理
        return key;
    }
}
