package com.iemodo.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when map/geocoding service operations fail.
 */
public class MapServiceException extends BusinessException {

    public MapServiceException(String message) {
        super(ErrorCode.MAP_SERVICE_ERROR, HttpStatus.SERVICE_UNAVAILABLE, message);
    }

    public MapServiceException(String message, Throwable cause) {
        super(ErrorCode.MAP_SERVICE_ERROR, HttpStatus.SERVICE_UNAVAILABLE, cause);
    }
}
