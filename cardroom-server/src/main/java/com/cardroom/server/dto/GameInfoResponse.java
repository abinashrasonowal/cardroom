package com.cardroom.server.dto;

/** One entry of {@code GET /api/games}. */
public record GameInfoResponse(String id, int minPlayers, int maxPlayers) {}
