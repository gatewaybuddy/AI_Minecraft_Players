package com.aiplayer.action;

import com.aiplayer.core.AIPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Combat Controller - Handles combat for AI players.
 *
 * Features:
 * - Entity targeting
 * - Attack timing (respects attack cooldown)
 * - Weapon selection (best available)
 * - Defensive movement (strafing, retreating)
 * - PvP control (can be disabled)
 *
 * Phase 2: Basic combat
 * Phase 5: Advanced tactics, teamwork
 */
public class CombatController {

    private static final Logger LOGGER = LoggerFactory.getLogger(CombatController.class);

    private final AIPlayerEntity player;
    private final boolean pvpEnabled;

    // Combat state
    private LivingEntity currentTarget;
    private long lastAttackTime;
    private static final long ATTACK_COOLDOWN_MS = 600; // 0.6 seconds

    public CombatController(AIPlayerEntity player, boolean pvpEnabled) {
        this.player = player;
        this.pvpEnabled = pvpEnabled;
        this.lastAttackTime = 0;
    }

    /**
     * Attack an entity.
     *
     * @param target Entity to attack
     * @return Future that completes when attack is done
     */
    public CompletableFuture<ActionResult> attackEntity(LivingEntity target) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Validate target
                if (target == null || !target.isAlive()) {
                    return ActionResult.failure("Target is dead or invalid");
                }

                // Check PvP permission
                if (target instanceof PlayerEntity && !pvpEnabled) {
                    return ActionResult.failure("PvP is disabled");
                }

                // Check if on cooldown
                long now = System.currentTimeMillis();
                if (now - lastAttackTime < ATTACK_COOLDOWN_MS) {
                    return ActionResult.failure("Attack on cooldown");
                }

                // Check distance
                double distance = player.distanceTo(target);
                if (distance > 4.0) {
                    return ActionResult.failure("Target too far away (" + String.format("%.1f", distance) + " blocks)");
                }

                // Select best weapon
                ItemStack weapon = selectBestWeapon();
                if (!weapon.isEmpty()) {
                    equipWeapon(weapon);
                }

                // Look at target
                lookAtEntity(target);

                // Attack
                player.attack(target);

                lastAttackTime = now;
                currentTarget = target;

                LOGGER.debug("Attacked {} (health: {})", target.getName().getString(), target.getHealth());

                return ActionResult.success("Attacked " + target.getName().getString());

            } catch (Exception e) {
                LOGGER.error("Error attacking entity", e);
                return ActionResult.failure("Attack error: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Select the best weapon from inventory.
     */
    private ItemStack selectBestWeapon() {
        PlayerInventory inventory = player.getInventory();
        ItemStack bestWeapon = ItemStack.EMPTY;
        float bestDamage = 1.0f; // Fist damage

        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);

            if (stack.isEmpty()) {
                continue;
            }

            // Check if it's a weapon
            if (stack.getItem() instanceof SwordItem sword) {
                float damage = sword.getAttackDamage();

                // Check durability
                if (stack.isDamageable() && stack.getDamage() >= stack.getMaxDamage() - 5) {
                    continue;
                }

                if (damage > bestDamage) {
                    bestDamage = damage;
                    bestWeapon = stack;
                }
            }
        }

        return bestWeapon;
    }

    /**
     * Equip a weapon in the main hand.
     */
    private boolean equipWeapon(ItemStack weapon) {
        if (weapon.isEmpty()) {
            return true; // Fighting with fists
        }

        PlayerInventory inventory = player.getInventory();

        // Find weapon in inventory
        int slot = -1;
        for (int i = 0; i < inventory.size(); i++) {
            if (inventory.getStack(i) == weapon) {
                slot = i;
                break;
            }
        }

        if (slot == -1) {
            return false;
        }

        // If in hotbar, select it
        if (slot < 9) {
            inventory.selectedSlot = slot;
            return true;
        }

        // Otherwise, swap with current hotbar slot
        ItemStack currentItem = inventory.getStack(inventory.selectedSlot);
        inventory.setStack(inventory.selectedSlot, weapon);
        inventory.setStack(slot, currentItem);

        return true;
    }

    /**
     * Look at an entity.
     */
    private void lookAtEntity(Entity target) {
        Vec3d targetPos = target.getPos().add(0, target.getHeight() / 2, 0);

        double dx = targetPos.x - player.getX();
        double dy = targetPos.y - player.getEyeY();
        double dz = targetPos.z - player.getZ();

        double distance = Math.sqrt(dx * dx + dz * dz);

        float yaw = (float) (Math.atan2(dz, dx) * 180 / Math.PI) - 90;
        float pitch = (float) -(Math.atan2(dy, distance) * 180 / Math.PI);

        player.setYaw(yaw);
        player.setPitch(pitch);
        player.setHeadYaw(yaw);
    }

    /**
     * Check if can attack (cooldown ready).
     */
    public boolean canAttack() {
        long now = System.currentTimeMillis();
        return now - lastAttackTime >= ATTACK_COOLDOWN_MS;
    }

    /**
     * Get current combat target.
     */
    public Optional<LivingEntity> getCurrentTarget() {
        if (currentTarget != null && currentTarget.isAlive()) {
            return Optional.of(currentTarget);
        }
        return Optional.empty();
    }

    /**
     * Clear current target.
     */
    public void clearTarget() {
        currentTarget = null;
    }

    /**
     * Find nearest hostile entity.
     */
    public Optional<LivingEntity> findNearestHostile(double maxDistance) {
        return player.getWorld()
            .getEntitiesByClass(
                HostileEntity.class,
                player.getBoundingBox().expand(maxDistance),
                entity -> entity.isAlive() && player.distanceTo(entity) <= maxDistance
            )
            .stream()
            .min((a, b) -> Double.compare(player.distanceTo(a), player.distanceTo(b)));
    }

    /**
     * Check if entity is a valid target.
     */
    public boolean isValidTarget(Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            return false;
        }

        if (!living.isAlive()) {
            return false;
        }

        // Don't attack self
        if (entity == player) {
            return false;
        }

        // Check PvP
        if (entity instanceof PlayerEntity && !pvpEnabled) {
            return false;
        }

        // Check distance
        return player.distanceTo(entity) <= 4.0;
    }

    /**
     * Retreat from target (defensive movement).
     */
    public void retreatFrom(Entity threat) {
        Vec3d threatPos = threat.getPos();
        Vec3d playerPos = player.getPos();

        // Move away from threat
        double dx = playerPos.x - threatPos.x;
        double dz = playerPos.z - threatPos.z;

        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance > 0.1) {
            dx /= distance;
            dz /= distance;
            player.setAIMovementDirection(dx, dz);
        }
    }

    /**
     * Strafe around target (advanced combat).
     */
    public void strafeAroundTarget(Entity target, boolean clockwise) {
        Vec3d targetPos = target.getPos();
        Vec3d playerPos = player.getPos();

        // Calculate perpendicular direction
        double dx = targetPos.x - playerPos.x;
        double dz = targetPos.z - playerPos.z;

        // Rotate 90 degrees
        double strafeDx, strafeDz;
        if (clockwise) {
            strafeDx = dz;
            strafeDz = -dx;
        } else {
            strafeDx = -dz;
            strafeDz = dx;
        }

        double distance = Math.sqrt(strafeDx * strafeDx + strafeDz * strafeDz);
        if (distance > 0.1) {
            strafeDx /= distance;
            strafeDz /= distance;
            player.setAIMovementDirection(strafeDx, strafeDz);
        }
    }

    /**
     * Check if PvP is enabled.
     */
    public boolean isPvpEnabled() {
        return pvpEnabled;
    }
}
