package com.enterprise.orchestrator.exception;

/** An illegal state transition or a violated business invariant. Maps to HTTP 409. */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) { super(message); }
}
