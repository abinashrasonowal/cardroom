package com.cardroom.contract;

import java.util.Objects;

/**
 * A place at the table, as a game module sees it.
 *
 * <p>Connection state is deliberately absent: §3 forbids a game module from touching sockets,
 * and a seat whose equality changed every time a tab opened would make any {@code S} holding
 * a seat list go stale for reasons that have nothing to do with the rules. The room tracks
 * sockets on its own side.
 */
public record Seat(int index, PlayerId id, String nick) {
    public Seat {
        if (index < 0) throw new IllegalArgumentException("negative seat index: " + index);
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(nick, "nick");
    }
}
