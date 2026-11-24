package jevmc.org.example.autobreakerclient.client.module.impl.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.effect.StatusEffectInstance;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Active potion effects as a compact text list, the way HUD mods show them -
 * readable at a glance, unlike the vanilla icon row which hides the remaining
 * time until you open your inventory.
 */
public class PotionEffectsModule extends HudModule {
    private static final int BENEFICIAL = 0xFF7FD38A;
    private static final int HARMFUL = 0xFFE0736B;
    /** Flash the last few seconds so an expiring effect is obvious. */
    private static final int EXPIRING_TICKS = 100;
    private static final int INFINITE_THRESHOLD = 32767;

    public PotionEffectsModule() {
        super("Potion Effects", "Lists active effects with their remaining time.");
    }

    @Override
    protected RenderResult renderHud(DrawContext context, int x, int y) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return new RenderResult(0, 0);
        }

        List<StatusEffectInstance> effects = new ArrayList<>(client.player.getStatusEffects());
        if (effects.isEmpty()) {
            return new RenderResult(0, 0);
        }
        // Longest first, so an effect about to run out does not make the rest
        // of the list jump around as it disappears.
        effects.sort(Comparator.comparingInt(StatusEffectInstance::getDuration).reversed());

        int widest = 0;
        int lineHeight = client.textRenderer.fontHeight + 1;
        for (int i = 0; i < effects.size(); i++) {
            StatusEffectInstance effect = effects.get(i);
            String text = label(effect);
            int color = effect.getEffectType().value().isBeneficial() ? BENEFICIAL : HARMFUL;
            if (effect.getDuration() < EXPIRING_TICKS && (System.currentTimeMillis() / 300) % 2 == 0) {
                color = 0xFFFFFFFF;
            }
            context.drawTextWithShadow(client.textRenderer, text, x, y + i * lineHeight, color);
            widest = Math.max(widest, client.textRenderer.getWidth(text));
        }
        return new RenderResult(widest, effects.size() * lineHeight);
    }

    private String label(StatusEffectInstance effect) {
        String name = effect.getEffectType().value().getName().getString();
        int amplifier = effect.getAmplifier();
        if (amplifier > 0) {
            name += " " + roman(amplifier + 1);
        }
        if (effect.getDuration() >= INFINITE_THRESHOLD) {
            return name + " **";
        }
        int seconds = effect.getDuration() / 20;
        return String.format("%s %d:%02d", name, seconds / 60, seconds % 60);
    }

    private String roman(int level) {
        return switch (level) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> String.valueOf(level);
        };
    }
}
