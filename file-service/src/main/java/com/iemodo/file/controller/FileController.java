package com.iemodo.file.controller;

import com.iemodo.common.exception.ErrorCode;
import com.iemodo.common.response.Response;
import com.iemodo.file.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    /**
     * Request a presigned upload URL for direct browser-to-MinIO upload.
     * <p>The client uploads directly to MinIO using the returned URL,
     * then saves the returned objectKey in the entity (e.g. brand.logoUrl).
     */
    @PostMapping("/presigned-upload")
    public Mono<Response<Map<String, String>>> requestPresignedUpload(@RequestBody Map<String, String> request) {
        String prefix = request.get("prefix");
        String fileName = request.get("fileName");

        if (prefix == null || fileName == null) {
            return Mono.just(Response.error(ErrorCode.BAD_REQUEST, "prefix and fileName are required"));
        }

        String objectKey = FileStorageService.buildObjectKey(prefix, fileName);

        return fileStorageService.generatePresignedUploadUrl(objectKey)
                .map(url -> Response.success(Map.of(
                        "presignedUrl", url,
                        "objectKey", objectKey
                )));
    }

    /**
     * Get a presigned download URL for a file.
     */
    @GetMapping("/presigned-url")
    public Mono<Response<Map<String, String>>> getPresignedUrl(@RequestParam("objectKey") String objectKey) {
        return fileStorageService.getPresignedUrl(objectKey)
                .map(url -> Response.success(Map.of(
                        "presignedUrl", url,
                        "objectKey", objectKey
                )));
    }

    /**
     * Quick health check for the file controller.
     */
    @GetMapping("/ping")
    public Mono<Response<String>> ping() {
        return Mono.just(Response.success("pong"));
    }

    /**
     * Delete a file from storage.
     */
    @DeleteMapping
    public Mono<Response<Void>> deleteFile(@RequestParam("objectKey") String objectKey) {
        return fileStorageService.deleteFile(objectKey)
                .then(Mono.just(Response.success()));
    }
}
