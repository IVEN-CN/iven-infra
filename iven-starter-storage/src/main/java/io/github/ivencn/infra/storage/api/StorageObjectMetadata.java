package io.github.ivencn.infra.storage.api;

/**
 * 对象存储中对象的元数据。
 *
 * @param etag
 *            对象的 ETag（通常为 MD5）
 * @param contentType
 *            对象的 Content-Type
 * @param size
 *            对象大小（字节）
 */
public record StorageObjectMetadata(String etag, String contentType, long size) {
}
