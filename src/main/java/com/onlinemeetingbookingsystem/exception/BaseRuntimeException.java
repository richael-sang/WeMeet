package com.onlinemeetingbookingsystem.exception;

// Optional: A base class for your custom runtime exceptions
public class BaseRuntimeException extends RuntimeException {
    public BaseRuntimeException(String message) {
        super(message);
    }

    public BaseRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
