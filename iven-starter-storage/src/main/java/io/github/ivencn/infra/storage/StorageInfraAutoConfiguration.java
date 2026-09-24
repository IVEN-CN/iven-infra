package io.github.ivencn.infra.storage;

import io.github.ivencn.infra.storage.api.ObjectStorage;
import io.github.ivencn.infra.storage.aliyun.AliyunOssObjectStorage;
import io.github.ivencn.infra.storage.magic.FileMagicChecker;
import io.github.ivencn.infra.storage.minio.MinioObjectStorage;
import io.github.ivencn.infra.core.banner.InfraBannerPrinter;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import io.minio.MinioClient;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * iven-starter-storage 自动装配。
 *
 * <p>
 * 开关：iven.storage.enabled（默认 true），提供方：iven.storage.provider（minio | aliyun-oss）。
 * 启动时通过当前启用的 {@link ObjectStorage} 初始化 bucket。
 * </p>
 */
@Slf4j
@AutoConfiguration
@ConditionalOnProperty(name = "iven.storage.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(StorageProperties.class)
public class StorageInfraAutoConfiguration {

    @PostConstruct
    void printBanner() {
        InfraBannerPrinter.printOnce();
    }

    @Bean
    @ConditionalOnProperty(name = "iven.storage.provider", havingValue = "minio", matchIfMissing = true)
    public MinioClient minioClient(StorageProperties storageProperties) {
        storageProperties.validateMinio();
        StorageProperties.Minio minioProperties = storageProperties.getMinio();
        String endpoint = Boolean.TRUE.equals(minioProperties.getUseSSL())
                ? "https://" + minioProperties.getEndpoint()
                : "http://" + minioProperties.getEndpoint();

        MinioClient client = MinioClient.builder()
                .endpoint(endpoint, minioProperties.getPort(), minioProperties.getUseSSL())
                .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                .build();

        log.info("MinIO client initialized: {}:{}", minioProperties.getEndpoint(), minioProperties.getPort());
        return client;
    }

    @Bean
    @ConditionalOnProperty(name = "iven.storage.provider", havingValue = "minio", matchIfMissing = true)
    @ConditionalOnMissingBean(ObjectStorage.class)
    public MinioObjectStorage minioObjectStorage(MinioClient minioClient, StorageProperties storageProperties) {
        log.info("Creating MinioObjectStorage bean");
        return new MinioObjectStorage(minioClient, storageProperties);
    }

    @Bean
    @ConditionalOnProperty(name = "iven.storage.provider", havingValue = "aliyun-oss")
    public OSS ossClient(StorageProperties storageProperties) {
        storageProperties.validateAliyunOss();
        StorageProperties.AliyunOss aliyunOss = storageProperties.getAliyunOss();
        OSS client = new OSSClientBuilder().build(
                aliyunOss.getEndpoint(),
                aliyunOss.getAccessKeyId(),
                aliyunOss.getAccessKeySecret());
        log.info("Aliyun OSS client initialized: {}", aliyunOss.getEndpoint());
        return client;
    }

    @Bean
    @ConditionalOnProperty(name = "iven.storage.provider", havingValue = "aliyun-oss")
    @ConditionalOnMissingBean(ObjectStorage.class)
    public AliyunOssObjectStorage aliyunOssObjectStorage(OSS ossClient, StorageProperties storageProperties) {
        log.info("Creating AliyunOssObjectStorage bean");
        return new AliyunOssObjectStorage(ossClient, storageProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    public FileMagicChecker fileMagicChecker() {
        return new FileMagicChecker();
    }

    /**
     * 应用启动时初始化对象存储 bucket，避免业务首次写入时才发现 bucket 不存在。
     * 通过方法参数隐式要求 ObjectStorage bean 存在，storage 关闭时自动退避。
     */
    @Bean
    public CommandLineRunner initializeObjectStorage(ObjectStorage objectStorage) {
        return args -> {
            log.info("Initializing object storage, provider={}", objectStorage.providerName());
            objectStorage.ensureBucket();
        };
    }
}
