package com.whitejack.server.dto.frame;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Every client → server frame (§7). {@code payload} stays raw: for an intent only the game's
 * {@code parseIntent} may read it, so the gateway never learns a card rule.
 */
public record ClientFrame(int v, String id, String type, JsonNode payload) {

    public static final int PROTOCOL = 1;
}
