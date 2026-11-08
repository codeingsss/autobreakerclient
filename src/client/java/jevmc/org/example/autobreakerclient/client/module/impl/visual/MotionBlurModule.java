package jevmc.org.example.autobreakerclient.client.module.impl.visual;

import jevmc.org.example.autobreakerclient.client.module.Category;
import jevmc.org.example.autobreakerclient.client.module.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.util.memory.ObjectAllocator;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * Frame-accumulation motion blur: each frame is blended with an accumulation
 * buffer from the previous frame, giving fast camera movement a trailing
 * smear. Ported from the open-source
 * {@code motionblur-fabric-1.21.11} mod (GPL-3.0,
 * github.com/7avyy/motionblur-fabric-1.21.11, originally by wavy/Noryea),
 * but reimplemented through public API only: that mod reads/writes its
 * shader's internal passes through three accessor mixins and a reflective
 * {@code createBuffer} call to give the blend amount a live, draggable
 * slider; this version instead bakes a single fixed blend strength into the
 * post-effect pipeline JSON (see {@code assets/autobreakerclient/post_effect
 * /motion_blur.json}), which only needs {@link MinecraftClient#getShaderLoader()}
 * and {@link PostEffectProcessor#render}, both public.
 *
 * <p>The one piece that still needs a mixin is knowing <em>when</em> to run
 * the effect each frame — there is no public "after the frame is fully
 * rendered" event, so {@code mixin.MotionBlurMixin} injects at the tail of
 * {@code GameRenderer#render}, mirroring where the reference mod hooks in.</p>
 *
 * <p>This is the one feature in AutoBreaker that reaches past public,
 * javap-verified API into Minecraft's post-processing pipeline. Everything
 * else in this project was checked against the real Yarn-mapped classes
 * before being written; whether a {@link PostEffectProcessor}'s internal
 * framebuffers actually keep their pixel contents between separate
 * {@code render()} calls — which is what makes the accumulation trick work
 * at all — can only be confirmed by actually running the game.</p>
 */
public class MotionBlurModule extends Module {
    private static final Logger LOGGER = LoggerFactory.getLogger("AutoBreaker");
    private static final Identifier EFFECT_ID = Identifier.of("autobreakerclient", "motion_blur");

    private static volatile boolean enabledFlag;
    private static volatile boolean failed;

    public MotionBlurModule() {
        super("Motion Blur", "Blends recent frames together for a trailing blur on fast camera movement.", Category.VISUAL, false);
    }

    @Override
    protected void onEnable() {
        failed = false;
        enabledFlag = true;
    }

    @Override
    protected void onDisable() {
        enabledFlag = false;
    }

    /** Called from {@code MotionBlurMixin} at the tail of every GameRenderer frame. */
    public static void apply(ObjectAllocator allocator) {
        if (!enabledFlag || failed) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return;
        }
        try {
            PostEffectProcessor processor = client.getShaderLoader()
                    .loadPostEffect(EFFECT_ID, Set.of(Identifier.ofVanilla("main")));
            if (processor == null) {
                failed = true;
                LOGGER.warn("Motion Blur post-effect failed to load, disabling for this session");
                return;
            }
            processor.render(client.getFramebuffer(), allocator);
        } catch (Exception e) {
            failed = true;
            LOGGER.warn("Motion Blur post-effect failed at runtime, disabling for this session", e);
        }
    }
}
