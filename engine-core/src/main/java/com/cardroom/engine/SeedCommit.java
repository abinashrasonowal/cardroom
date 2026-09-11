package com.cardroom.engine;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * One per hand, never per room. A room plays many hands; reusing a seed means that after the
 * first reveal every player can compute the next deck.
 *
 * <p>The commit and the reveal travel as log events — {@code HandStarted(commit, …)} at the
 * hand's seq 0 and {@code SeedRevealed(serverSeed)} at the end — not as fields on {@code Room}.
 * That way they are ordered against every card event, arrive on reconnect for free, and no
 * verifier can be shown a different commit than the players saw.
 */
public final class SeedCommit {

    private static final SecureRandom SECURE = new SecureRandom();
    private static final int SEED_BYTES = 32;

    private final byte[] serverSeed;
    private final String commit;

    private SeedCommit(byte[] serverSeed) {
        if (serverSeed.length != SEED_BYTES) {
            throw new IllegalArgumentException("serverSeed must be " + SEED_BYTES + " bytes, got " + serverSeed.length);
        }
        this.serverSeed = serverSeed;
        this.commit = HexFormat.of().formatHex(sha256(serverSeed));
    }

    public static SeedCommit fresh() {
        byte[] seed = new byte[SEED_BYTES];
        SECURE.nextBytes(seed);
        return new SeedCommit(seed);
    }

    /** For replay and tests: rebuild the commit for a seed that was already revealed. */
    public static SeedCommit of(byte[] serverSeed) {
        return new SeedCommit(serverSeed.clone());
    }

    /** sha256(serverSeed), lower-case hex. Broadcast before the deal. */
    public String commit() {
        return commit;
    }

    /** Broadcast after the hand ends. Copied, so a caller cannot zero the engine's seed. */
    public byte[] reveal() {
        return serverSeed.clone();
    }

    /** What the {@code /verify} page checks first: the server did not change its mind. */
    public static boolean verify(String commit, byte[] serverSeed) {
        byte[] expected = HexFormat.of().parseHex(commit);
        return MessageDigest.isEqual(expected, sha256(serverSeed));
    }

    private static byte[] sha256(byte[] input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required by every JVM", e);
        }
    }

    /**
     * Deliberately prints the commit and never the seed: one {@code log.debug("{}", seedCommit)}
     * before the hand ends would hand the whole deal to anyone reading the logs.
     */
    @Override
    public String toString() {
        return "SeedCommit[commit=" + commit + "]";
    }

    static byte[] utf8(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }
}
