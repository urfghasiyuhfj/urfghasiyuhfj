package com.ppm.exception;

/**
 * 资源重复异常
 */
public class DuplicateResourceException extends BusinessException {

    public DuplicateResourceException(String resource, String field, String value) {
        super(409, resource + " 已存在: " + field + " = " + value);
    }

    public DuplicateResourceException(String message) {
        super(409, message);
    }
}
