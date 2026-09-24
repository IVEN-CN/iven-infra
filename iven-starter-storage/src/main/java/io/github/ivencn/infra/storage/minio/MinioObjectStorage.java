package io.github.ivencn.infra.storage.minio;

import io.github.ivencn.infra.storage.StorageProperties;
import io.github.ivencn.infra.storage.api.ObjectLocation;
import io.github.ivencn.infra.storage.api.ObjectStorage;
import io.github.ivencn.infra.storage.api.StorageObjectMetadata;
import io.github.ivencn.infra.core.exception.BizException;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * MinIO 对象存储适配器，负责将统一的 {@link ObjectStorage} 操作转换为 MinIO SDK 调用。
 */
@Slf4j
public class MinioObjectStorage implements ObjectStorage {

    private final MinioClient minioClient;
    private final MinioClient publicMinioClient;
    private final StorageProperties storageProperties;

    public MinioObjectStorage(MinioClient minioClient, StorageProperties storageProperties) {
        this.minioClient = minioClient;
        this.storageProperties = storageProperties;
        this.publicMinioClient = createPublicClient(storageProperties);
    }

    private static MinioClient createPublicClient(StorageProperties storageProperties) {
        String publicUrl = storageProperties.getMinio().getPublicUrl();
        if (StringUtils.hasText(publicUrl)) {
            try {
                MinioClient client = MinioClient.builder()
                        .endpoint(publicUrl)
                        .credentials(
                                storageProperties.getMinio().getAccessKey(),
                                storageProperties.getMinio().getSecretKey())
                        .build();
                log.info("MinIO public client initialized: {}", publicUrl);
                return client;
            } catch (Exception e) {
                log.error("Failed to initialize MinIO public client with URL: {}", publicUrl, e);
            }
        }
        return null;
    }

    private ObjectLocation location(String key) {
        return new ObjectLocation(storageProperties.getBucket(), key);
    }

    @Override
    public String providerName() {
        return "minio";
    }

    @Override
    public void ensureBucket() {
        String bucket = storageProperties.getBucket();
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("Created MinIO bucket: {}", bucket);
            } else {
                log.debug("MinIO bucket already exists: {}", bucket);
            }
        } catch (Exception e) {
            log.error("Failed to initialize MinIO bucket: {}", bucket, e);
            throw new RuntimeException("Failed to initialize MinIO bucket: " + bucket, e);
        }
    }

    @Override
    public void put(String key, InputStream inputStream) {
        if (inputStream == null) {
            throw new IllegalArgumentException("InputStream cannot be null");
        }
        ObjectLocation location = location(key);
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(location.bucket())
                            .object(location.objectKey())
                            .stream(inputStream, -1, 10485760)
                            .build());
            log.debug("File saved successfully: {}/{}", location.bucket(), location.objectKey());
        } catch (Exception e) {
            log.error("Error saving file to MinIO: {}/{}", location.bucket(), location.objectKey(), e);
            throw new RuntimeException("Failed to save file: " + key, e);
        }
    }

    @Override
    public Resource get(String key) {
        ObjectLocation location = location(key);
        try (InputStream inputStream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(location.bucket())
                        .object(location.objectKey())
                        .build())) {
            byte[] data = inputStream.readAllBytes();
            log.debug("File loaded successfully: {}/{}", location.bucket(), location.objectKey());
            return resource(key, data);
        } catch (ErrorResponseException e) {
            if (e.response().code() == 404) {
                log.warn("File not found in MinIO: {}/{}", location.bucket(), location.objectKey());
                throw new BizException(HttpStatus.NOT_FOUND, "File not found: " + key);
            }
            log.error("MinIO error response while loading file: {}/{}", location.bucket(), location.objectKey(), e);
            throw new RuntimeException("Failed to load file from MinIO: " + key, e);
        } catch (Exception e) {
            log.error("Error loading file from MinIO: {}/{}", location.bucket(), location.objectKey(), e);
            throw new RuntimeException("Failed to load file: " + key, e);
        }
    }

    @Override
    public void delete(String key) {
        ObjectLocation location = location(key);
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(location.bucket())
                            .object(location.objectKey())
                            .build());
            log.debug("File deleted successfully from MinIO: {}/{}", location.bucket(), location.objectKey());
        } catch (Exception e) {
            log.error("Error deleting file from MinIO: {}/{}", location.bucket(), location.objectKey(), e);
            throw new RuntimeException("Failed to delete file from MinIO: " + key, e);
        }
    }

    @Override
    public void checkHealth() {
        try {
            minioClient.bucketExists(BucketExistsArgs.builder().bucket(storageProperties.getBucket()).build());
        } catch (Exception e) {
            throw new RuntimeException("MinIO health check failed", e);
        }
    }

    private MinioClient presignedClient() {
        return publicMinioClient != null ? publicMinioClient : minioClient;
    }

    @Override
    public String getPresignedUploadUrl(String key, String contentType, Duration expiry) {
        ObjectLocation location = location(key);
        try {
            Map<String, String> extraHeaders = new HashMap<>();
            if (contentType != null && !contentType.isBlank()) {
                extraHeaders.put("Content-Type", contentType);
            }
            return presignedClient().getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(location.bucket())
                            .object(location.objectKey())
                            .expiry((int) expiry.getSeconds())
                            .extraHeaders(extraHeaders)
                            .build());
        } catch (Exception e) {
            log.error(
                    "Error generating presigned upload URL from MinIO: {}/{}",
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
            return presignedClient().getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(location.bucket())
                            .object(location.objectKey())
                            .expiry((int) expiry.getSeconds())
                            .build());
        } catch (Exception e) {
            log.error(
                    "Error generating presigned download URL from MinIO: {}/{}",
                    location.bucket(),
                    location.objectKey(),
                    e);
            throw new RuntimeException("Failed to generate presigned download URL: " + key, e);
        }
    }

    @Override
    public byte[] getObjectHeader(String key, int bytes) {
        ObjectLocation location = location(key);
        try (InputStream is = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(location.bucket())
                        .object(location.objectKey())
                        .offset(0L)
                        .length((long) bytes)
                        .build())) {
            return is.readNBytes(bytes);
        } catch (ErrorResponseException e) {
            if (e.response().code() == 404) {
                throw new BizException(HttpStatus.NOT_FOUND, "File not found: " + key);
            }
            throw new RuntimeException("Failed to get object header from MinIO: " + key, e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get object header: " + key, e);
        }
    }

    @Override
    public StorageObjectMetadata headObject(String key) {
        ObjectLocation location = location(key);
        try {
            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(location.bucket())
                            .object(location.objectKey())
                            .build());
            return new StorageObjectMetadata(stat.etag(), stat.contentType(), stat.size());
        } catch (ErrorResponseException e) {
            if (e.response().code() == 404) {
                log.warn("File not found in MinIO: {}/{}", location.bucket(), location.objectKey());
                throw new BizException(HttpStatus.NOT_FOUND, "File not found: " + key);
            }
            log.error("MinIO error response while getting metadata: {}/{}", location.bucket(), location.objectKey(), e);
            throw new RuntimeException("Failed to get object metadata from MinIO: " + key, e);
        } catch (Exception e) {
            log.error("Error getting object metadata from MinIO: {}/{}", location.bucket(), location.objectKey(), e);
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
