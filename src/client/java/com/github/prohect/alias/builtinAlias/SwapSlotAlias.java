package com.github.prohect.alias.builtinAlias;

import com.github.prohect.BindAliasPlusClient;
import com.github.prohect.alias.Alias;
import com.github.prohect.alias.BuiltinAliasWithArgs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.regex.Pattern;

public class SwapSlotAlias extends BuiltinAliasWithArgs<SwapSlotAlias> {

    /**
     * @param args args typed by user.
     *             pattern: slot1 slot2, or slot1, spilt by white space,
     *             1-9 means hotbarSlots,
     *             10-36 means slots inside inventory,
     *             37-40 means equipments, 37 is feet, 40 is head
     *             41 means the second hand,
     */
    @Override
    public SwapSlotAlias run(String args) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        ClientPlayerEntity player = minecraftClient.player;
        if (player == null) {
            BindAliasPlusClient.LOGGER.warn("[switchSlot]Player is null");
            return this;
        }
        PlayerInventory inventory = player.getInventory();
        if (inventory == null) {
            BindAliasPlusClient.LOGGER.warn("[switchSlot]Inventory is null");
            return this;
        }
        int selectedSlot = inventory.selectedSlot;
        ClientPlayNetworkHandler networkHandler = minecraftClient.getNetworkHandler();
        if (networkHandler == null) {
            BindAliasPlusClient.LOGGER.warn("[SwitchSlot]network handler is null");
            return this;
        }

        String[] strings = args.split(Pattern.quote(String.valueOf(Alias.divider4AliasArgs)));
        int[] slots = new int[]{0, selectedSlot};
        try {
            if (strings.length == 1) slots[0] = Integer.parseInt(strings[0]) - 1;
            else if (strings.length == 2) {
                slots[0] = Integer.parseInt(strings[0]) - 1;
                slots[1] = Integer.parseInt(strings[1]) - 1;
            } else {
                BindAliasPlusClient.LOGGER.warn("[SwitchSlot]Invalid arguments:args pattern not expected");
                return this;
            }
        } catch (NumberFormatException e) {
            BindAliasPlusClient.LOGGER.warn("[SwitchSlot]Invalid arguments: cant parse number");
            return this;
        }

        if (slots[0] < 0 || slots[1] < 0 || slots[0] > 40 || slots[1] > 40 || slots[0] == slots[1]) {
            BindAliasPlusClient.LOGGER.warn("[SwitchSlot]Invalid arguments: slot index out of bounds, or slot index1 equals to slot index2");
            return this;
        }

        Screen currentScreen = minecraftClient.currentScreen;
        boolean creativeInventory = currentScreen instanceof CreativeInventoryScreen;
        boolean inInventory = currentScreen instanceof InventoryScreen || creativeInventory;
        if (creativeInventory) currentScreen.close();

        try {
            final int offhand = 40;
            boolean slot0IsOffhand = slots[0] == offhand;
            boolean hasOffHand = slots[1] == offhand || slot0IsOffhand;
            int ratherOffhand = slot0IsOffhand ? slots[1] : slots[0];

            boolean slot0IsHotbar = slots[0] < 9;
            boolean hasHotbar = slots[1] < 9 || slot0IsHotbar;
            int hotbar = slot0IsHotbar ? slots[0] : slots[1];
            int ratherHotbar = slot0IsHotbar ? slots[1] : slots[0];

            boolean insideHotbarsAndOffHand = (slot0IsHotbar || slot0IsOffhand) && (slots[1] < 9 || slots[1] == offhand);
            if (insideHotbarsAndOffHand) {
                if (hasOffHand) {
                    swapSlotOffhand(networkHandler, ratherOffhand);
                } else {
                    swapSlotOffhand(networkHandler, slots[0]);
                    swapSlotOffhand(networkHandler, slots[1]);
                    swapSlotOffhand(networkHandler, slots[0]);
                }
                networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(selectedSlot));
            } else {
                InventoryScreen inventoryScreen = inInventory ? creativeInventory ? new InventoryScreen(player) : (InventoryScreen) currentScreen : new InventoryScreen(player);
                if (!inInventory) minecraftClient.setScreen(inventoryScreen);
                if (creativeInventory) minecraftClient.setScreen(inventoryScreen);
                try {
                    ClientPlayerInteractionManager interactionManager = minecraftClient.interactionManager;
                    if (interactionManager != null) {

                        if (hasOffHand) {
                            Slot slotRatherOffhand = getSlot(inventoryScreen, ratherOffhand);
                            if (slotRatherOffhand != null)
                                clickSlot(interactionManager, inventoryScreen, slotRatherOffhand, offhand, player);
                            else BindAliasPlusClient.LOGGER.warn("[switchSlot]Slot {} is null", ratherOffhand);
                        } else if (hasHotbar) {
                            Slot slotRatherHotbar = getSlot(inventoryScreen, ratherHotbar);
                            if (slotRatherHotbar != null)
                                clickSlot(interactionManager, inventoryScreen, slotRatherHotbar, hotbar, player);
                            else BindAliasPlusClient.LOGGER.warn("[switchSlot]Slot {} is nul", ratherHotbar);
                        } else {
                            Slot slot0 = getSlot(inventoryScreen, slots[0]);
                            Slot slot1 = getSlot(inventoryScreen, slots[1]);
                            if (slot0 != null) {
                                if (slot1 != null) {
                                    clickSlot(interactionManager, inventoryScreen, slot0, offhand, player);
                                    clickSlot(interactionManager, inventoryScreen, slot1, offhand, player);
                                    clickSlot(interactionManager, inventoryScreen, slot0, offhand, player);
                                } else BindAliasPlusClient.LOGGER.warn("[SwitchSlot]slot1 {} is null", slots[1]);
                            } else BindAliasPlusClient.LOGGER.warn("[SwitchSlot]slot0 {} is null", slots[0]);
                        }
                    } else BindAliasPlusClient.LOGGER.warn("[SwitchSlot]interactionManager is null");
                } finally {
                    if (!inInventory) inventoryScreen.close();
                }
            }
        } catch (Exception e) {
            BindAliasPlusClient.LOGGER.error("[SwitchSlot]Failed to swap slots.", e);
        }

        return this;
    }

    /**
     * @param slot   the slot of an inventory of a screen, chest inventory or player inventory for example
     * @param button index of a list, could be 0,1,...,8 which means hotbars, or 40 which means hasOffHand, would be used to get a certain slot object via playerInventory.getStack(button)
     *               <p>value range check inside, only 0-8 and 40 allowed
     */
    private static void clickSlot(ClientPlayerInteractionManager interactionManager, InventoryScreen inventoryScreen, Slot slot, int button, ClientPlayerEntity player) {
        interactionManager.clickSlot(inventoryScreen.handler.syncId, slot.id, button, SlotActionType.SWAP, player);
    }

    private static void swapSlotOffhand(ClientPlayNetworkHandler networkHandler, int ratherOffhand) {
        networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(ratherOffhand));
        networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, Direction.DOWN));
    }

    private static Slot getSlot(InventoryScreen inventoryScreen, int index) {
        for (Slot slot : inventoryScreen.handler.slots) {
            if (slot.getIndex() == index && slot.inventory instanceof PlayerInventory) {
                return slot;
            }
        }
        return null;
    }
}
