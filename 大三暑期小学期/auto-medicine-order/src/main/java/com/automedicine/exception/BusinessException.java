package com.automedicine.exception;

import lombok.Getter;

/**
 * 自定义业务异常
 * <p>
 * 用于在业务逻辑中主动抛出的可预期异常，
 * 由 GlobalExceptionHandler 统一捕获并返回标准 ApiResponse 格式。
 * </p>
 */
@Getter
public class BusinessException extends RuntimeException {

    /** 业务错误码 */
    private final Integer code;

    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    public BusinessException(String message, Integer code) {
        super(message);
        this.code = code;
    }
}
