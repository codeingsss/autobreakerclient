package jevmc.org.example.autobreakerclient.client.module.impl.qol;

import jevmc.org.example.autobreakerclient.client.module.Category;
import jevmc.org.example.autobreakerclient.client.module.Module;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;

import java.util.List;

/** Sends "gg" in chat shortly after the server announces a round/game has ended. */
public class AutoGgModule extends Module {
    private static final List<String> TRIGGERS = List.of(
            "has won the game",
            "won the game!",
            "Winner:",
            "Winning Team",
            "Statistics of this game",
            "-= Statistics of this game =-"
    );
    private static final long DELAY_MS = 1000;

    private long sendAtMillis = -1;

    public AutoGgModule() {
        super("Auto GG", "Sends \"gg\" in chat when the server announces a round has ended.", Category.QOL, false);

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!isEnabled() || overlay) {
                return;
            }
            String text = message.getString();
            if (TRIGGERS.stream().anyMatch(text::contains)) {
                sendAtMillis = System.currentTimeMillis() + DELAY_MS;
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!isEnabled() || sendAtMillis < 0) {
                return;
            }
            if (System.currentTimeMillis() >= sendAtMillis) {
                sendAtMillis = -1;
                if (client.player != null && client.getNetworkHandler() != null) {
                    client.getNetworkHandler().sendChatMessage("gg");
                }
            }
        });
    }
}
