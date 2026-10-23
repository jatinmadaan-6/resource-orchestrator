package com.enterprise.orchestrator.exception;

/** The user has the right role, but is not allowed to act on THIS record. Maps to HTTP 403. */
public class ForbiddenOperationException extends RuntimeException {
    public ForbiddenOperationException(String message) { super(message); }
}
