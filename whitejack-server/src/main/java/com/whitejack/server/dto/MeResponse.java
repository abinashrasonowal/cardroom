package com.whitejack.server.dto;

/** {@code GET /api/me}. The token is echoed so the client can mirror it into localStorage (§4). */
public record MeResponse(String playerId, String token) {}
