package com.iemodo.file.service;

import io.minio.MinioClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FileStorageService")
class FileStorageServiceTest {

    @Mock private MinioClient minioClient;

    private FileStorageService fileStorageService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        fileStorageService = new FileStorageService(minioClient);
    }

    @Test
    @DisplayName("constants: should have correct bucket names")
    void constants_shouldBeCorrect() {
        assertThat(FileStorageService.BUCKET_PRODUCT_IMAGES).isEqualTo("product-images");
        assertThat(FileStorageService.BUCKET_USER_AVATARS).isEqualTo("user-avatars");
        assertThat(FileStorageService.BUCKET_DOCUMENTS).isEqualTo("documents");
    }
}
