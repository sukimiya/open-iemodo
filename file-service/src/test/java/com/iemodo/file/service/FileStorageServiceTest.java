package com.iemodo.file.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FileStorageService")
class FileStorageServiceTest {

    @Test
    @DisplayName("prefix constants should be correct")
    void prefixConstants_shouldBeCorrect() {
        assertThat(FileStorageService.PREFIX_PRODUCTS).isEqualTo("products");
        assertThat(FileStorageService.PREFIX_BRANDS).isEqualTo("products/brands");
        assertThat(FileStorageService.PREFIX_CATEGORIES).isEqualTo("categories");
        assertThat(FileStorageService.PREFIX_AVATARS).isEqualTo("customers/avatars");
        assertThat(FileStorageService.PREFIX_SNS).isEqualTo("customers/sns");
        assertThat(FileStorageService.PREFIX_FEEDBACK).isEqualTo("feedback/images");
        assertThat(FileStorageService.PREFIX_BANNERS).isEqualTo("banners");
        assertThat(FileStorageService.PREFIX_SYSTEM).isEqualTo("system");
        assertThat(FileStorageService.PREFIX_TEMP).isEqualTo("temp");
    }

    @Test
    @DisplayName("buildObjectKey should include prefix and extension")
    void buildObjectKey_shouldIncludePrefixAndExtension() {
        String key = FileStorageService.buildObjectKey("products", "hello.jpg");
        assertThat(key).startsWith("products/");
        assertThat(key).endsWith(".jpg");
        assertThat(key.length()).isGreaterThan("products/".length() + ".jpg".length());
    }

    @Test
    @DisplayName("buildObjectKey should handle filename without extension")
    void buildObjectKey_shouldHandleNoExtension() {
        String key = FileStorageService.buildObjectKey("temp", "file");
        assertThat(key).startsWith("temp/");
        assertThat(key).doesNotContain(".");
    }
}
