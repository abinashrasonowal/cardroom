package com.whitejack.server.config;

import com.whitejack.contract.PlayerId;
import com.whitejack.contract.RoomCode;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.IOException;
import java.util.function.Function;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Identifier records go over the wire as bare strings, not {@code {"value": …}}. Spring Boot
 * registers any {@code Module} bean on its ObjectMapper, so HTTP and the socket agree.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public SimpleModule whitejackWireModule() {
        SimpleModule module = new SimpleModule("whitejack-wire");
        module.addSerializer(PlayerId.class, new ToStringSerializer<>(PlayerId::value));
        module.addSerializer(RoomCode.class, new ToStringSerializer<>(RoomCode::value));
        return module;
    }

    private static final class ToStringSerializer<T> extends JsonSerializer<T> {
        private final Function<T, String> value;

        ToStringSerializer(Function<T, String> value) {
            this.value = value;
        }

        @Override
        public void serialize(T id, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeString(value.apply(id));
        }
    }
}
