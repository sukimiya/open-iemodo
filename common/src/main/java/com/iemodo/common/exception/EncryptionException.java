package com.iemodo.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when encryption/decryption operations fail.
 */
public class EncryptionException extends BusinessException {

    public EncryptionException(String message) {
        super(ErrorCode.ENCRYPTION_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, message);
    }

    public EncryptionException(String message, Throwable cause) {
        super(ErrorCode.ENCRYPTION_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }
}
