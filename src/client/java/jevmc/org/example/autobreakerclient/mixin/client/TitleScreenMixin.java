package jevmc.org.example.autobreakerclient.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * There's no public event for "a screen just finished rendering" either, so
 * this injects at the tail of {@link TitleScreen#render} the same way
 * {@link MotionBlurMixin} does for {@code GameRenderer#render} - drawing an
 * AutoBreaker watermark and a faint orange wash over vanilla's own title
 * screen without touching its panorama, buttons or layout at all.
 *
 * <p>{@code width}/{@code height} are declared on {@link Screen}, not on
 * {@link TitleScreen} itself, and Mixin's {@code @Shadow} only resolves
 * fields actually present in the literal target class - not inherited ones
 * (confirmed by an {@code InvalidMixinException} doing exactly that). Both
 * fields are public on {@code Screen} though, so a plain cast reaches them
 * through ordinary Java inheritance instead.</p>
 */
@Mixin(TitleScreen.class)
public class TitleScreenMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void autobreakerclient$brandTitleScreen(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        Screen self = (Screen) (Object) this;
        context.fill(0, 0, self.width, self.height, 0x14FF8A33);

        MinecraftClient client = MinecraftClient.getInstance();
        Text text = Text.literal("AutoBreaker");
        int textWidth = client.textRenderer.getWidth(text);
        context.drawTextWithShadow(client.textRenderer, text, self.width - textWidth - 6, self.height - client.textRenderer.fontHeight - 6, 0xFFFF8A33);
    }
}
