package com.iemodo.file.controller;

import com.iemodo.common.response.Response;
import com.iemodo.file.dto.FileUploadResponse;
import com.iemodo.file.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * REST controller for file operations.
 * 
 * <p>Base path: /api/v1/files
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    /**
     * Upload a product image.
     */
    @PostMapping(value = "/product-images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<Response<FileUploadResponse>> uploadProductImage(
            @RequestPart("file") Mono<FilePart> filePart) {
        return filePart.flatMap(file -> 
                fileStorageService.uploadFile(
                        FileStorageService.BUCKET_PRODUCT_IMAGES, 
                        file, 
                        generateObjectKey(file.filename()))
                        .map(objectKey -> FileUploadResponse.builder()
                                .bucket(FileStorageService.BUCKET_PRODUCT_IMAGES)
                                .objectKey(objectKey)
                                .originalFilename(file.filename())
                                .contentType(file.headers().getContentType() != null 
                                        ? file.headers().getContentType().toString() 
                                        : "application/octet-stream")
                                .build())
        ).map(Response::success);
    }

    /**
     * Upload a user avatar.
     */
    @PostMapping(value = "/user-avatars", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<Response<FileUploadResponse>> uploadUserAvatar(
            @RequestPart("file") Mono<FilePart> filePart,
            @RequestParam(value = "userId", required = false) String userId) {
        return filePart.flatMap(file -> {
            String prefix = userId != null ? userId + "-" : "";
            String objectKey = prefix + generateObjectKey(file.filename());
            
            return fileStorageService.uploadFile(
                            FileStorageService.BUCKET_USER_AVATARS, 
                            file, 
                            objectKey)
                    .map(key -> FileUploadResponse.builder()
                            .bucket(FileStorageService.BUCKET_USER_AVATARS)
                            .objectKey(key)
                            .originalFilename(file.filename())
                            .contentType(file.headers().getContentType() != null 
                                    ? file.headers().getContentType().toString() 
                                    : "application/octet-stream")
                            .build());
        }).map(Response::success);
    }

    /**
     * Upload a document.
     */
    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<Response<FileUploadResponse>> uploadDocument(
            @RequestPart("file") Mono<FilePart> filePart) {
        return filePart.flatMap(file -> 
                fileStorageService.uploadFile(
                        FileStorageService.BUCKET_DOCUMENTS, 
                        file, 
                        generateObjectKey(file.filename()))
                        .map(objectKey -> FileUploadResponse.builder()
                                .bucket(FileStorageService.BUCKET_DOCUMENTS)
                                .objectKey(objectKey)
                                .originalFilename(file.filename())
                                .contentType(file.headers().getContentType() != null 
                                        ? file.headers().getContentType().toString() 
                                        : "application/octet-stream")
                                .build())
        ).map(Response::success);
    }

    /**
     * Get a pre-signed URL for accessing a file.
     */
    @GetMapping("/{bucket}/presigned-url")
    public Mono<Response<String>> getPresignedUrl(
            @PathVariable String bucket,
            @RequestParam("objectKey") String objectKey) {
        return fileStorageService.getPresignedUrl(bucket, objectKey)
                .map(Response::success);
    }

    /**
     * Delete a file.
     */
    @DeleteMapping("/{bucket}")
    public Mono<Response<Void>> deleteFile(
            @PathVariable String bucket,
            @RequestParam("objectKey") String objectKey) {
        return fileStorageService.deleteFile(bucket, objectKey)
                .then(Mono.just(Response.success()));
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
