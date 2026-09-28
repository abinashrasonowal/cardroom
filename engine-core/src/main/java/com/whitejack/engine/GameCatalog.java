package com.whitejack.engine;

import com.whitejack.contract.GameDefinition;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * Every game on the classpath, found through {@code META-INF/services}. This is the only place
 * the engine learns a game exists, which is what lets a new game ship without touching
 * engine-core.
 */
public final class GameCatalog {

    private final Map<String, GameDefinition<?, ?, ?>> games;

    private GameCatalog(Map<String, GameDefinition<?, ?, ?>> games) {
        this.games = Collections.unmodifiableMap(games);
    }

    /** Loads with the thread's context class loader, which is what a Spring Boot fat jar needs. */
    public static GameCatalog load() {
        return load(Thread.currentThread().getContextClassLoader());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static GameCatalog load(ClassLoader loader) {
        Map<String, GameDefinition<?, ?, ?>> games = new LinkedHashMap<>();
        for (GameDefinition def : ServiceLoader.load(GameDefinition.class, loader)) {
            GameDefinition<?, ?, ?> previous = games.putIfAbsent(def.id(), def);
            if (previous != null) {
                // Two jars claiming one id would make room creation depend on classpath order.
                throw new IllegalStateException("duplicate game id '" + def.id() + "': "
                        + previous.getClass().getName() + " and " + def.getClass().getName());
            }
        }
        return new GameCatalog(games);
    }

    /** @throws IllegalArgumentException for an id no game on the classpath declares */
    public GameDefinition<?, ?, ?> require(String id) {
        GameDefinition<?, ?, ?> def = games.get(id);
        if (def == null) throw new IllegalArgumentException("no such game: " + id);
        return def;
    }

    public Collection<GameDefinition<?, ?, ?>> all() {
        return games.values();
    }
}
