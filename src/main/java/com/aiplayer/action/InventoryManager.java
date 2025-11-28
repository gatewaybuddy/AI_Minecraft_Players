package com.aiplayer.action;

import com.aiplayer.core.AIPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Inventory Manager - Handles inventory operations for AI players.
 *
 * Features:
 * - Item searching and counting
 * - Inventory organization (sorting, stacking)
 * - Tool/item selection
 * - Hotbar management
 * - Drop/pickup control
 */
public class InventoryManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(InventoryManager.class);

    private final AIPlayerEntity player;

    public InventoryManager(AIPlayerEntity player) {
        this.player = player;
    }

    /**
     * Find an item by name in inventory.
     *
     * @param itemName Item name (partial match, case-insensitive)
     * @return First matching item stack, or empty if not found
     */
    public Optional<ItemStack> findItem(String itemName) {
        PlayerInventory inventory = player.getInventory();
        String searchName = itemName.toLowerCase();

        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);

            if (stack.isEmpty()) {
                continue;
            }

            String stackName = stack.getItem().getName().getString().toLowerCase();
            if (stackName.contains(searchName)) {
                return Optional.of(stack);
            }
        }

        return Optional.empty();
    }

    /**
     * Find an item by exact Item type.
     */
    public Optional<ItemStack> findItem(Item item) {
        PlayerInventory inventory = player.getInventory();

        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);

            if (!stack.isEmpty() && stack.getItem() == item) {
                return Optional.of(stack);
            }
        }

        return Optional.empty();
    }

    /**
     * Count total quantity of an item.
     */
    public int countItem(String itemName) {
        PlayerInventory inventory = player.getInventory();
        String searchName = itemName.toLowerCase();
        int count = 0;

        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);

            if (stack.isEmpty()) {
                continue;
            }

            String stackName = stack.getItem().getName().getString().toLowerCase();
            if (stackName.contains(searchName)) {
                count += stack.getCount();
            }
        }

        return count;
    }

    /**
     * Count total quantity of a specific Item type.
     */
    public int countItem(Item item) {
        PlayerInventory inventory = player.getInventory();
        int count = 0;

        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);

            if (!stack.isEmpty() && stack.getItem() == item) {
                count += stack.getCount();
            }
        }

        return count;
    }

    /**
     * Check if inventory contains an item.
     */
    public boolean hasItem(String itemName) {
        return findItem(itemName).isPresent();
    }

    /**
     * Check if inventory contains a specific Item.
     */
    public boolean hasItem(Item item) {
        return findItem(item).isPresent();
    }

    /**
     * Check if inventory contains at least N of an item.
     */
    public boolean hasItemCount(String itemName, int minCount) {
        return countItem(itemName) >= minCount;
    }

    /**
     * Get number of empty slots in inventory.
     */
    public int getEmptySlots() {
        PlayerInventory inventory = player.getInventory();
        int empty = 0;

        for (int i = 0; i < inventory.size(); i++) {
            if (inventory.getStack(i).isEmpty()) {
                empty++;
            }
        }

        return empty;
    }

    /**
     * Check if inventory is full.
     */
    public boolean isFull() {
        return getEmptySlots() == 0;
    }

    /**
     * Get inventory usage percentage (0-100).
     */
    public int getUsagePercentage() {
        PlayerInventory inventory = player.getInventory();
        int total = inventory.size();
        int used = total - getEmptySlots();
        return (used * 100) / total;
    }

    /**
     * Move an item to the hotbar.
     *
     * @param stack Item stack to move
     * @param hotbarSlot Target hotbar slot (0-8)
     * @return true if successful
     */
    public boolean moveToHotbar(ItemStack stack, int hotbarSlot) {
        if (hotbarSlot < 0 || hotbarSlot >= 9) {
            return false;
        }

        PlayerInventory inventory = player.getInventory();

        // Find item in inventory
        int sourceSlot = -1;
        for (int i = 0; i < inventory.size(); i++) {
            if (inventory.getStack(i) == stack) {
                sourceSlot = i;
                break;
            }
        }

        if (sourceSlot == -1) {
            return false;
        }

        // If already in hotbar, just select it
        if (sourceSlot < 9) {
            inventory.selectedSlot = sourceSlot;
            return true;
        }

        // Swap with target hotbar slot
        ItemStack hotbarItem = inventory.getStack(hotbarSlot);
        inventory.setStack(hotbarSlot, stack);
        inventory.setStack(sourceSlot, hotbarItem);
        inventory.selectedSlot = hotbarSlot;

        return true;
    }

    /**
     * Select a hotbar slot.
     */
    public void selectHotbarSlot(int slot) {
        if (slot >= 0 && slot < 9) {
            player.getInventory().selectedSlot = slot;
        }
    }

    /**
     * Get currently selected item.
     */
    public ItemStack getSelectedItem() {
        return player.getInventory().getMainHandStack();
    }

    /**
     * Drop an item from inventory.
     *
     * @param itemName Item to drop
     * @param count Number to drop (0 = all)
     * @return true if dropped
     */
    public boolean dropItem(String itemName, int count) {
        Optional<ItemStack> itemOpt = findItem(itemName);
        if (itemOpt.isEmpty()) {
            return false;
        }

        ItemStack stack = itemOpt.get();
        PlayerInventory inventory = player.getInventory();

        // Find slot
        int slot = -1;
        for (int i = 0; i < inventory.size(); i++) {
            if (inventory.getStack(i) == stack) {
                slot = i;
                break;
            }
        }

        if (slot == -1) {
            return false;
        }

        // Drop entire stack or specific count
        if (count == 0 || count >= stack.getCount()) {
            player.dropItem(stack, false);
            inventory.setStack(slot, ItemStack.EMPTY);
        } else {
            ItemStack dropStack = stack.split(count);
            player.dropItem(dropStack, false);
        }

        return true;
    }

    /**
     * Organize inventory (stack items, sort by type).
     */
    public void organize() {
        PlayerInventory inventory = player.getInventory();

        // Phase 1: Stack similar items
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack1 = inventory.getStack(i);

            if (stack1.isEmpty() || stack1.getCount() >= stack1.getMaxCount()) {
                continue;
            }

            // Find other stacks of same item
            for (int j = i + 1; j < inventory.size(); j++) {
                ItemStack stack2 = inventory.getStack(j);

                if (stack2.isEmpty()) {
                    continue;
                }

                // Can stack together?
                if (ItemStack.canCombine(stack1, stack2)) {
                    int transfer = Math.min(
                        stack2.getCount(),
                        stack1.getMaxCount() - stack1.getCount()
                    );

                    stack1.increment(transfer);
                    stack2.decrement(transfer);

                    if (stack2.isEmpty()) {
                        inventory.setStack(j, ItemStack.EMPTY);
                    }

                    if (stack1.getCount() >= stack1.getMaxCount()) {
                        break;
                    }
                }
            }
        }

        LOGGER.debug("Organized inventory for {}", player.getName().getString());
    }

    /**
     * Get all items in inventory.
     */
    public List<ItemStack> getAllItems() {
        List<ItemStack> items = new ArrayList<>();
        PlayerInventory inventory = player.getInventory();

        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (!stack.isEmpty()) {
                items.add(stack);
            }
        }

        return items;
    }

    /**
     * Get inventory summary (for status display).
     */
    public String getSummary() {
        PlayerInventory inventory = player.getInventory();
        int totalSlots = inventory.size();
        int usedSlots = totalSlots - getEmptySlots();

        return String.format("Inventory: %d/%d slots used (%d%%)",
            usedSlots, totalSlots, getUsagePercentage());
    }

    /**
     * Get hotbar summary.
     */
    public String getHotbarSummary() {
        PlayerInventory inventory = player.getInventory();
        StringBuilder sb = new StringBuilder("Hotbar: ");

        for (int i = 0; i < 9; i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.isEmpty()) {
                sb.append("[ ] ");
            } else {
                sb.append("[").append(stack.getItem().getName().getString()).append("x").append(stack.getCount()).append("] ");
            }
        }

        return sb.toString();
    }

    /**
     * Find best tool for a specific task.
     * This is used by MiningController.
     */
    public Optional<ItemStack> findBestTool(java.util.function.Predicate<ItemStack> predicate) {
        PlayerInventory inventory = player.getInventory();

        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);

            if (!stack.isEmpty() && predicate.test(stack)) {
                return Optional.of(stack);
            }
        }

        return Optional.empty();
    }
}
