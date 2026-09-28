package com.whitejack.server.dto;

/** HTTP error body. {@code error} is a stable code for clients; {@code detail} is for humans. */
public record ErrorResponse(String error, String detail) {}
