package com.iemodo.user.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("HashUtil")
class HashUtilTest {

    @Test
    @DisplayName("should produce consistent SHA-256 hash for same input")
    void shouldProduceConsistentHash() {
        String input = "myRefreshToken123";
        String hash1 = HashUtil.sha256(input);
        String hash2 = HashUtil.sha256(input);

        assertThat(hash1).isEqualTo(hash2);
        assertThat(hash1).hasSize(64); // SHA-256 hex = 64 chars
    }

    @Test
    @DisplayName("should produce different hashes for different inputs")
    void shouldProduceDifferentHashesForDifferentInputs() {
        String hash1 = HashUtil.sha256("token_a");
        String hash2 = HashUtil.sha256("token_b");

        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    @DisplayName("should produce hex-only output")
    void shouldProduceHexOutput() {
        String hash = HashUtil.sha256("test");
        assertThat(hash).matches("[0-9a-f]{64}");
    }
}
