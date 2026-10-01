package com.whitejack.bots;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Objects;

/**
 * One legal option, read off the view. {@code label} is what the model sees; {@code value} is
 * the exact JSON the bot sends (a whole intent, or one card of a multi-card pick).
 */
public record Move(String label, JsonNode value) {
    public Move {
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(value, "value");
    }
}
