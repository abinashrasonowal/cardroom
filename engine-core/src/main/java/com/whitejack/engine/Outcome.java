package com.whitejack.engine;

import com.whitejack.contract.Validation;

/**
 * What the room actor needs to know after an intent: whether to broadcast, and if not, what to
 * tell the one caller. A rejection keeps its reason so the gateway can answer the {@code id}
 * that caused it.
 *
 * @param seq the sequence number after processing — unchanged when the intent was rejected
 */
public record Outcome(long seq, Validation result) {

    public boolean accepted() {
        return result.isOk();
    }
}
