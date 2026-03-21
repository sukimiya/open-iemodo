package com.iemodo.common.exception;

import org.springframework.http.HttpStatus;

public class TenantNotFoundException extends BusinessException {

    public TenantNotFoundException(String tenantId) {
        super(ErrorCode.TENANT_NOT_FOUND, HttpStatus.BAD_REQUEST,
                "Tenant not found: " + tenantId);
    }
}
