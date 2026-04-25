package com.iemodo.file.service;

import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final MinioClient minioClient;

    @Value("${iemodo.minio.bucket:iemodo}")
    private String bucket;

    @Value("${iemodo.minio.url-expiry:3600}")
    private int urlExpirySeconds;

    // ─── Storage directory prefixes ─────────────────────────────────────
    public static final String PREFIX_PRODUCTS       = "products";
    public static final String PREFIX_BRANDS         = "products/brands";
    public static final String PREFIX_CATEGORIES     = "categories";
    public static final String PREFIX_AVATARS        = "customers/avatars";
    public static final String PREFIX_SNS            = "customers/sns";
    public static final String PREFIX_FEEDBACK       = "feedback/images";
    public static final String PREFIX_BANNERS        = "banners";
    public static final String PREFIX_SYSTEM         = "system";
    public static final String PREFIX_TEMP           = "temp";

    /**
     * Generate a presigned PUT URL for direct browser-to-MinIO upload.
     *
     * @param objectKey the full object key including prefix, e.g. "products/abc123.jpg"
     * @return presigned PUT URL valid for 15 minutes
     */
    public Mono<String> generatePresignedUploadUrl(String objectKey) {
        return Mono.fromCallable(() -> {
            String url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(bucket)
                            .object(objectKey)
                            .expiry(15, TimeUnit.MINUTES)
                            .build());
            log.debug("Presigned upload URL for {}/{}", bucket, objectKey);
            return url;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Generate a presigned GET URL for downloading a file.
     *
     * @param objectKey the full object key including prefix, e.g. "products/abc123.jpg"
     * @return presigned GET URL valid for the configured expiry time
     */
    public Mono<String> getPresignedUrl(String objectKey) {
        return Mono.fromCallable(() -> {
            String url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucket)
                            .object(objectKey)
                            .expiry(urlExpirySeconds, TimeUnit.SECONDS)
                            .build());
            log.debug("Presigned GET URL for {}/{}", bucket, objectKey);
            return url;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Delete a file from storage.
     */
    public Mono<Void> deleteFile(String objectKey) {
        return Mono.fromCallable(() -> {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .build());
            log.info("Deleted {}/{}", bucket, objectKey);
            return null;
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    /**
     * Check if a file exists.
     */
    public Mono<Boolean> fileExists(String objectKey) {
        return Mono.fromCallable(() -> {
            try {
                minioClient.statObject(
                        StatObjectArgs.builder()
                                .bucket(bucket)
                                .object(objectKey)
                                .build());
                return true;
            } catch (ErrorResponseException e) {
                if (e.errorResponse().code().equals("NoSuchKey")) {
                    return false;
                }
                throw e;
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Build a unique object key from prefix and original filename.
     * <p>Result: {@code <prefix>/<uuid><ext>}, e.g. {@code products/a1b2c3d4.jpg}
     */
    public static String buildObjectKey(String prefix, String originalFilename) {
        String extension = "";
        int lastDot = originalFilename.lastIndexOf('.');
        if (lastDot > 0) {
            extension = originalFilename.substring(lastDot);
        }
        return prefix + "/" + UUID.randomUUID().toString().replace("-", "") + extension;
    }
}
