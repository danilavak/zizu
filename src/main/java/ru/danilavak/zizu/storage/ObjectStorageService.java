package ru.danilavak.zizu.storage;

import java.net.URI;

public interface ObjectStorageService {
    void putPrivateObject(String objectKey, byte[] content, String contentType);

    void deleteObject(String objectKey);

    URI createPresignedGetUrl(String objectKey);

    String bucketName();
}
