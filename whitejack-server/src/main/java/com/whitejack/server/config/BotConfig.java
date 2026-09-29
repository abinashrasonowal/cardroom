package com.whitejack.server.config;

import com.whitejack.bots.Advisor;
import com.whitejack.bots.JevAdvisor;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The model behind the bots: TypeSafe's Jev on OpenRouter when a key is set, heuristic play
 * when not. Either way a bot only ever sends a move the server listed as legal.
 */
@Configuration
public class BotConfig {

    private static final Logger LOG = LoggerFactory.getLogger(BotConfig.class);

    @Bean
    public Advisor botAdvisor(
            @Value("${whitejack.bots.openrouter-api-key:}") String apiKey,
            @Value("${whitejack.bots.model:" + JevAdvisor.DEFAULT_MODEL + "}") String model,
            @Value("${whitejack.bots.timeout:8s}") Duration timeout) {
        if (apiKey.isBlank()) {
            LOG.info("whitejack.bots.openrouter-api-key is not set; bots play heuristic moves only");
            return Advisor.NONE;
        }
        LOG.info("bots consult {} on OpenRouter", model);
        return new JevAdvisor(apiKey, model, timeout);
    }
}
