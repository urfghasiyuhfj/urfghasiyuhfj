package com.ppm.exception;

/**
 * 资源不存在异常
 */
public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String resource, Object id) {
        super(404, resource + " 不存在: " + id);
    }

    public ResourceNotFoundException(String message) {
        super(404, message);
    }
}
