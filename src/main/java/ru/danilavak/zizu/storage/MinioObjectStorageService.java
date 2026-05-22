package ru.danilavak.zizu.storage;

import java.io.ByteArrayInputStream;
import java.net.URI;

import org.springframework.stereotype.Service;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;

@Service
public class MinioObjectStorageService implements ObjectStorageService {
    private final MinioClient minioClient;
    private final ObjectStorageProperties properties;

    public MinioObjectStorageService(MinioClient minioClient, ObjectStorageProperties properties) {
        this.minioClient = minioClient;
        this.properties = properties;
    }

    @Override
    public void putPrivateObject(String objectKey, byte[] content, String contentType) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName())
                            .object(objectKey)
                            .stream(new ByteArrayInputStream(content), content.length, -1)
                            .contentType(contentType)
                            .build()
            );
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to upload object to storage", exception);
        }
    }

    @Override
    public void deleteObject(String objectKey) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName())
                            .object(objectKey)
                            .build()
            );
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to delete object from storage", exception);
        }
    }

    @Override
    public URI createPresignedGetUrl(String objectKey) {
        try {
            String url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName())
                            .object(objectKey)
                            .expiry(properties.getPresignedExpiryMinutes() * 60)
                            .build()
            );
            return URI.create(url);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to create presigned URL", exception);
        }
    }

    @Override
    public String bucketName() {
        return properties.getBucket();
    }
}
