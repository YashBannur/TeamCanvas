package com.teamcanvas.common.exception;

/** Maps to HTTP 403: the caller is a member but their role does not allow the action. */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
