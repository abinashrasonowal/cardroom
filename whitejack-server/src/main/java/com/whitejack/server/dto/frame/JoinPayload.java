package com.whitejack.server.dto.frame;

/** Payload of a {@code join} frame. Sent on every socket open; a known player is a reconnect. */
public record JoinPayload(String room, String nick, String clientSeed) {}
