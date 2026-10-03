package com.trackflow.gateway.auth.web;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;

import com.trackflow.gateway.auth.service.AuthExceptions.InvalidCredentialsException;
import com.trackflow.gateway.auth.service.AuthExceptions.InvalidUserChangeException;
import com.trackflow.gateway.auth.service.AuthExceptions.UserNotFoundException;
import com.trackflow.gateway.auth.service.AuthExceptions.UsernameTakenException;

/** Maps auth failures to RFC 9457 problem responses. */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(InvalidCredentialsException.class)
    ProblemDetail invalidCredentials(InvalidCredentialsException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(UsernameTakenException.class)
    ProblemDetail usernameTaken(UsernameTakenException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(UserNotFoundException.class)
    ProblemDetail notFound(UserNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(InvalidUserChangeException.class)
    ProblemDetail invalidChange(InvalidUserChangeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    /** e.g. "503 Unable to find instance" while a service is (re)registering with Eureka. */
    @ExceptionHandler(HttpStatusCodeException.class)
    ProblemDetail downstream(HttpStatusCodeException ex) {
        return ProblemDetail.forStatusAndDetail(ex.getStatusCode(), ex.getStatusText());
    }

    /** The service is registered but not answering (stopped, restarting): connection refused or timed out. */
    @ExceptionHandler(ResourceAccessException.class)
    ProblemDetail unreachable(ResourceAccessException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "The service is not reachable right now; it may be restarting");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validation(MethodArgumentNotValidException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed");
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> errors.putIfAbsent(e.getField(), e.getDefaultMessage()));
        problem.setProperty("errors", errors);
        return problem;
    }
}
