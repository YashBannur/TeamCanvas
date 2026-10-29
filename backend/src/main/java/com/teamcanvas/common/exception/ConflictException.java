package com.teamcanvas.common.exception;

/** Maps to HTTP 409 (for example, a duplicate e-mail address). */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
