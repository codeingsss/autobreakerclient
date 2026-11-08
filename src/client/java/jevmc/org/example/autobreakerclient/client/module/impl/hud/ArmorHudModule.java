package jevmc.org.example.autobreakerclient.client.module.impl.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

/** Ported from CoralMod (CC0-1.0, github.com/jirmjahu/coralmod). */
public class ArmorHudModule extends HudModule {
    private static final EquipmentSlot[] SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };
    private static final int ITEM_SIZE = 16;
    private static final int ITEM_SPACING = 2;

    public ArmorHudModule() {
        super("Armor HUD", "Shows your equipped armor with durability.");
    }

    @Override
    protected RenderResult renderHud(DrawContext context, int x, int y) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null) {
            return new RenderResult(0, 0);
        }

        int rowY = y;
        int rows = 0;
        int maxTextWidth = 0;
        for (EquipmentSlot slot : SLOTS) {
            ItemStack stack = player.getEquippedStack(slot);
            if (stack.isEmpty()) {
                continue;
            }
            context.drawItem(stack, x, rowY);
            int maxDamage = stack.getMaxDamage();
            if (maxDamage > 0) {
                int durability = maxDamage - stack.getDamage();
                String text = durability + "/" + maxDamage;
                context.drawTextWithShadow(client.textRenderer, text, x + ITEM_SIZE + 4,
                        rowY + (ITEM_SIZE - client.textRenderer.fontHeight) / 2, 0xFFAAAAAA);
                maxTextWidth = Math.max(maxTextWidth, client.textRenderer.getWidth(text));
            }
            rowY += ITEM_SIZE + ITEM_SPACING;
            rows++;
        }
        if (rows == 0) {
            return new RenderResult(0, 0);
        }
        int width = ITEM_SIZE + (maxTextWidth > 0 ? ITEM_SIZE / 4 + maxTextWidth : 0);
        int height = rowY - y - ITEM_SPACING;
        return new RenderResult(width, height);
    }
}
