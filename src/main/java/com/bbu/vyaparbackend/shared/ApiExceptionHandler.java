package com.bbu.vyaparbackend.shared;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ApiException.class)
    ProblemDetail handleApi(ApiException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());
        detail.setType(URI.create("https://api.vyapar.local/problems/" + ex.getCode()));
        detail.setTitle(ex.getCode());
        return withRequestId(detail);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                ErrorMessages.Messages.VALIDATION_FAILED);
        detail.setTitle(ErrorMessages.Codes.VALIDATION_FAILED);
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(error -> error.getField(),
                        error -> error.getDefaultMessage() == null ? "invalid" : error.getDefaultMessage(),
                        (first, ignored) -> first));
        detail.setProperty(JsonKeys.ERRORS, errors);
        return withRequestId(detail);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handleConstraint() {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                ErrorMessages.Messages.DATA_CONFLICT);
        detail.setTitle(ErrorMessages.Codes.DATA_CONFLICT);
        return withRequestId(detail);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ProblemDetail handleConcurrentUpdate() {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                ErrorMessages.Messages.CONCURRENT_UPDATE);
        detail.setTitle(ErrorMessages.Codes.CONCURRENT_UPDATE);
        return withRequestId(detail);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MaxUploadSizeExceededException.class})
    ProblemDetail handleMalformed() {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                ErrorMessages.Messages.MALFORMED_REQUEST);
        detail.setTitle(ErrorMessages.Codes.INVALID_REQUEST);
        return withRequestId(detail);
    }

    private ProblemDetail withRequestId(ProblemDetail detail) {
        String requestId = RequestLogContext.requestId();
        if (requestId != null) detail.setProperty(JsonKeys.REQUEST_ID, requestId);
        return detail;
    }
}
