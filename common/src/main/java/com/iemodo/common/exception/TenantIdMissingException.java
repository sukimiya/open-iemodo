package com.iemodo.common.exception;

import org.springframework.http.HttpStatus;

public class TenantIdMissingException extends BusinessException {

    public TenantIdMissingException() {
        super(ErrorCode.TENANT_ID_MISSING, HttpStatus.BAD_REQUEST);
    }
}
