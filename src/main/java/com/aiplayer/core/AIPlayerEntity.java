package com.aiplayer.core;

import com.aiplayer.action.ActionController;
import com.aiplayer.perception.WorldPerceptionEngine;
import com.aiplayer.perception.WorldState;
import com.mojang.authlib.GameProfile;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * AI Player Entity - A fake player controlled by AI.
 *
 * This class extends ServerPlayerEntity to create a fully functional player that:
 * - Appears in the player list
 * - Can perform all player actions
 * - Is controlled by AI instead of a human
 *
 * The AI decision-making is delegated to AIPlayerBrain.
 * Actions are executed through various controllers (Movement, Mining, etc.).
 */
public class AIPlayerEntity extends ServerPlayerEntity {

    private static final Logger LOGGER = LoggerFactory.getLogger(AIPlayerEntity.class);

    private final UUID aiPlayerId;
    private final AIPlayerBrain brain;
    private final WorldPerceptionEngine perceptionEngine;
    private final ActionController actionController;

    // Movement state (Phase 1)
    private double aiMovementX = 0;
    private double aiMovementZ = 0;

    // Configuration
    private final boolean autoRespawn;

    /**
     * Create a new AI player entity.
     *
     * @param server Minecraft server instance
     * @param world The world to spawn in
     * @param profile Player profile (name and UUID)
     * @param autoRespawn Whether to automatically respawn on death
     */
    public AIPlayerEntity(MinecraftServer server, ServerWorld world, GameProfile profile, boolean autoRespawn) {
        super(server, world, profile, null);

        this.aiPlayerId = UUID.randomUUID();
        this.autoRespawn = autoRespawn;

        // Initialize AI components
        this.brain = new AIPlayerBrain(this);
        this.perceptionEngine = new WorldPerceptionEngine(this);
        this.actionController = new ActionController(this, false); // PvP disabled by default

        // Set game mode to survival
        this.changeGameMode(GameMode.SURVIVAL);

        LOGGER.info("Created AI player: {} (UUID: {})", profile.getName(), aiPlayerId);
    }

    /**
     * Main tick method - called every game tick (20 times per second).
     * This is where the AI makes decisions and performs actions.
     */
    @Override
    public void tick() {
        super.tick();

        // Only update AI every 10 ticks (0.5 seconds) to reduce CPU usage
        if (age % 10 == 0) {
            updateAI();
        }

        // Apply AI movement every tick for smooth movement
        applyAIMovement();
    }

    /**
     * Update AI decision making.
     */
    private void updateAI() {
        try {
            // Perceive the world
            WorldState worldState = perceptionEngine.perceiveWorld();

            // Update action controller (handles path following, etc.)
            actionController.update();

            // Let the brain decide what to do
            brain.update(worldState);

        } catch (Exception e) {
            LOGGER.error("Error updating AI for {}", getName().getString(), e);
        }
    }

    /**
     * Apply movement based on AI decisions.
     * Called every tick for smooth movement.
     */
    private void applyAIMovement() {
        if (aiMovementX == 0 && aiMovementZ == 0) {
            return; // Not moving
        }

        // Calculate movement vector
        Vec3d movement = new Vec3d(aiMovementX, 0, aiMovementZ).normalize().multiply(0.2);

        // Apply movement
        setVelocity(movement.x, getVelocity().y, movement.z);

        // Update look direction to face movement direction
        if (movement.lengthSquared() > 0.001) {
            float yaw = (float) (Math.atan2(movement.z, movement.x) * 180 / Math.PI) - 90;
            setYaw(yaw);
            setHeadYaw(yaw);
        }
    }

    /**
     * Set the AI's desired movement direction.
     * Called by AIPlayerBrain.
     *
     * @param dx Direction X component (-1 to 1)
     * @param dz Direction Z component (-1 to 1)
     */
    public void setAIMovementDirection(double dx, double dz) {
        this.aiMovementX = dx;
        this.aiMovementZ = dz;
    }

    /**
     * Stop AI movement.
     */
    public void stopMovement() {
        this.aiMovementX = 0;
        this.aiMovementZ = 0;
        setVelocity(Vec3d.ZERO);
    }

    /**
     * Handle death - auto-respawn if configured.
     */
    @Override
    public void onDeath(DamageSource source) {
        super.onDeath(source);

        LOGGER.info("AI player {} died: {}", getName().getString(), source.getName());

        if (autoRespawn) {
            // Schedule respawn (will be implemented in later phase)
            LOGGER.debug("Auto-respawn enabled for {}", getName().getString());
        }
    }

    /**
     * Get the AI player's brain (for status queries).
     */
    public AIPlayerBrain getBrain() {
        return brain;
    }

    /**
     * Get the AI player's action controller.
     */
    public ActionController getActionController() {
        return actionController;
    }

    /**
     * Get the AI player's unique ID.
     */
    public UUID getAIPlayerId() {
        return aiPlayerId;
    }

    /**
     * Send a message to nearby players.
     * Used for AI chat (Phase 4).
     */
    public void sendChatMessage(String message) {
        Text text = Text.literal("<" + getName().getString() + "> " + message);
        getServer().getPlayerManager().broadcast(text, false);
    }

    /**
     * Cleanup when AI player is removed.
     */
    public void cleanup() {
        LOGGER.info("Cleaning up AI player: {}", getName().getString());
        stopMovement();
        actionController.stopAll();
    }

    /**
     * Get status information for display.
     */
    public String getStatusInfo() {
        return String.format("AI Player: %s | Position: %.1f, %.1f, %.1f | Health: %.1f | Goal: %s",
            getName().getString(),
            getX(), getY(), getZ(),
            getHealth(),
            brain.getCurrentGoalDescription()
        );
    }

    @Override
    public boolean isSpectator() {
        return false;
    }

    @Override
    public boolean isCreative() {
        return false;
    }
}
