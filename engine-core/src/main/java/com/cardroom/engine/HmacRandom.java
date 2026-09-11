package com.cardroom.engine;

import com.cardroom.contract.RandomSource;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.regex.Pattern;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * The seeded, replayable {@link RandomSource} every deal comes from.
 *
 * <h2>Wire format — changing any byte below breaks {@code /verify} forever</h2>
 *
 * <pre>
 * message   = clientSeed1 : clientSeed2 : … : clientSeedN | handIndex      (UTF-8, lower-case hex seeds)
 * handSeed  = HMAC-SHA256(key = serverSeed, msg = message)
 * block(i)  = HMAC-SHA256(key = handSeed,   msg = i as 8-byte big-endian)
 * stream    = block(0) ‖ block(1) ‖ …, read 4 bytes at a time, big-endian unsigned
 * </pre>
 *
 * <p>In the browser that is about twelve lines of WebCrypto, which is the whole reason it is
 * not {@code SplittableRandom}: that class's only constructor takes a {@code long}, so 32
 * secure bytes would collapse to 8, and JavaScript has no SplitMix64 to re-derive the deal.
 *
 * <p><b>Why the client seeds.</b> A server that alone picks the seed can grind: generate ten
 * thousand candidates, simulate each deal, and commit to the one where the house wins. The
 * commit and the reveal still verify perfectly. Mixing in a contribution nobody else controls
 * means no single party can steer the deal.
 *
 * <p><b>Not thread-safe.</b> One instance per hand, used only on that room's thread. The
 * counter is mutable state, which is also why a {@code RandomSource} never belongs in {@code S}.
 */
public final class HmacRandom implements RandomSource {

    /**
     * Client seeds are hex, so joining with ':' is unambiguous. Without a separator a player
     * could pick "bc" to make {"a","bc"} derive the same deal as another table's {"ab","c"},
     * and the point of the client contribution would be gone.
     */
    private static final Pattern HEX_SEED = Pattern.compile("[0-9a-f]{8,128}");

    private static final String HMAC = "HmacSHA256";
    private static final int BLOCK_BYTES = 32;

    private final byte[] handSeed;
    private byte[] block = new byte[0];
    private int offset = 0;
    private long counter = 0;

    private HmacRandom(byte[] handSeed) {
        this.handSeed = handSeed;
    }

    /**
     * @param serverSeed 32 secure bytes, committed to before the deal
     * @param clientSeeds one per seated player, in seat order; lower-case hex, 8..128 chars
     * @param handIndex 0 for a room's first hand
     */
    public static HmacRandom forHand(byte[] serverSeed, List<String> clientSeeds, int handIndex) {
        if (handIndex < 0) throw new IllegalArgumentException("negative handIndex: " + handIndex);
        StringBuilder msg = new StringBuilder();
        for (int i = 0; i < clientSeeds.size(); i++) {
            String seed = clientSeeds.get(i);
            if (seed == null || !HEX_SEED.matcher(seed).matches()) {
                throw new IllegalArgumentException("clientSeed " + i + " is not 8..128 lower-case hex chars");
            }
            if (i > 0) msg.append(':');
            msg.append(seed);
        }
        msg.append('|').append(handIndex);
        return new HmacRandom(hmac(serverSeed, SeedCommit.utf8(msg.toString())));
    }

    @Override
    public int nextInt(int bound) {
        if (bound <= 0) throw new IllegalArgumentException("bound must be positive: " + bound);
        if (bound == 1) return 0;

        // Rejection sampling. `value % bound` alone would make the low values of an uneven
        // split fractionally likelier -- a bias small enough to survive eyeballing a shuffle
        // and large enough to be the whole story if anyone ever audits the deal.
        long limit = (1L << 32) - ((1L << 32) % bound);
        while (true) {
            long value = nextUnsignedInt();
            if (value < limit) return (int) (value % bound);
        }
    }

    private long nextUnsignedInt() {
        if (offset + 4 > block.length) {
            block = hmac(handSeed, ByteBuffer.allocate(8).putLong(counter++).array());
            offset = 0;
        }
        long value = ((block[offset] & 0xFFL) << 24)
                | ((block[offset + 1] & 0xFFL) << 16)
                | ((block[offset + 2] & 0xFFL) << 8)
                | (block[offset + 3] & 0xFFL);
        offset += 4;
        return value;
    }

    private static byte[] hmac(byte[] key, byte[] message) {
        try {
            Mac mac = Mac.getInstance(HMAC);
            mac.init(new SecretKeySpec(key, HMAC));
            byte[] out = mac.doFinal(message);
            assert out.length == BLOCK_BYTES;
            return out;
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("HmacSHA256 is required by every JVM", e);
        }
    }
}
