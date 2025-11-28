package com.aiplayer.action;

import com.aiplayer.core.AIPlayerEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Building Controller - Handles block placement for AI players.
 *
 * Features:
 * - Smart block placement with orientation
 * - Material selection from inventory
 * - Placement validation (can place here?)
 * - Support for directional blocks (stairs, doors, etc.)
 * - Safe placement (won't suffocate player)
 */
public class BuildingController {

    private static final Logger LOGGER = LoggerFactory.getLogger(BuildingController.class);

    private final AIPlayerEntity player;

    public BuildingController(AIPlayerEntity player) {
        this.player = player;
    }

    /**
     * Place a block at the given position.
     *
     * @param pos Position to place block
     * @param blockItem Block item to place
     * @param facing Direction to place from (affects block orientation)
     * @return Future that completes when placement is done
     */
    public CompletableFuture<ActionResult> placeBlock(BlockPos pos, BlockItem blockItem, Direction facing) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                World world = player.getWorld();

                // Validate position
                BlockState existingState = world.getBlockState(pos);
                if (!existingState.isReplaceable()) {
                    return ActionResult.failure("Position already occupied");
                }

                // Check distance
                double distance = player.getPos().distanceTo(pos.toCenterPos());
                if (distance > 6.0) {
                    return ActionResult.failure("Position too far away");
                }

                // Find block item in inventory
                ItemStack blockStack = findBlockInInventory(blockItem);
                if (blockStack.isEmpty()) {
                    return ActionResult.failure("Don't have block: " + blockItem.getName().getString());
                }

                // Equip block
                if (!equipBlock(blockStack)) {
                    return ActionResult.failure("Failed to equip block");
                }

                // Look at placement position
                lookAtPosition(pos, facing);

                // Create hit result for placement
                Vec3d hitPos = Vec3d.ofCenter(pos.offset(facing.getOpposite()));
                BlockHitResult hitResult = new BlockHitResult(
                    hitPos,
                    facing,
                    pos.offset(facing.getOpposite()),
                    false
                );

                // Place block
                boolean success = player.interactionManager.interactBlock(
                    player,
                    player.getWorld(),
                    player.getStackInHand(Hand.MAIN_HAND),
                    Hand.MAIN_HAND,
                    hitResult
                ).isAccepted();

                if (success) {
                    LOGGER.debug("Placed {} at {}", blockItem.getName().getString(), pos);
                    return ActionResult.success("Placed " + blockItem.getName().getString());
                } else {
                    return ActionResult.failure("Failed to place block");
                }

            } catch (Exception e) {
                LOGGER.error("Error placing block", e);
                return ActionResult.failure("Placement error: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Place a block with automatic facing detection.
     */
    public CompletableFuture<ActionResult> placeBlock(BlockPos pos, BlockItem blockItem) {
        // Determine best face to place from
        Direction facing = determineBestFacing(pos);
        return placeBlock(pos, blockItem, facing);
    }

    /**
     * Place a block by name (searches inventory).
     */
    public CompletableFuture<ActionResult> placeBlockByName(BlockPos pos, String blockName) {
        ItemStack blockStack = findBlockByName(blockName);
        if (blockStack.isEmpty()) {
            return CompletableFuture.completedFuture(
                ActionResult.failure("Don't have block: " + blockName)
            );
        }

        if (!(blockStack.getItem() instanceof BlockItem blockItem)) {
            return CompletableFuture.completedFuture(
                ActionResult.failure("Item is not a block: " + blockName)
            );
        }

        return placeBlock(pos, blockItem);
    }

    /**
     * Find a specific block item in inventory.
     */
    private ItemStack findBlockInInventory(BlockItem blockItem) {
        PlayerInventory inventory = player.getInventory();

        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.getItem() == blockItem && !stack.isEmpty()) {
                return stack;
            }
        }

        return ItemStack.EMPTY;
    }

    /**
     * Find a block by name in inventory.
     */
    private ItemStack findBlockByName(String blockName) {
        PlayerInventory inventory = player.getInventory();
        String searchName = blockName.toLowerCase();

        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);

            if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem)) {
                continue;
            }

            String itemName = stack.getItem().getName().getString().toLowerCase();
            if (itemName.contains(searchName)) {
                return stack;
            }
        }

        return ItemStack.EMPTY;
    }

    /**
     * Equip a block in the main hand.
     */
    private boolean equipBlock(ItemStack blockStack) {
        if (blockStack.isEmpty()) {
            return false;
        }

        PlayerInventory inventory = player.getInventory();

        // Find block in inventory
        int slot = -1;
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack == blockStack) {
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
        inventory.setStack(inventory.selectedSlot, blockStack);
        inventory.setStack(slot, currentItem);

        return true;
    }

    /**
     * Look at a position for block placement.
     */
    private void lookAtPosition(BlockPos pos, Direction facing) {
        // Look at the center of the face we're placing on
        Vec3d targetPos = Vec3d.ofCenter(pos.offset(facing.getOpposite()));

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
     * Determine the best facing direction for placement.
     */
    private Direction determineBestFacing(BlockPos pos) {
        Vec3d playerPos = player.getPos();
        Vec3d blockPos = Vec3d.ofCenter(pos);

        // Calculate direction from player to block
        Vec3d direction = blockPos.subtract(playerPos).normalize();

        // Determine which face the player is looking at most directly
        double maxDot = Double.NEGATIVE_INFINITY;
        Direction bestFacing = Direction.UP;

        for (Direction dir : Direction.values()) {
            Vec3d dirVec = new Vec3d(dir.getOffsetX(), dir.getOffsetY(), dir.getOffsetZ());
            double dot = direction.dotProduct(dirVec);

            if (dot > maxDot) {
                maxDot = dot;
                bestFacing = dir;
            }
        }

        return bestFacing.getOpposite(); // We want to place FROM this direction
    }

    /**
     * Check if a position is valid for placement.
     */
    public boolean canPlaceAt(BlockPos pos) {
        World world = player.getWorld();

        // Check if position is loaded
        if (!world.isChunkLoaded(pos)) {
            return false;
        }

        // Check if position is replaceable
        BlockState state = world.getBlockState(pos);
        if (!state.isReplaceable()) {
            return false;
        }

        // Check if placement would suffocate player
        BlockPos playerPos = player.getBlockPos();
        if (pos.equals(playerPos) || pos.equals(playerPos.up())) {
            return false;
        }

        // Check distance
        double distance = player.getPos().distanceTo(pos.toCenterPos());
        return distance <= 6.0;
    }

    /**
     * Check if player has a specific block type.
     */
    public boolean hasBlock(String blockName) {
        return !findBlockByName(blockName).isEmpty();
    }

    /**
     * Count how many of a specific block player has.
     */
    public int countBlocks(String blockName) {
        PlayerInventory inventory = player.getInventory();
        String searchName = blockName.toLowerCase();
        int count = 0;

        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);

            if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem)) {
                continue;
            }

            String itemName = stack.getItem().getName().getString().toLowerCase();
            if (itemName.contains(searchName)) {
                count += stack.getCount();
            }
        }

        return count;
    }
}
