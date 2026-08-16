package com.bbu.vyaparbackend.shared;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ApiException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public static ApiException notFound(String resource) {
        return new ApiException(HttpStatus.NOT_FOUND, ErrorMessages.Codes.NOT_FOUND, resource + " was not found");
    }

    public static ApiException forbidden() {
        return new ApiException(HttpStatus.FORBIDDEN, ErrorMessages.Codes.FORBIDDEN,
                ErrorMessages.Messages.FORBIDDEN);
    }

    public static ApiException conflict(String message) {
        return new ApiException(HttpStatus.CONFLICT, ErrorMessages.Codes.CONFLICT, message);
    }

    public static ApiException invalid(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, ErrorMessages.Codes.INVALID_REQUEST, message);
    }
}
