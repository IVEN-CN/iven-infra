package io.github.ivencn.infra.storage.api;

/**
 * 对象存储统一接口。
 *
 * <p>
 * 所有方法以 {@code String key}（bucket 内对象路径）定位对象，与业务无关；
 * key 的业务前缀规则（如文件类型分目录）由应用侧自行组装。具体由 MinIO 或阿里云 OSS 适配器实现。
 * </p>
 */
public interface ObjectStorage {

    /**
     * 当前对象存储提供方名称，用于日志和健康检查输出。
     */
    String providerName();

    /**
     * 初始化并确保配置的 bucket 可用。
     */
    void ensureBucket();

    /**
     * 保存对象。
     *
     * @param key
     *            对象 key
     * @param inputStream
     *            文件输入流
     */
    void put(String key, java.io.InputStream inputStream);

    /**
     * 读取对象。
     *
     * @param key
     *            对象 key
     * @return 文件资源
     */
    org.springframework.core.io.Resource get(String key);

    /**
     * 删除对象。
     *
     * @param key
     *            对象 key
     */
    void delete(String key);

    /**
     * 检查对象存储连接和 bucket 可访问性。
     */
    void checkHealth();

    /**
     * 生成预签名上传 URL。
     *
     * @param key
     *            对象 key
     * @param contentType
     *            文件 Content-Type
     * @param expiry
     *            URL 过期时间
     * @return 预签名 PUT URL
     */
    String getPresignedUploadUrl(String key, String contentType, java.time.Duration expiry);

    /**
     * 生成预签名下载 URL。
     *
     * @param key
     *            对象 key
     * @param expiry
     *            URL 过期时间
     * @return 预签名 GET URL
     */
    String getPresignedDownloadUrl(String key, java.time.Duration expiry);

    /**
     * 获取对象元数据（HEAD 请求）。
     *
     * @param key
     *            对象 key
     * @return 对象元数据
     */
    StorageObjectMetadata headObject(String key);

    /**
     * 读取对象前 N 个字节。
     *
     * @param key
     *            对象 key
     * @param bytes
     *            要读取的字节数
     * @return 文件头字节数组
     */
    byte[] getObjectHeader(String key, int bytes);
}
