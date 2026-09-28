package com.whitejack.engine;

import com.whitejack.contract.GameEvent;

/**
 * One event, stamped and numbered.
 *
 * <p>{@code gameVersion} is the rules version that produced it, stamped from day one because
 * retrofitting a version field onto already-persisted events is the one migration nobody can
 * perform. It is deliberately not called {@code v}: §7's wire envelope has a {@code v} too and
 * that one is the <em>protocol</em> version.
 *
 * @param seq monotonic within a room, starting at 0
 * @param at epoch millis from {@code EngineContext.now()}, never {@code System.currentTimeMillis}
 */
public record SequencedEvent<E extends GameEvent>(long seq, long at, int gameVersion, E event) {}
