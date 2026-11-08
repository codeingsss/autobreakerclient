package jevmc.org.example.autobreakerclient.client.gui;

import jevmc.org.example.autobreakerclient.client.module.Category;
import jevmc.org.example.autobreakerclient.client.module.Module;
import jevmc.org.example.autobreakerclient.client.module.ModuleManager;
import jevmc.org.example.autobreakerclient.client.module.setting.IntSetting;
import jevmc.org.example.autobreakerclient.client.module.setting.Setting;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * A single centered, frosted-glass click GUI: the game world behind it is blurred with
 * the vanilla {@link DrawContext#applyBlur()} post effect, and the panel is drawn with
 * rounded corners via {@link RoundedRect}. No shaders, textures or mixins required.
 */
public class ClickGuiScreen extends Screen {
    private static final int PANEL_WIDTH = 240;
    private static final int HEADER_HEIGHT = 24;
    private static final int PADDING = 8;
    private static final int ROW_HEIGHT = 18;
    private static final int ROW_SPACING = 4;
    private static final int SLIDER_HEIGHT = 14;
    private static final int CATEGORY_LABEL_HEIGHT = 14;
    private static final int CATEGORY_SPACING = 6;
    private static final int SCREEN_MARGIN = 40;
    private static final int SCROLL_STEP = 16;

    private static final int COLOR_HEADER_BG = 0xCC14141A;
    private static final int COLOR_PANEL_BG = 0xB2101014;
    private static final int COLOR_ROW_IDLE = 0x30000000;
    private static final int COLOR_ROW_HOVER = 0x40FFFFFF;
    private static final int COLOR_ROW_ENABLED = 0xFF3C8CFF;
    private static final int COLOR_SLIDER_TRACK = 0x30000000;
    private static final int COLOR_SLIDER_FILL = 0xFF3C8CFF;
    private static final int COLOR_ACCENT_TEXT = 0xFF55C1FF;
    private static final int COLOR_SCROLLBAR = 0x80FFFFFF;

    private final List<RowHit> moduleRows = new ArrayList<>();
    private final List<SliderHit> sliderRows = new ArrayList<>();
    private final List<CategoryHeaderHit> categoryHeaders = new ArrayList<>();

    private int panelX;
    private int panelY;
    private int panelHeight;
    private int scrollOffset;
    private boolean centered;
    private boolean draggingPanel;
    private double dragOffsetX;
    private double dragOffsetY;
    private IntSetting draggingSlider;

    public ClickGuiScreen() {
        super(Text.literal("AutoBreaker"));
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        ModuleManager.saveConfig();
        super.close();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!centered) {
            panelX = (this.width - PANEL_WIDTH) / 2;
            panelY = (this.height - measureHeight()) / 2;
            centered = true;
        }
        panelX = Math.max(0, Math.min(panelX, Math.max(0, this.width - PANEL_WIDTH)));
        panelY = Math.max(0, Math.min(panelY, Math.max(0, this.height - HEADER_HEIGHT)));

        tryApplyBlur(context);

        int naturalHeight = measureHeight();
        int maxPanelHeight = Math.max(HEADER_HEIGHT + ROW_HEIGHT + PADDING * 2, this.height - SCREEN_MARGIN * 2);
        panelHeight = Math.min(naturalHeight, maxPanelHeight);
        int maxScroll = Math.max(0, naturalHeight - panelHeight);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

        moduleRows.clear();
        sliderRows.clear();
        categoryHeaders.clear();
        layout(scrollOffset);

        RoundedRect.fill(context, panelX, panelY, PANEL_WIDTH, HEADER_HEIGHT, 10, COLOR_HEADER_BG);
        RoundedRect.fill(context, panelX, panelY + HEADER_HEIGHT, PANEL_WIDTH, panelHeight - HEADER_HEIGHT, 10, COLOR_PANEL_BG);

        context.drawCenteredTextWithShadow(textRenderer, getTitle(), panelX + PANEL_WIDTH / 2,
                panelY + (HEADER_HEIGHT - textRenderer.fontHeight) / 2, 0xFFFFFFFF);

        int contentTop = panelY + HEADER_HEIGHT;
        int contentBottom = panelY + panelHeight;
        context.enableScissor(panelX, contentTop, panelX + PANEL_WIDTH, contentBottom);

        for (CategoryHeaderHit header : categoryHeaders) {
            context.drawTextWithShadow(textRenderer, header.category.getDisplayName(), header.x, header.y, COLOR_ACCENT_TEXT);
        }

        for (RowHit hit : moduleRows) {
            boolean enabled = hit.module.isEnabled();
            boolean hovered = contains(hit.x, hit.y, hit.w, hit.h, mouseX, mouseY) && withinContent(mouseY, contentTop, contentBottom);
            int bg = enabled ? COLOR_ROW_ENABLED : hovered ? COLOR_ROW_HOVER : COLOR_ROW_IDLE;
            RoundedRect.fill(context, hit.x, hit.y, hit.w, hit.h, 6, bg);
            context.drawTextWithShadow(textRenderer, hit.module.getName(), hit.x + 6,
                    hit.y + (hit.h - textRenderer.fontHeight) / 2, 0xFFFFFFFF);
        }

        for (SliderHit hit : sliderRows) {
            RoundedRect.fill(context, hit.x, hit.y, hit.w, hit.h, 4, COLOR_SLIDER_TRACK);
            int fillWidth = (int) Math.round(hit.w * hit.setting.getFraction());
            if (fillWidth > 0) {
                RoundedRect.fill(context, hit.x, hit.y, fillWidth, hit.h, 4, COLOR_SLIDER_FILL);
            }
            String label = hit.setting.getName() + ": " + hit.setting.getValue();
            context.drawTextWithShadow(textRenderer, label, hit.x + 4,
                    hit.y + (hit.h - textRenderer.fontHeight) / 2, 0xFFFFFFFF);
        }

        context.disableScissor();

        if (maxScroll > 0) {
            int trackHeight = panelHeight - HEADER_HEIGHT;
            int thumbHeight = Math.max(12, trackHeight * panelHeight / naturalHeight);
            int thumbY = contentTop + (trackHeight - thumbHeight) * scrollOffset / maxScroll;
            context.fill(panelX + PANEL_WIDTH - 4, thumbY, panelX + PANEL_WIDTH - 1, thumbY + thumbHeight, COLOR_SCROLLBAR);
        }
    }

    private int measureHeight() {
        moduleRows.clear();
        sliderRows.clear();
        categoryHeaders.clear();
        return layout(0);
    }

    private int layout(int scrollOffsetPx) {
        int x = panelX;
        int y = panelY + HEADER_HEIGHT + PADDING - scrollOffsetPx;
        for (Category category : Category.values()) {
            List<Module> modules = ModuleManager.getModules(category);
            if (modules.isEmpty()) {
                continue;
            }
            categoryHeaders.add(new CategoryHeaderHit(category, x + PADDING, y));
            y += CATEGORY_LABEL_HEIGHT;
            for (Module module : modules) {
                moduleRows.add(new RowHit(module, x + PADDING, y, PANEL_WIDTH - PADDING * 2, ROW_HEIGHT));
                y += ROW_HEIGHT + ROW_SPACING;
                if (module.isEnabled()) {
                    for (Setting<?> setting : module.getSettings()) {
                        if (setting instanceof IntSetting intSetting) {
                            sliderRows.add(new SliderHit(module, intSetting, x + PADDING, y, PANEL_WIDTH - PADDING * 2, SLIDER_HEIGHT));
                            y += SLIDER_HEIGHT + ROW_SPACING;
                        }
                    }
                }
            }
            y += CATEGORY_SPACING;
        }
        return y - panelY + scrollOffsetPx + PADDING;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubleClick) {
        if (click.button() != 0) {
            return false;
        }
        double mx = click.x();
        double my = click.y();

        if (contains(panelX, panelY, PANEL_WIDTH, HEADER_HEIGHT, mx, my)) {
            draggingPanel = true;
            dragOffsetX = mx - panelX;
            dragOffsetY = my - panelY;
            return true;
        }

        if (!withinContent(my, panelY + HEADER_HEIGHT, panelY + panelHeight)) {
            return false;
        }

        for (SliderHit hit : sliderRows) {
            if (contains(hit.x, hit.y, hit.w, hit.h, mx, my)) {
                draggingSlider = hit.setting;
                updateSlider(hit, mx);
                return true;
            }
        }

        for (RowHit hit : moduleRows) {
            if (contains(hit.x, hit.y, hit.w, hit.h, mx, my)) {
                hit.module.toggle();
                ModuleManager.saveConfig();
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (draggingPanel) {
            panelX = (int) (click.x() - dragOffsetX);
            panelY = (int) (click.y() - dragOffsetY);
            return true;
        }
        if (draggingSlider != null) {
            for (SliderHit hit : sliderRows) {
                if (hit.setting == draggingSlider) {
                    updateSlider(hit, click.x());
                    break;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(Click click) {
        boolean handled = draggingPanel || draggingSlider != null;
        draggingPanel = false;
        if (draggingSlider != null) {
            draggingSlider = null;
            ModuleManager.saveConfig();
        }
        return handled;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!contains(panelX, panelY, PANEL_WIDTH, panelHeight, mouseX, mouseY)) {
            return false;
        }
        scrollOffset -= (int) (verticalAmount * SCROLL_STEP);
        return true;
    }

    private static boolean withinContent(double y, int contentTop, int contentBottom) {
        return y >= contentTop && y <= contentBottom;
    }

    private static void tryApplyBlur(DrawContext context) {
        try {
            context.applyBlur();
        } catch (IllegalStateException alreadyBlurred) {
            // Minecraft itself applies a blur once per frame when a screen is opened
            // (Screen#renderBackground, gated by the vanilla "Menu Background
            // Blurriness" option) and can do so immediately on open, before our own
            // render() call. The effect is already in place either way, so ignore it.
        }
    }

    private static void updateSlider(SliderHit hit, double mouseX) {
        double fraction = (mouseX - hit.x) / hit.w;
        hit.setting.setFraction(Math.max(0, Math.min(1, fraction)));
    }

    private static boolean contains(int x, int y, int w, int h, double px, double py) {
        return px >= x && px < x + w && py >= y && py < y + h;
    }

    private record RowHit(Module module, int x, int y, int w, int h) {
    }

    private record SliderHit(Module module, IntSetting setting, int x, int y, int w, int h) {
    }

    private record CategoryHeaderHit(Category category, int x, int y) {
    }
}
