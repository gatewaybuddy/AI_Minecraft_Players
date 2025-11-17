package com.aiplayer.core;

import com.aiplayer.AIPlayerMod;
import com.aiplayer.util.AILogger;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

import java.util.UUID;

/**
 * AI Player Entity
 *
 * Represents an autonomous AI player in Minecraft.
 * This extends ServerPlayerEntity to function as a real player in the game world.
 *
 * The AI player can:
 * - Appear in the player list
 * - Move around the world
 * - Interact with blocks and entities
 * - Be perceived by other players and mobs
 *
 * @author AI Minecraft Players Team
 */
public class AIPlayerEntity extends ServerPlayerEntity {
    private final String aiName;
    private long tickCounter = 0;
    private boolean isActive = true;

    // AI Brain - will be implemented in later tasks
    // private AIPlayerBrain brain;

    /**
     * Create a new AI player entity
     *
     * @param server The Minecraft server
     * @param world The world to spawn in
     * @param profile The game profile (contains name and UUID)
     */
    public AIPlayerEntity(MinecraftServer server, ServerWorld world, GameProfile profile) {
        super(server, world, profile, null);
        this.aiName = profile.getName();

        AILogger.info("Created AI player entity: {} (UUID: {})", aiName, profile.getId());
    }

    /**
     * Initialize the AI player after spawning
     */
    public void initialize() {
        // Set game mode to survival by default
        this.changeGameMode(GameMode.SURVIVAL);

        // Initialize AI brain (will be implemented in Task 1.9)
        // this.brain = new AIPlayerBrain(this);

        AILogger.info("Initialized AI player: {}", aiName);
    }

    /**
     * Tick the AI player
     * Called every game tick (20 times per second)
     */
    @Override
    public void tick() {
        if (!isActive) {
            return;
        }

        // Call parent tick for basic player functionality
        super.tick();

        tickCounter++;

        // AI decision making every 10 ticks (0.5 seconds)
        // This reduces CPU usage and makes behavior more realistic
        if (tickCounter % 10 == 0) {
            updateAI();
        }
    }

    /**
     * Update AI decision making
     * Called every 0.5 seconds (10 ticks)
     */
    private void updateAI() {
        // TODO: Implement in Task 1.9 - Basic AI Brain Structure
        // For now, just log that we're alive every 5 seconds
        if (tickCounter % 100 == 0) {
            AILogger.debug("[{}] AI player active at position: {}",
                aiName,
                this.getPos().toString());
        }
    }

    /**
     * Get the AI player's name
     */
    public String getAIName() {
        return aiName;
    }

    /**
     * Check if this AI player is active
     */
    public boolean isActive() {
        return isActive;
    }

    /**
     * Deactivate this AI player
     * Called before despawning
     */
    public void deactivate() {
        this.isActive = false;
        AILogger.info("Deactivated AI player: {}", aiName);
    }

    /**
     * Override to prevent some automated server actions
     */
    @Override
    public boolean isSpectator() {
        return false;
    }

    /**
     * Override to prevent some automated server actions
     */
    @Override
    public boolean isCreative() {
        return this.interactionManager.getGameMode() == GameMode.CREATIVE;
    }

    /**
     * Get current position as a friendly string
     */
    public String getPositionString() {
        Vec3d pos = this.getPos();
        return String.format("(%.1f, %.1f, %.1f)", pos.x, pos.y, pos.z);
    }

    /**
     * Get tick counter for debugging
     */
    public long getTickCounter() {
        return tickCounter;
    }

    /**
     * Custom toString for debugging
     */
    @Override
    public String toString() {
        return String.format("AIPlayer{name='%s', uuid=%s, pos=%s, active=%s}",
            aiName,
            this.getUuid(),
            getPositionString(),
            isActive);
    }
}
