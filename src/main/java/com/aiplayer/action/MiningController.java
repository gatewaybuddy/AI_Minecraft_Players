package com.aiplayer.action;

import com.aiplayer.core.AIPlayerEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolItem;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Mining Controller - Handles block breaking for AI players.
 *
 * Features:
 * - Intelligent tool selection (best available tool)
 * - Mining speed calculation
 * - Durability management (switch tools when low)
 * - Drop collection tracking
 * - Silk touch / Fortune awareness
 *
 * Implements the mining logic from TECHNICAL_SPEC.md.
 */
public class MiningController {

    private static final Logger LOGGER = LoggerFactory.getLogger(MiningController.class);

    private final AIPlayerEntity player;

    // Mining state
    private BlockPos currentlyMining;
    private long miningStartTime;
    private boolean isMining;

    public MiningController(AIPlayerEntity player) {
        this.player = player;
        this.isMining = false;
    }

    /**
     * Mine a block at the given position.
     *
     * @param pos Position of block to mine
     * @param requireCorrectTool If true, only mine if we have the right tool
     * @return Future that completes when mining is done
     */
    public CompletableFuture<ActionResult> mineBlock(BlockPos pos, boolean requireCorrectTool) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                World world = player.getWorld();
                BlockState state = world.getBlockState(pos);

                // Validate position
                if (state.isAir()) {
                    return ActionResult.failure("No block at position");
                }

                // Check distance
                double distance = player.getPos().distanceTo(pos.toCenterPos());
                if (distance > 6.0) {
                    return ActionResult.failure("Block too far away (" + String.format("%.1f", distance) + " blocks)");
                }

                // Select best tool
                ItemStack bestTool = selectBestTool(state);

                if (requireCorrectTool && !isCorrectTool(bestTool, state)) {
                    return ActionResult.failure("Don't have correct tool for " + state.getBlock().getName().getString());
                }

                // Equip tool
                if (!equipTool(bestTool)) {
                    return ActionResult.failure("Failed to equip tool");
                }

                // Look at block
                lookAtBlock(pos);

                // Calculate mining time
                float miningSpeed = calculateMiningSpeed(bestTool, state);
                float hardness = state.getHardness(world, pos);
                long miningTimeMs = (long) ((hardness * 1000) / miningSpeed);

                // Start mining
                isMining = true;
                currentlyMining = pos;
                miningStartTime = System.currentTimeMillis();

                LOGGER.debug("Mining {} at {} (speed: {}, time: {}ms)",
                    state.getBlock().getName().getString(), pos, miningSpeed, miningTimeMs);

                // Break block
                boolean success = player.interactionManager.tryBreakBlock(pos);

                // Wait for block to break (simulate mining time)
                Thread.sleep(Math.min(miningTimeMs, 5000)); // Cap at 5 seconds

                isMining = false;
                currentlyMining = null;

                if (success) {
                    return ActionResult.success("Mined " + state.getBlock().getName().getString());
                } else {
                    return ActionResult.failure("Failed to break block");
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                isMining = false;
                return ActionResult.failure("Mining interrupted", e);
            } catch (Exception e) {
                isMining = false;
                LOGGER.error("Error mining block", e);
                return ActionResult.failure("Mining error: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Mine a block with automatic tool selection.
     */
    public CompletableFuture<ActionResult> mineBlock(BlockPos pos) {
        return mineBlock(pos, false);
    }

    /**
     * Select the best tool for mining a block.
     */
    private ItemStack selectBestTool(BlockState state) {
        PlayerInventory inventory = player.getInventory();
        ItemStack bestTool = ItemStack.EMPTY;
        float bestSpeed = 1.0f;

        // Check hotbar + main inventory
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);

            if (stack.isEmpty()) {
                continue;
            }

            float speed = stack.getMiningSpeedMultiplier(state);

            // Consider durability (don't use almost broken tools)
            if (stack.isDamageable() && stack.getDamage() >= stack.getMaxDamage() - 5) {
                continue;
            }

            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestTool = stack;
            }
        }

        return bestTool.isEmpty() ? ItemStack.EMPTY : bestTool;
    }

    /**
     * Check if a tool is the correct type for a block.
     */
    private boolean isCorrectTool(ItemStack tool, BlockState state) {
        if (tool.isEmpty()) {
            // Can mine most blocks with hand, just slower
            return !state.isToolRequired();
        }

        // Check if tool is effective for this block
        return tool.isSuitableFor(state);
    }

    /**
     * Equip a tool in the main hand.
     */
    private boolean equipTool(ItemStack tool) {
        if (tool.isEmpty()) {
            return true; // Mining with hand
        }

        PlayerInventory inventory = player.getInventory();

        // Find tool in inventory
        int slot = -1;
        for (int i = 0; i < inventory.size(); i++) {
            if (inventory.getStack(i) == tool) {
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
        inventory.setStack(inventory.selectedSlot, tool);
        inventory.setStack(slot, currentItem);

        return true;
    }

    /**
     * Look at a block position.
     */
    private void lookAtBlock(BlockPos pos) {
        double dx = pos.getX() + 0.5 - player.getX();
        double dy = pos.getY() + 0.5 - player.getEyeY();
        double dz = pos.getZ() + 0.5 - player.getZ();

        double distance = Math.sqrt(dx * dx + dz * dz);

        float yaw = (float) (Math.atan2(dz, dx) * 180 / Math.PI) - 90;
        float pitch = (float) -(Math.atan2(dy, distance) * 180 / Math.PI);

        player.setYaw(yaw);
        player.setPitch(pitch);
        player.setHeadYaw(yaw);
    }

    /**
     * Calculate mining speed for a tool on a block.
     */
    private float calculateMiningSpeed(ItemStack tool, BlockState state) {
        float speed = tool.getMiningSpeedMultiplier(state);

        // Apply efficiency enchantment
        int efficiency = EnchantmentHelper.getLevel(Enchantments.EFFICIENCY, tool);
        if (efficiency > 0 && speed > 1.0f) {
            speed += efficiency * efficiency + 1;
        }

        // Apply mining speed based on player state
        // (would include potion effects, fatigue, etc.)

        return Math.max(speed, 1.0f);
    }

    /**
     * Check if currently mining.
     */
    public boolean isMining() {
        return isMining;
    }

    /**
     * Get currently mining block position.
     */
    public BlockPos getCurrentlyMining() {
        return currentlyMining;
    }

    /**
     * Cancel current mining operation.
     */
    public void cancelMining() {
        isMining = false;
        currentlyMining = null;
    }

    /**
     * Check if player can mine a specific block type.
     */
    public boolean canMine(BlockState state) {
        // Can't mine bedrock
        if (state.isIn(BlockTags.WITHER_IMMUNE)) {
            return false;
        }

        // Check if we have any tool that can mine it
        ItemStack bestTool = selectBestTool(state);

        // If block requires a tool, we must have one
        if (state.isToolRequired() && bestTool.isEmpty()) {
            return false;
        }

        return true;
    }

    /**
     * Get mining progress (0.0 to 1.0).
     */
    public float getMiningProgress() {
        if (!isMining || currentlyMining == null) {
            return 0.0f;
        }

        long elapsed = System.currentTimeMillis() - miningStartTime;
        BlockState state = player.getWorld().getBlockState(currentlyMining);
        ItemStack tool = player.getMainHandStack();

        float miningSpeed = calculateMiningSpeed(tool, state);
        float hardness = state.getHardness(player.getWorld(), currentlyMining);
        float totalTime = (hardness * 1000) / miningSpeed;

        return Math.min(elapsed / totalTime, 1.0f);
    }
}
