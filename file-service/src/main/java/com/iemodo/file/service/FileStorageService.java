package com.iemodo.file.service;

import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Service for file storage operations using MinIO.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final MinioClient minioClient;

    @Value("${minio.url-expiry:3600}")
    private int urlExpirySeconds;

    // Predefined buckets
    public static final String BUCKET_PRODUCT_IMAGES = "product-images";
    public static final String BUCKET_USER_AVATARS = "user-avatars";
    public static final String BUCKET_DOCUMENTS = "documents";

    /**
     * Initialize buckets on startup.
     */
    public Mono<Void> initializeBuckets() {
        return Mono.fromCallable(() -> {
            createBucketIfNotExists(BUCKET_PRODUCT_IMAGES);
            createBucketIfNotExists(BUCKET_USER_AVATARS);
            createBucketIfNotExists(BUCKET_DOCUMENTS);
            return null;
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    private void createBucketIfNotExists(String bucketName) 
            throws ServerException, InsufficientDataException, 
                   ErrorResponseException, IOException, 
                   NoSuchAlgorithmException, InvalidKeyException, 
                   InvalidResponseException, XmlParserException, 
                   InternalException {
        boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(bucketName).build());
        if (!exists) {
            minioClient.makeBucket(
                    MakeBucketArgs.builder().bucket(bucketName).build());
            log.info("Created bucket: {}", bucketName);
        }
    }

    /**
     * Upload a file to the specified bucket.
     *
     * @param bucketName target bucket
     * @param file       the file to upload
     * @param objectKey  optional custom object key; if null, auto-generated UUID
     * @return the object key of the stored file
     */
    public Mono<String> uploadFile(String bucketName, FilePart file, String objectKey) {
        String key = objectKey != null ? objectKey : generateObjectKey(file.filename());
        
        return Mono.fromCallable(() -> {
            // Convert FilePart to InputStream and upload
            // Note: This is a simplified implementation
            // In production, you might want to use a streaming approach
            
            byte[] bytes = file.content()
                    .reduce(new byte[0], (acc, buffer) -> {
                        int readable = buffer.readableByteCount();
                        byte[] newAcc = new byte[acc.length + readable];
                        System.arraycopy(acc, 0, newAcc, 0, acc.length);
                        buffer.read(newAcc, acc.length, readable);
                        return newAcc;
                    })
                    .block();

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(key)
                            .stream(new java.io.ByteArrayInputStream(bytes), bytes.length, -1)
                            .contentType(file.headers().getContentType() != null 
                                    ? file.headers().getContentType().toString() 
                                    : "application/octet-stream")
                            .build());

            log.info("Uploaded file to {}/{} ({} bytes)", bucketName, key, bytes.length);
            return key;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Get a pre-signed URL for downloading a file.
     *
     * @param bucketName the bucket containing the file
     * @param objectKey  the object key
     * @return pre-signed URL valid for configured expiry time
     */
    public Mono<String> getPresignedUrl(String bucketName, String objectKey) {
        return Mono.fromCallable(() -> {
            String url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(objectKey)
                            .expiry(urlExpirySeconds, TimeUnit.SECONDS)
                            .build());
            log.debug("Generated presigned URL for {}/{}", bucketName, objectKey);
            return url;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Delete a file from storage.
     *
     * @param bucketName the bucket containing the file
     * @param objectKey  the object key
     */
    public Mono<Void> deleteFile(String bucketName, String objectKey) {
        return Mono.fromCallable(() -> {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .build());
            log.info("Deleted file from {}/{}", bucketName, objectKey);
            return null;
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    /**
     * Check if a file exists.
     */
    public Mono<Boolean> fileExists(String bucketName, String objectKey) {
        return Mono.fromCallable(() -> {
            try {
                minioClient.statObject(
                        StatObjectArgs.builder()
                                .bucket(bucketName)
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

    private String generateObjectKey(String originalFilename) {
        String extension = "";
        int lastDot = originalFilename.lastIndexOf('.');
        if (lastDot > 0) {
            extension = originalFilename.substring(lastDot);
        }
        return UUID.randomUUID().toString().replace("-", "") + extension;
    }
}
