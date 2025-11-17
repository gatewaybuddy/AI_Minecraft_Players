package com.aiplayer.core;

import java.util.UUID;

/**
 * AI Player Entity
 *
 * Represents an autonomous AI player in Minecraft.
 * This is a stub that will be fully implemented in Phase 1, Task 1.5.
 *
 * TODO: Implement full FakePlayer entity in Phase 1
 */
public class AIPlayerEntity {
    private final UUID uuid;
    private final String name;

    public AIPlayerEntity(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    /**
     * Tick the AI player
     * Called every game tick (20 times per second)
     */
    public void tick() {
        // TODO: Implement AI tick logic in Phase 1
    }
}
