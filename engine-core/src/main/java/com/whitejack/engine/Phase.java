package com.whitejack.engine;

/** Room lifecycle. Transitions are the engine's business, never a game module's. */
public enum Phase {
    LOBBY,
    PLAYING,
    FINISHED
}
