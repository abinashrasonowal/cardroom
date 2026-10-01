package com.whitejack.bots;

/**
 * Where bots report the moves they send, so a server can show their reasoning once it no
 * longer gives anything away. Called from bot threads; implementations must be thread-safe.
 */
public interface ReasoningLog {

    /** A move {@code decision.nick()} sent in {@code room}. */
    void record(String room, Decision decision);

    /**
     * The newest hand a bot has seen in {@code room}; {@code over} once the game has ended.
     * Hands before it are finished, so their decisions are safe to show.
     */
    void handSeen(String room, int hand, boolean over);

    /** Keeps nothing. */
    ReasoningLog NONE = new ReasoningLog() {
        @Override
        public void record(String room, Decision decision) {}

        @Override
        public void handSeen(String room, int hand, boolean over) {}
    };
}
