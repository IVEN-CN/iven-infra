package io.github.ivencn.infra.storage.aliyun;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.aliyun.oss.model.OSSObject;
import io.github.ivencn.infra.storage.StorageProperties;
import io.github.ivencn.infra.storage.api.ObjectLocation;
import io.github.ivencn.infra.storage.api.ObjectStorage;
import io.github.ivencn.infra.storage.api.StorageObjectMetadata;
import io.github.ivencn.infra.core.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.util.Date;

/**
 * 阿里云 OSS 对象存储适配器，负责将统一的 {@link ObjectStorage} 操作转换为阿里云 OSS SDK 调用。
 * 公共端点客户端（用于生成预签名 URL）在内部按需创建，不暴露为 Spring Bean。
 */
@Slf4j
public class AliyunOssObjectStorage implements ObjectStorage {

    private final OSS ossClient;
    private final OSS publicOssClient;
    private final StorageProperties storageProperties;

    public AliyunOssObjectStorage(OSS ossClient, StorageProperties storageProperties) {
        this.ossClient = ossClient;
        this.storageProperties = storageProperties;
        this.publicOssClient = createPublicClient(ossClient, storageProperties);
    }

    private static OSS createPublicClient(OSS internalClient, StorageProperties properties) {
        StorageProperties.AliyunOss aliyunOss = properties.getAliyunOss();
        if (aliyunOss == null) {
            return internalClient;
        }
        String publicEndpoint = aliyunOss.getPublicEndpoint();
        String endpoint = aliyunOss.getEndpoint();
        if (!StringUtils.hasText(publicEndpoint) || publicEndpoint.equals(endpoint)) {
            log.debug("Aliyun OSS publicEndpoint not configured or same as endpoint, reusing internal client");
            return internalClient;
        }
        OSS client = new OSSClientBuilder().build(
                publicEndpoint,
                aliyunOss.getAccessKeyId(),
                aliyunOss.getAccessKeySecret());
        log.info("Aliyun OSS public client initialized: {}", publicEndpoint);
        return client;
    }

    private ObjectLocation location(String key) {
        return new ObjectLocation(storageProperties.getBucket(), key);
    }

    @Override
    public String providerName() {
        return "aliyun-oss";
    }

    @Override
    public void ensureBucket() {
        String bucket = storageProperties.getBucket();
        try {
            if (!ossClient.doesBucketExist(bucket)) {
                ossClient.createBucket(bucket);
                log.info("Created Aliyun OSS bucket: {}", bucket);
            } else {
                log.debug("Aliyun OSS bucket already exists: {}", bucket);
            }
        } catch (OSSException | ClientException e) {
            log.error("Failed to initialize Aliyun OSS bucket: {}", bucket, e);
            throw new RuntimeException("Failed to initialize Aliyun OSS bucket: " + bucket, e);
        }
    }

    @Override
    public void put(String key, InputStream inputStream) {
        if (inputStream == null) {
            throw new IllegalArgumentException("InputStream cannot be null");
        }
        ObjectLocation location = location(key);
        try {
            ossClient.putObject(location.bucket(), location.objectKey(), inputStream);
            log.debug("File saved successfully: {}/{}", location.bucket(), location.objectKey());
        } catch (OSSException | ClientException e) {
            log.error("Error saving file to Aliyun OSS: {}/{}", location.bucket(), location.objectKey(), e);
            throw new RuntimeException("Failed to save file: " + key, e);
        }
    }

    @Override
    public Resource get(String key) {
        ObjectLocation location = location(key);
        try (OSSObject ossObject = ossClient.getObject(location.bucket(), location.objectKey());
                InputStream inputStream = ossObject.getObjectContent()) {
            byte[] data = inputStream.readAllBytes();
            log.debug("File loaded successfully: {}/{}", location.bucket(), location.objectKey());
            return resource(key, data);
        } catch (OSSException e) {
            if ("NoSuchKey".equals(e.getErrorCode()) || "NoSuchBucket".equals(e.getErrorCode())) {
                log.warn("File not found in Aliyun OSS: {}/{}", location.bucket(), location.objectKey());
                throw new BizException(HttpStatus.NOT_FOUND, "File not found: " + key);
            }
            log.error("Aliyun OSS error while loading file: {}/{}", location.bucket(), location.objectKey(), e);
            throw new RuntimeException("Failed to load file from Aliyun OSS: " + key, e);
        } catch (Exception e) {
            log.error("Error loading file from Aliyun OSS: {}/{}", location.bucket(), location.objectKey(), e);
            throw new RuntimeException("Failed to load file: " + key, e);
        }
    }

    @Override
    public void delete(String key) {
        ObjectLocation location = location(key);
        try {
            ossClient.deleteObject(location.bucket(), location.objectKey());
            log.debug("File deleted successfully from Aliyun OSS: {}/{}", location.bucket(), location.objectKey());
        } catch (OSSException | ClientException e) {
            log.error("Error deleting file from Aliyun OSS: {}/{}", location.bucket(), location.objectKey(), e);
            throw new RuntimeException("Failed to delete file from Aliyun OSS: " + key, e);
        }
    }

    @Override
    public void checkHealth() {
        try {
            ossClient.doesBucketExist(storageProperties.getBucket());
        } catch (OSSException | ClientException e) {
            throw new RuntimeException("Aliyun OSS health check failed", e);
        }
    }

    @Override
    public String getPresignedUploadUrl(String key, String contentType, Duration expiry) {
        ObjectLocation location = location(key);
        try {
            Date expiration = new Date(System.currentTimeMillis() + expiry.toMillis());
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(location.bucket(),
                    location.objectKey(), HttpMethod.PUT);
            request.setExpiration(expiration);
            if (contentType != null && !contentType.isBlank()) {
                request.setContentType(contentType);
            }
            URL url = publicOssClient.generatePresignedUrl(request);
            return url.toString();
        } catch (OSSException | ClientException e) {
            log.error(
                    "Error generating presigned upload URL from Aliyun OSS: {}/{}",
                    location.bucket(),
                    location.objectKey(),
                    e);
            throw new RuntimeException("Failed to generate presigned upload URL: " + key, e);
        }
    }

    @Override
    public String getPresignedDownloadUrl(String key, Duration expiry) {
        ObjectLocation location = location(key);
        try {
            Date expiration = new Date(System.currentTimeMillis() + expiry.toMillis());
            URL url = publicOssClient
                    .generatePresignedUrl(location.bucket(), location.objectKey(), expiration, HttpMethod.GET);
            return url.toString();
        } catch (OSSException | ClientException e) {
            log.error(
                    "Error generating presigned download URL from Aliyun OSS: {}/{}",
                    location.bucket(),
                    location.objectKey(),
                    e);
            throw new RuntimeException("Failed to generate presigned download URL: " + key, e);
        }
    }

    @Override
    public byte[] getObjectHeader(String key, int bytes) {
        ObjectLocation location = location(key);
        try {
            com.aliyun.oss.model.GetObjectRequest request = new com.aliyun.oss.model.GetObjectRequest(
                    location.bucket(), location.objectKey());
            request.setRange(0, bytes - 1);
            OSSObject object = ossClient.getObject(request);
            try (InputStream is = object.getObjectContent()) {
                return is.readNBytes(bytes);
            }
        } catch (OSSException e) {
            if ("NoSuchKey".equals(e.getErrorCode()) || "NoSuchBucket".equals(e.getErrorCode())) {
                throw new BizException(HttpStatus.NOT_FOUND, "File not found: " + key);
            }
            throw new RuntimeException("Failed to get object header from Aliyun OSS: " + key, e);
        } catch (ClientException e) {
            throw new RuntimeException("Failed to get object header: " + key, e);
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to read object header: " + key, e);
        }
    }

    @Override
    public StorageObjectMetadata headObject(String key) {
        ObjectLocation location = location(key);
        try {
            com.aliyun.oss.model.ObjectMetadata metadata = ossClient
                    .headObject(location.bucket(), location.objectKey());
            return new StorageObjectMetadata(metadata.getETag(), metadata.getContentType(),
                    metadata.getContentLength());
        } catch (OSSException e) {
            if ("NoSuchKey".equals(e.getErrorCode()) || "NoSuchBucket".equals(e.getErrorCode())) {
                log.warn("File not found in Aliyun OSS: {}/{}", location.bucket(), location.objectKey());
                throw new BizException(HttpStatus.NOT_FOUND, "File not found: " + key);
            }
            log.error("Aliyun OSS error while getting metadata: {}/{}", location.bucket(), location.objectKey(), e);
            throw new RuntimeException("Failed to get object metadata from Aliyun OSS: " + key, e);
        } catch (ClientException e) {
            log.error(
                    "Error getting object metadata from Aliyun OSS: {}/{}",
                    location.bucket(),
                    location.objectKey(),
                    e);
            throw new RuntimeException("Failed to get object metadata: " + key, e);
        }
    }

    private Resource resource(String filename, byte[] data) {
        return new ByteArrayResource(data) {
            @Override
            public @NonNull String getFilename() {
                return filename;
            }
        };
    }
}
