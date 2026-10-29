package com.teamcanvas.common.exception;

/** Maps to HTTP 400: the request is well-formed but breaks a business rule. */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
