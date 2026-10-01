package com.whitejack.server.exception;

import org.springframework.http.HttpStatus;

/** A request the server will not carry out; answered with {@code status} and an {@code ErrorResponse}. */
public class InvalidRequestException extends RuntimeException {

    private final String code;
    private final HttpStatus status;

    /** A client mistake: 400. */
    public InvalidRequestException(String code, String detail) {
        this(HttpStatus.BAD_REQUEST, code, detail);
    }

    public InvalidRequestException(HttpStatus status, String code, String detail) {
        super(detail);
        this.code = code;
        this.status = status;
    }

    public String code() {
        return code;
    }

    public HttpStatus status() {
        return status;
    }
}
