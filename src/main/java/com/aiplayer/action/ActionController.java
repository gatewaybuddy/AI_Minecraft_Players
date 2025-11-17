package com.aiplayer.action;

import com.aiplayer.core.AIPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Action Controller - Central coordinator for all AI player actions.
 *
 * This class provides a unified interface to all action controllers:
 * - MovementController - Navigation and pathfinding
 * - MiningController - Block breaking
 * - BuildingController - Block placement
 * - CombatController - Entity combat
 * - InventoryManager - Item management
 *
 * Acts as a facade pattern to simplify action execution for the AI brain.
 */
public class ActionController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActionController.class);

    private final AIPlayerEntity player;

    // Sub-controllers
    private final MovementController movementController;
    private final MiningController miningController;
    private final BuildingController buildingController;
    private final CombatController combatController;
    private final InventoryManager inventoryManager;

    public ActionController(AIPlayerEntity player, boolean pvpEnabled) {
        this.player = player;

        // Initialize all controllers
        this.movementController = new MovementController(player);
        this.miningController = new MiningController(player);
        this.buildingController = new BuildingController(player);
        this.combatController = new CombatController(player, pvpEnabled);
        this.inventoryManager = new InventoryManager(player);

        LOGGER.debug("ActionController initialized for {}", player.getName().getString());
    }

    /**
     * Update all controllers (called periodically).
     */
    public void update() {
        // Update movement (handles path following)
        movementController.update();

        // Other controllers are action-based, don't need periodic updates
    }

    // ===== Movement =====

    public MovementController movement() {
        return movementController;
    }

    // ===== Mining =====

    public MiningController mining() {
        return miningController;
    }

    // ===== Building =====

    public BuildingController building() {
        return buildingController;
    }

    // ===== Combat =====

    public CombatController combat() {
        return combatController;
    }

    // ===== Inventory =====

    public InventoryManager inventory() {
        return inventoryManager;
    }

    /**
     * Stop all current actions.
     */
    public void stopAll() {
        movementController.stopMovement();
        miningController.cancelMining();
        combatController.clearTarget();

        LOGGER.debug("Stopped all actions for {}", player.getName().getString());
    }

    /**
     * Check if any action is currently executing.
     */
    public boolean isBusy() {
        return movementController.isMoving() ||
               miningController.isMining() ||
               combatController.getCurrentTarget().isPresent();
    }

    /**
     * Get status summary of all controllers.
     */
    public String getStatusSummary() {
        StringBuilder sb = new StringBuilder();

        if (movementController.isMoving()) {
            sb.append("[MOVING] ");
            movementController.getCurrentPath().ifPresent(path ->
                sb.append("Following path (").append(path.getLength()).append(" waypoints) ")
            );
        }

        if (miningController.isMining()) {
            sb.append("[MINING] ");
            sb.append("Progress: ").append(String.format("%.0f%%", miningController.getMiningProgress() * 100));
        }

        combatController.getCurrentTarget().ifPresent(target ->
            sb.append("[COMBAT] ").append("Fighting ").append(target.getName().getString()).append(" ")
        );

        if (sb.length() == 0) {
            return "[IDLE]";
        }

        return sb.toString().trim();
    }
}
