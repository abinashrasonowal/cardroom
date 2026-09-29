package com.whitejack.bots;

import java.util.Optional;

/**
 * A language model the bot may consult. An empty answer — no key, a timeout, an outage — is
 * normal, not an error: the bot then plays its heuristic move.
 */
public interface Advisor {

    Optional<String> ask(String system, String user);

    /** Heuristic play only. */
    Advisor NONE = (system, user) -> Optional.empty();
}
