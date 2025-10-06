package com.onlinemeetingbookingsystem.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// For authorization failures (user authenticated but lacks permission), typically 403 Forbidden
@ResponseStatus(HttpStatus.FORBIDDEN)
public class ForbiddenException extends BaseRuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}

