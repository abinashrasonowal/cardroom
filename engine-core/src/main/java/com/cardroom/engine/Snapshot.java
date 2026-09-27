package com.cardroom.engine;

import com.cardroom.contract.PlayerView;

/**
 * What a reconnecting client gets: the view, and the sequence number it already reflects.
 *
 * <p>No event tail. The snapshot already includes every event up to {@code seq}, so sending a
 * tail as well makes the client double-apply unless it knows where the snapshot stopped.
 */
public record Snapshot(long seq, PlayerView view) {}
