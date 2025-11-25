package com.aiplayer.core;

import com.aiplayer.AIPlayerMod;
import com.aiplayer.action.ActionController;
import com.aiplayer.chat.ChatSystem;
import com.aiplayer.llm.LLMProvider;
import com.aiplayer.perception.WorldPerceptionEngine;
import com.aiplayer.perception.WorldState;
import com.mojang.authlib.GameProfile;
import net.minecraft.entity.Entity;
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
    private final ChatSystem chatSystem;

    // Movement state (Phase 1)
    private double aiMovementX = 0;
    private double aiMovementZ = 0;

    // Configuration
    private final boolean autoRespawn;

    // Respawn state
    private int respawnCooldown = 0;
    private boolean pendingRespawn = false;
    private boolean hasSentGreeting = false;

    /**
     * Create a new AI player entity with LLM provider (intelligent mode).
     *
     * @param server Minecraft server instance
     * @param world The world to spawn in
     * @param profile Player profile (name and UUID)
     * @param autoRespawn Whether to automatically respawn on death
     * @param llmProvider LLM provider for intelligent planning (null for simple mode)
     */
    public AIPlayerEntity(MinecraftServer server, ServerWorld world, GameProfile profile, boolean autoRespawn, LLMProvider llmProvider) {
        super(server, world, profile);

        this.aiPlayerId = UUID.randomUUID();
        this.autoRespawn = autoRespawn;

        // Initialize AI components with LLM provider
        this.brain = new AIPlayerBrain(this, llmProvider);
        this.perceptionEngine = new WorldPerceptionEngine(this);
        this.actionController = new ActionController(this, false); // PvP disabled by default

        // Initialize chat system if LLM is available
        if (llmProvider != null && llmProvider.isAvailable()) {
            this.chatSystem = new ChatSystem(this, llmProvider);
            LOGGER.info("Chat system initialized for {}", profile.getName());
        } else {
            this.chatSystem = null;
            LOGGER.info("Chat system disabled (no LLM available) for {}", profile.getName());
        }

        // Set game mode to survival
        this.changeGameMode(GameMode.SURVIVAL);

        String mode = (llmProvider != null && brain.isIntelligentMode()) ? "INTELLIGENT" : "SIMPLE";
        LOGGER.info("Created AI player: {} (UUID: {}, mode: {})", profile.getName(), aiPlayerId, mode);
    }

    /**
     * Create a new AI player entity in simple mode (no LLM).
     *
     * @param server Minecraft server instance
     * @param world The world to spawn in
     * @param profile Player profile (name and UUID)
     * @param autoRespawn Whether to automatically respawn on death
     */
    public AIPlayerEntity(MinecraftServer server, ServerWorld world, GameProfile profile, boolean autoRespawn) {
        this(server, world, profile, autoRespawn, null);
    }

    /**
     * Main tick method - called every game tick (20 times per second).
     * This is where the AI makes decisions and performs actions.
     */
    @Override
    public void tick() {
        try {
            super.tick();

            // Handle respawn cooldown
            if (respawnCooldown > 0) {
                respawnCooldown--;
                if (respawnCooldown == 0) {
                    if (pendingRespawn) {
                        LOGGER.info("[ENTITY] Respawn cooldown complete - respawning {}", getName().getString());
                        requestRespawn();
                        hasSentGreeting = false; // Reset greeting flag
                        sendChatMessage("I'm back! That was unpleasant...");
                        pendingRespawn = false;
                    } else if (isDead()) {
                        // Auto-respawn disabled - remove dead AI
                        LOGGER.info("[ENTITY] Removing dead AI player {}", getName().getString());
                        discard();
                        remove(RemovalReason.KILLED);
                    }
                }
                return; // Don't update AI while dead/respawning
            }

            // Stop all AI activity if dead
            if (isDead() || getHealth() <= 0) {
                if (age % 20 == 0) { // Log once per second
                    LOGGER.info("[ENTITY] {} is dead - stopping AI updates", getName().getString());
                }
                stopMovement();
                return;
            }

            // Send greeting on first tick
            if (!hasSentGreeting && age > 5) {
                sendChatMessage("Hello! I'm " + getName().getString() + " and I'm ready to help!");
                hasSentGreeting = true;
                LOGGER.info("[ENTITY] {} sent greeting message", getName().getString());
            }

            // Update AI based on config (default: every 5 ticks = 4 times per second)
            int updateInterval = AIPlayerMod.getConfig().getBehavior().getAiUpdateIntervalTicks();
            if (age % updateInterval == 0) {
                LOGGER.debug("[ENTITY] Tick {} - Updating AI for {}", age, getName().getString());
                updateAI();
            }

            // Apply AI movement every tick for smooth movement
            applyAIMovement();
        } catch (Exception e) {
            // Catch exceptions from other mods to prevent server crashes
            LOGGER.error("[ENTITY] Error in AI player tick for {}: {}", getName().getString(), e.getMessage(), e);
        }
    }

    /**
     * Update AI decision making.
     */
    private void updateAI() {
        try {
            LOGGER.debug("[ENTITY] Starting AI update cycle - Tick: {}", age);

            // Perceive the world
            WorldState worldState = perceptionEngine.perceiveWorld();
            LOGGER.debug("[ENTITY] World state perceived - Pos: {}, Entities: {}",
                worldState.getPlayerPosition(), worldState.getNearbyEntities().size());

            // Update action controller (handles path following, etc.)
            actionController.update();

            // Let the brain decide what to do
            LOGGER.debug("[ENTITY] Calling brain.update()");
            brain.update(worldState);

            LOGGER.debug("[ENTITY] AI update cycle complete");

        } catch (Exception e) {
            LOGGER.error("[ENTITY] ERROR in updateAI", e);
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

        // Update look direction FIRST to face movement direction
        float yaw = (float) (Math.atan2(aiMovementZ, aiMovementX) * 180 / Math.PI) - 90;
        setYaw(yaw);
        setHeadYaw(yaw);

        // Calculate movement vector in world space
        Vec3d movement = new Vec3d(aiMovementX, 0, aiMovementZ).normalize().multiply(0.1);

        // Use move() method which handles collision, physics, and gravity properly
        // MovementType.SELF means player-initiated movement (not piston, water, etc.)
        move(net.minecraft.entity.MovementType.SELF, movement);
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
        LOGGER.debug("[ENTITY] Stopping AI movement - Velocity set to zero");
        this.aiMovementX = 0;
        this.aiMovementZ = 0;
        setVelocity(Vec3d.ZERO);
    }

    /**
     * Handle death - auto-respawn if configured.
     */
    @Override
    public void onDeath(DamageSource source) {
        LOGGER.info("[ENTITY] AI player {} died from: {}", getName().getString(), source.getName());

        super.onDeath(source);

        // Store death memory
        if (brain != null) {
            brain.getMemorySystem().store(new com.aiplayer.memory.Memory(
                com.aiplayer.memory.Memory.MemoryType.OBSERVATION,
                "I died from: " + source.getName(),
                0.95
            ));
        }

        if (autoRespawn) {
            LOGGER.info("[ENTITY] Scheduling auto-respawn for {} in 3 seconds (60 ticks)", getName().getString());
            // Use tick-based delay instead of Thread.sleep()
            respawnCooldown = 60; // 3 seconds at 20 ticks/second
            pendingRespawn = true;
        } else {
            LOGGER.info("[ENTITY] Auto-respawn disabled - will remove after 5 seconds");
            // Use tick-based delay for removal too
            respawnCooldown = 100; // 5 seconds
            pendingRespawn = false;
            // Removal will be handled in tick() method
        }
    }

    /**
     * Get the AI player's brain (for status queries).
     * Note: renamed from getBrain() to getAIBrain() to avoid conflict with LivingEntity.getBrain()
     */
    public AIPlayerBrain getAIBrain() {
        return brain;
    }

    /**
     * Get the AI player's action controller.
     */
    public ActionController getActionController() {
        return actionController;
    }

    /**
     * Get the AI player's chat system.
     *
     * @return ChatSystem instance, or null if LLM is unavailable
     */
    @Nullable
    public ChatSystem getChatSystem() {
        return chatSystem;
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
