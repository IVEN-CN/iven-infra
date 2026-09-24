package io.github.ivencn.infra.storage.api;

/**
 * 对象存储位置。
 *
 * @param bucket
 *            存储桶名称
 * @param objectKey
 *            bucket 内的对象路径
 */
public record ObjectLocation(String bucket, String objectKey) {
}
