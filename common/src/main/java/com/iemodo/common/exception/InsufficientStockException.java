package com.iemodo.common.exception;

import org.springframework.http.HttpStatus;

public class InsufficientStockException extends BusinessException {

    public InsufficientStockException(String sku) {
        super(ErrorCode.INSUFFICIENT_STOCK, HttpStatus.CONFLICT,
                "Insufficient stock for SKU: " + sku);
    }
}
