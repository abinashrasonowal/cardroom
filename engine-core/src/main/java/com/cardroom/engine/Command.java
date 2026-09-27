package com.cardroom.engine;

import com.cardroom.contract.PlayerId;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Everything that can reach a room. Only {@link Submit} is ever handed to a game module, which
 * is what keeps phase transitions and host powers out of game code — a blackjack module has no
 * opinion about who may close the room.
 *
 * <p>There is no {@code Reconnect}: the gateway sends {@code join} on every socket open, and a
 * {@link Join} whose {@code PlayerId} is already seated <em>is</em> a reconnect. A second
 * command would be a second code path that can disagree with the first about seat ownership.
 *
 * <p>{@code Kick} and {@code TransferHost} are not here yet. Sealing means adding one later is
 * a compile error at the dispatch, which is the point.
 */
public sealed interface Command {

    /** Also the reconnect path: a known player simply gains another socket. */
    record Join(PlayerId player, SocketId socket, String nick, String clientSeed) implements Command {}

    /** Quitting. Distinct from {@link Disconnect} — dropping is not quitting. */
    record Leave(PlayerId player) implements Command {}

    /** One socket closed. The seat is held; other sockets for the same player keep it live. */
    record Disconnect(PlayerId player, SocketId socket) implements Command {}

    record Submit(PlayerId actor, JsonNode intent, String clientMsgId) implements Command {}

    /**
     * @param armedAtSeq the sequence number the clock was armed at — the fencing token that
     *     stops a timer firing at T−1ms from auto-playing the <em>next</em> player's turn
     */
    record Timeout(long armedAtSeq, PlayerId actor) implements Command {}

    record Start(PlayerId requester) implements Command {}

    record Close(PlayerId requester) implements Command {}
}
