package com.github.prohect.alias.builtinAlias;

import com.github.prohect.BindAliasPlusClient;
import com.github.prohect.alias.BuiltinAliasWithArgs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;

public class SlotAlias extends BuiltinAliasWithArgs<SlotAlias> {
    /**
     * @param args from 1-9
     */
    @SuppressWarnings("DataFlowIssue")
    @Override
    public SlotAlias run(String args) {
        try {
            int i = Integer.parseInt(args);
            if (!(1 <= i && i <= 9)) {
                BindAliasPlusClient.LOGGER.warn("[Slot]Invalid input! Please enter a number between 1 and 9");
                return this;
            }

/*            KeyBinding hotbarKey = MinecraftClient.getInstance().options.hotbarKeys[i - 1];
            hotbarKey.setPressed(true);
            hotbarKey.setPressed(false);
            KeyBinding.onKeyPressed(hotbarKey.boundKey);*/
            MinecraftClient minecraftClient = MinecraftClient.getInstance();
            ClientPlayerEntity player = minecraftClient.player;
            if (player == null) {
                BindAliasPlusClient.LOGGER.warn("[Slot]Player is null");
                return this;
            }
            PlayerInventory inventory = player.getInventory();
            if (inventory == null) {
                BindAliasPlusClient.LOGGER.warn("[Slot]Inventory is null");
                return this;
            }
            inventory.selectedSlot = (i - 1);
            try {
                minecraftClient.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(i - 1));
            } catch (Exception e) {
                BindAliasPlusClient.LOGGER.error("[Slot]Failed to update selected slot.", e);
            }
        } catch (NumberFormatException e) {
            BindAliasPlusClient.LOGGER.warn("[Slot]Invalid arguments for slot alias");
        }
        return this;
    }
}
