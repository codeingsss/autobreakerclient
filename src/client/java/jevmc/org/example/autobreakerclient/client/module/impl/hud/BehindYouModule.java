package jevmc.org.example.autobreakerclient.client.module.impl.hud;

import jevmc.org.example.autobreakerclient.client.module.setting.IntSetting;
import jevmc.org.example.autobreakerclient.client.module.setting.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * Warns when another player is behind you - the BehindYou idea, done as a HUD
 * element rather than a jump-scare.
 *
 * <p>"Behind" is measured as the angle between where you are looking and the
 * direction to them, so it follows your camera rather than your position, and
 * a player you have simply turned your back on counts immediately.</p>
 */
public class BehindYouModule extends HudModule {
    /** Degrees away from straight ahead before someone counts as behind you. */
    private static final double BEHIND_ANGLE = 110.0;
    private static final int WARNING = 0xFFE0736B;

    private final IntSetting range = new IntSetting("Range (blocks)", 12, 3, 48);

    public BehindYouModule() {
        super("Behind You", "Warns when another player is behind you.");
    }

    @Override
    public List<Setting<?>> getSettings() {
        return List.of(range);
    }

    @Override
    protected RenderResult renderHud(DrawContext context, int x, int y) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity self = client.player;
        if (self == null || client.world == null) {
            return new RenderResult(0, 0);
        }

        AbstractClientPlayerEntity closest = null;
        double closestDistance = Double.MAX_VALUE;
        List<AbstractClientPlayerEntity> players = client.world.getPlayers();
        for (AbstractClientPlayerEntity other : players) {
            if (other == self || other.isSpectator()) {
                continue;
            }
            double distance = other.distanceTo(self);
            if (distance > range.getValue() || distance >= closestDistance) {
                continue;
            }
            if (isBehind(self, other)) {
                closest = other;
                closestDistance = distance;
            }
        }

        if (closest == null) {
            return new RenderResult(0, 0);
        }

        String text = "! " + closest.getName().getString() + " behind you (" + (int) closestDistance + "m)";
        context.drawTextWithShadow(client.textRenderer, text, x, y, WARNING);
        return new RenderResult(client.textRenderer.getWidth(text), client.textRenderer.fontHeight);
    }

    private boolean isBehind(ClientPlayerEntity self, AbstractClientPlayerEntity other) {
        Vec3d toOther = other.getEntityPos().subtract(self.getEntityPos());
        if (toOther.horizontalLengthSquared() < 1.0E-4) {
            return false;
        }
        // Minecraft yaw is 0 facing south and grows clockwise, so the heading
        // towards a point is atan2(-dx, dz) in the same frame.
        double bearing = Math.toDegrees(Math.atan2(-toOther.x, toOther.z));
        double difference = Math.abs(MathHelper.wrapDegrees(bearing - self.getYaw()));
        return difference > BEHIND_ANGLE;
    }
}
