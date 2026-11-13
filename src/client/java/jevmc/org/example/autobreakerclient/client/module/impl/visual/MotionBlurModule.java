package jevmc.org.example.autobreakerclient.client.module.impl.visual;

import jevmc.org.example.autobreakerclient.client.module.Category;
import jevmc.org.example.autobreakerclient.client.module.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.FrameGraphBuilder;
import net.minecraft.client.util.Handle;
import net.minecraft.client.util.memory.ObjectAllocator;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Frame-accumulation motion blur: each frame is blended with a history buffer
 * from the previous frame, giving fast camera movement a trailing smear.
 *
 * <p>{@link PostEffectProcessor#render(Framebuffer, ObjectAllocator)}'s own
 * internal "accum" target does not reliably keep its pixel contents between
 * separate {@code render()} calls across frames, so this keeps its own
 * history buffer instead and explicitly re-captures it every frame. {@link
 * #history} is a {@link SimpleFramebuffer} this class owns, imported into the
 * post-effect's frame graph as the external target {@code
 * autobreakerclient:previous} alongside vanilla's own {@code minecraft:main},
 * mirroring how {@code WorldRenderer} imports its own long-lived
 * entity-outline framebuffer the same way for its outline post-effect. The
 * pipeline (see {@code assets/autobreakerclient/post_effect/motion_blur.json})
 * writes the blended result into both {@code minecraft:main} (so it displays)
 * and {@code autobreakerclient:previous} (so next frame blends against it),
 * so no copy needs to happen back on the Java side.</p>
 *
 * <p>The one piece needing a mixin is knowing <em>when</em> to run this each
 * frame — there's no public "after the frame is fully rendered" event, so
 * {@code mixin.MotionBlurMixin} injects at the tail of
 * {@code GameRenderer#render}.</p>
 */
public class MotionBlurModule extends Module {
    private static final Logger LOGGER = LoggerFactory.getLogger("AutoBreaker");
    private static final Identifier EFFECT_ID = Identifier.of("autobreakerclient", "motion_blur");
    private static final Identifier PREVIOUS_ID = Identifier.of("autobreakerclient", "previous");
    private static final Identifier MAIN_ID = Identifier.ofVanilla("main");
    private static final Set<Identifier> EXTERNAL_TARGETS = Set.of(MAIN_ID, PREVIOUS_ID);

    private static volatile boolean enabledFlag;
    private static volatile boolean failed;
    private static Framebuffer history;

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
            Framebuffer main = client.getFramebuffer();
            ensureHistoryBuffer(main.textureWidth, main.textureHeight);

            PostEffectProcessor processor = client.getShaderLoader().loadPostEffect(EFFECT_ID, EXTERNAL_TARGETS);
            if (processor == null) {
                failed = true;
                LOGGER.warn("Motion Blur post-effect failed to load, disabling for this session");
                return;
            }

            FrameGraphBuilder builder = new FrameGraphBuilder();
            Handle<Framebuffer> mainHandle = builder.createObjectNode("autobreakerclient_main", main);
            Handle<Framebuffer> historyHandle = builder.createObjectNode("autobreakerclient_history", history);

            Map<Identifier, Handle<Framebuffer>> handles = new HashMap<>();
            handles.put(MAIN_ID, mainHandle);
            handles.put(PREVIOUS_ID, historyHandle);

            processor.render(builder, main.textureWidth, main.textureHeight, new MapFramebufferSet(handles));
            builder.run(allocator);
        } catch (Exception e) {
            failed = true;
            LOGGER.warn("Motion Blur post-effect failed at runtime, disabling for this session", e);
        }
    }

    private static void ensureHistoryBuffer(int width, int height) {
        if (history == null) {
            history = new SimpleFramebuffer("autobreakerclient_motion_blur_history", width, height, false);
        } else if (history.textureWidth != width || history.textureHeight != height) {
            history.resize(width, height);
        }
    }

    private static final class MapFramebufferSet implements PostEffectProcessor.FramebufferSet {
        private final Map<Identifier, Handle<Framebuffer>> handles;

        private MapFramebufferSet(Map<Identifier, Handle<Framebuffer>> handles) {
            this.handles = handles;
        }

        @Override
        public void set(Identifier id, Handle<Framebuffer> handle) {
            handles.put(id, handle);
        }

        @Override
        public Handle<Framebuffer> get(Identifier id) {
            return handles.get(id);
        }
    }
}
