package com.teamcanvas.common.exception;

/** Maps to HTTP 401 (bad credentials, missing/expired/revoked token). */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
