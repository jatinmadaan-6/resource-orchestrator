package com.enterprise.orchestrator.web;

import com.enterprise.orchestrator.exception.BusinessRuleException;
import com.enterprise.orchestrator.exception.ForbiddenOperationException;
import com.enterprise.orchestrator.exception.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/** One place that turns exceptions into consistent RFC-7807 JSON error responses. */
@Hidden
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail notFound(ResourceNotFoundException e) { return problem(HttpStatus.NOT_FOUND, e.getMessage()); }

    @ExceptionHandler(BusinessRuleException.class)
    public ProblemDetail conflict(BusinessRuleException e) { return problem(HttpStatus.CONFLICT, e.getMessage()); }

    @ExceptionHandler({ForbiddenOperationException.class, AccessDeniedException.class})
    public ProblemDetail forbidden(RuntimeException e) { return problem(HttpStatus.FORBIDDEN, e.getMessage()); }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ProblemDetail concurrent(ObjectOptimisticLockingFailureException e) {
        return problem(HttpStatus.CONFLICT, "The record was modified by someone else. Please retry.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail invalid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + " " + f.getDefaultMessage()).collect(Collectors.joining("; "));
        return problem(HttpStatus.BAD_REQUEST, msg);
    }

    private ProblemDetail problem(HttpStatus status, String detail) {
        return ProblemDetail.forStatusAndDetail(status, detail);
    }
}
