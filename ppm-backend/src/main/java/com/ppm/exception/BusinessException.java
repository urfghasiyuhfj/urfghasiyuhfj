package com.ppm.exception;

/**
 * 业务异常基类
 */
public class BusinessException extends RuntimeException {

    private int code = 400;

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }

    public int getCode() {
        return code;
    }
}
