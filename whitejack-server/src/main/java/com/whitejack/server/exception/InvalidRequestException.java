package com.whitejack.server.exception;

/** A client mistake on the HTTP surface; answered with 400 and an {@code ErrorResponse}. */
public class InvalidRequestException extends RuntimeException {

    private final String code;

    public InvalidRequestException(String code, String detail) {
        super(detail);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
