package jevmc.org.example.autobreakerclient.mixin.client;

import jevmc.org.example.autobreakerclient.client.module.impl.visual.MotionBlurModule;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.memory.ObjectPool;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * There is no public "frame fully rendered" event to hook the motion blur
 * post-effect from, so this injects at the tail of GameRenderer's own render
 * method instead — see {@link MotionBlurModule} for why everything else that
 * effect needs is reached through public API.
 */
@Mixin(GameRenderer.class)
public class MotionBlurMixin {
    @Shadow
    @Final
    private ObjectPool pool;

    @Inject(method = "render", at = @At("TAIL"))
    private void autobreakerclient$applyMotionBlur(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci) {
        MotionBlurModule.apply(this.pool);
    }
}
