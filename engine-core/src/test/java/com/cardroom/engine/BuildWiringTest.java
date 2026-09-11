package com.cardroom.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.cardroom.contract.Intent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

/** Step 0 smoke test: toolchain, project graph and the Jackson boundary all resolve. */
class BuildWiringTest {

    @Test
    void runsOnJava17() {
        assertEquals(17, Runtime.version().feature());
    }

    @Test
    void seesContractTypes() {
        Intent anonymous = new Intent() {};
        assertEquals("com.cardroom.contract.Intent", Intent.class.getName());
        assertEquals(Intent.class, anonymous.getClass().getInterfaces()[0]);
    }

    @Test
    void jacksonArrivesTransitivelyFromTheContract() throws Exception {
        var node = new ObjectMapper().readTree("{\"type\":\"draw\"}");
        assertEquals("draw", node.get("type").asText());
    }
}
