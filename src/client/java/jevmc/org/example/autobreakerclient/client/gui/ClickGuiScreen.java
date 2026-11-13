package jevmc.org.example.autobreakerclient.client.gui;

import jevmc.org.example.autobreakerclient.client.module.Category;
import jevmc.org.example.autobreakerclient.client.module.Module;
import jevmc.org.example.autobreakerclient.client.module.ModuleManager;
import jevmc.org.example.autobreakerclient.client.module.setting.IntSetting;
import jevmc.org.example.autobreakerclient.client.module.setting.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * A fixed, centered settings window: a rectangular panel with a left-hand
 * category sidebar and a scrollable content list on the right, instead of a
 * single draggable floating box. The background is blurred with the vanilla
 * {@link DrawContext#applyBlur()} post effect and every panel uses rounded
 * corners via {@link RoundedRect} — no shaders, textures or mixins required.
 */
public class ClickGuiScreen extends Screen {
    private static final int WINDOW_WIDTH = 460;
    private static final int WINDOW_HEIGHT = 380;
    private static final int HEADER_HEIGHT = 32;
    private static final int SIDEBAR_WIDTH = 130;
    private static final int TAB_HEIGHT = 26;
    private static final int TAB_SPACING = 4;
    private static final int CONTENT_PADDING = 10;
    private static final int ROW_HEIGHT = 20;
    private static final int ROW_SPACING = 4;
    private static final int SLIDER_HEIGHT = 14;
    private static final int CLOSE_BUTTON_SIZE = 16;
    private static final int SCROLL_STEP = 16;

    private static final int COLOR_HEADER_BG = 0xE614141A;
    private static final int COLOR_SIDEBAR_BG = 0xE60D0D12;
    private static final int COLOR_CONTENT_BG = 0xE6101016;
    private static final int COLOR_TAB_IDLE = 0x00000000;
    private static final int COLOR_TAB_HOVER = 0x30FFFFFF;
    private static final int COLOR_TAB_SELECTED = 0xFFFF8A33;
    private static final int COLOR_ROW_IDLE = 0x30000000;
    private static final int COLOR_ROW_HOVER = 0x40FFFFFF;
    private static final int COLOR_ROW_ENABLED = 0xFFFF8A33;
    private static final int COLOR_SLIDER_TRACK = 0x30000000;
    private static final int COLOR_SLIDER_FILL = 0xFFFF8A33;
    private static final int COLOR_CLOSE_IDLE = 0x20FFFFFF;
    private static final int COLOR_CLOSE_HOVER = 0xFFE64C4C;
    private static final int COLOR_SCROLLBAR = 0x80FFFFFF;
    private static final int COLOR_ACCENT_TEXT = 0xFFFFB366;
    private static final int COLOR_BORDER = 0xFFFF8A33;
    private static final int BORDER_THICKNESS = 2;

    private final List<TabHit> tabHits = new ArrayList<>();
    private final List<RowHit> moduleRows = new ArrayList<>();
    private final List<SliderHit> sliderRows = new ArrayList<>();
    private ButtonHit editHudButton;

    private int panelX;
    private int panelY;
    private boolean positioned;
    private Category selectedCategory = Category.values()[0];
    private int scrollOffset;
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
        if (!positioned) {
            panelX = (this.width - WINDOW_WIDTH) / 2;
            panelY = (this.height - WINDOW_HEIGHT) / 2;
            positioned = true;
        }

        tryApplyBlur(context);

        int contentX = panelX + SIDEBAR_WIDTH;
        int contentTop = panelY + HEADER_HEIGHT;
        int contentBottom = panelY + WINDOW_HEIGHT;
        int contentWidth = WINDOW_WIDTH - SIDEBAR_WIDTH;

        // A single orange backdrop drawn first, slightly larger than the three panels drawn
        // on top of it, leaves a persistent border around the whole window - including a
        // thin ring at the seams between header/sidebar/content - so the brand reads even
        // when every module is toggled off.
        RoundedRect.fill(context, panelX - BORDER_THICKNESS, panelY - BORDER_THICKNESS,
                WINDOW_WIDTH + BORDER_THICKNESS * 2, WINDOW_HEIGHT + BORDER_THICKNESS * 2, 12, COLOR_BORDER);

        RoundedRect.fill(context, panelX, panelY, WINDOW_WIDTH, HEADER_HEIGHT, 10, COLOR_HEADER_BG);
        RoundedRect.fill(context, panelX, contentTop, SIDEBAR_WIDTH, WINDOW_HEIGHT - HEADER_HEIGHT, 10, COLOR_SIDEBAR_BG);
        RoundedRect.fill(context, contentX, contentTop, contentWidth, WINDOW_HEIGHT - HEADER_HEIGHT, 10, COLOR_CONTENT_BG);

        context.drawCenteredTextWithShadow(textRenderer, getTitle(), panelX + WINDOW_WIDTH / 2,
                panelY + (HEADER_HEIGHT - textRenderer.fontHeight) / 2, 0xFFFFFFFF);

        int closeX = panelX + WINDOW_WIDTH - CONTENT_PADDING - CLOSE_BUTTON_SIZE;
        int closeY = panelY + (HEADER_HEIGHT - CLOSE_BUTTON_SIZE) / 2;
        boolean closeHovered = contains(closeX, closeY, CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE, mouseX, mouseY);
        RoundedRect.fill(context, closeX, closeY, CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE, 4, closeHovered ? COLOR_CLOSE_HOVER : COLOR_CLOSE_IDLE);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("x"), closeX + CLOSE_BUTTON_SIZE / 2,
                closeY + (CLOSE_BUTTON_SIZE - textRenderer.fontHeight) / 2, 0xFFFFFFFF);

        layoutTabs();
        for (TabHit tab : tabHits) {
            boolean selected = tab.category == selectedCategory;
            boolean hovered = contains(tab.x, tab.y, tab.w, tab.h, mouseX, mouseY);
            int bg = selected ? COLOR_TAB_SELECTED : hovered ? COLOR_TAB_HOVER : COLOR_TAB_IDLE;
            if (bg != COLOR_TAB_IDLE) {
                RoundedRect.fill(context, tab.x, tab.y, tab.w, tab.h, 6, bg);
            }
            context.drawTextWithShadow(textRenderer, tab.category.getDisplayName(), tab.x + 10,
                    tab.y + (tab.h - textRenderer.fontHeight) / 2, 0xFFFFFFFF);
        }

        int naturalHeight = layoutModules(contentX, contentTop, contentWidth, 0);
        int visibleHeight = WINDOW_HEIGHT - HEADER_HEIGHT - CONTENT_PADDING * 2;
        int maxScroll = Math.max(0, naturalHeight - visibleHeight);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

        moduleRows.clear();
        sliderRows.clear();
        layoutModules(contentX, contentTop, contentWidth, scrollOffset);

        context.enableScissor(contentX, contentTop, contentX + contentWidth, contentBottom);
        if (editHudButton != null) {
            boolean hovered = contains(editHudButton.x(), editHudButton.y(), editHudButton.w(), editHudButton.h(), mouseX, mouseY)
                    && withinContent(mouseY, contentTop, contentBottom);
            RoundedRect.fill(context, editHudButton.x(), editHudButton.y(), editHudButton.w(), editHudButton.h(), 6,
                    hovered ? COLOR_TAB_HOVER : COLOR_ROW_IDLE);
            context.drawTextWithShadow(textRenderer, "Edit HUD Layout...", editHudButton.x() + 8,
                    editHudButton.y() + (editHudButton.h() - textRenderer.fontHeight) / 2, COLOR_ACCENT_TEXT);
        }
        for (RowHit hit : moduleRows) {
            boolean enabled = hit.module.isEnabled();
            boolean hovered = contains(hit.x, hit.y, hit.w, hit.h, mouseX, mouseY) && withinContent(mouseY, contentTop, contentBottom);
            int bg = enabled ? COLOR_ROW_ENABLED : hovered ? COLOR_ROW_HOVER : COLOR_ROW_IDLE;
            RoundedRect.fill(context, hit.x, hit.y, hit.w, hit.h, 6, bg);
            context.drawTextWithShadow(textRenderer, hit.module.getName(), hit.x + 8,
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
            int thumbHeight = Math.max(12, visibleHeight * visibleHeight / naturalHeight);
            int thumbY = contentTop + (visibleHeight - thumbHeight) * scrollOffset / maxScroll;
            context.fill(contentX + contentWidth - 4, thumbY, contentX + contentWidth - 1, thumbY + thumbHeight, COLOR_SCROLLBAR);
        }
    }

    private void layoutTabs() {
        tabHits.clear();
        int x = panelX + TAB_SPACING;
        int y = panelY + HEADER_HEIGHT + TAB_SPACING;
        int w = SIDEBAR_WIDTH - TAB_SPACING * 2;
        for (Category category : Category.values()) {
            if (ModuleManager.getModules(category).isEmpty()) {
                continue;
            }
            tabHits.add(new TabHit(category, x, y, w, TAB_HEIGHT));
            y += TAB_HEIGHT + TAB_SPACING;
        }
    }

    private int layoutModules(int contentX, int contentTop, int contentWidth, int scrollOffsetPx) {
        int rowX = contentX + CONTENT_PADDING;
        int rowWidth = contentWidth - CONTENT_PADDING * 2;
        int y = contentTop + CONTENT_PADDING - scrollOffsetPx;

        editHudButton = null;
        if (selectedCategory == Category.HUD) {
            editHudButton = new ButtonHit(rowX, y, rowWidth, ROW_HEIGHT);
            y += ROW_HEIGHT + ROW_SPACING;
        }

        for (Module module : ModuleManager.getModules(selectedCategory)) {
            moduleRows.add(new RowHit(module, rowX, y, rowWidth, ROW_HEIGHT));
            y += ROW_HEIGHT + ROW_SPACING;
            if (module.isEnabled()) {
                for (Setting<?> setting : module.getSettings()) {
                    if (setting instanceof IntSetting intSetting) {
                        sliderRows.add(new SliderHit(module, intSetting, rowX, y, rowWidth, SLIDER_HEIGHT));
                        y += SLIDER_HEIGHT + ROW_SPACING;
                    }
                }
            }
        }
        return y - contentTop + scrollOffsetPx + CONTENT_PADDING;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubleClick) {
        if (click.button() != 0) {
            return false;
        }
        double mx = click.x();
        double my = click.y();

        int closeX = panelX + WINDOW_WIDTH - CONTENT_PADDING - CLOSE_BUTTON_SIZE;
        int closeY = panelY + (HEADER_HEIGHT - CLOSE_BUTTON_SIZE) / 2;
        if (contains(closeX, closeY, CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE, mx, my)) {
            close();
            return true;
        }

        for (TabHit tab : tabHits) {
            if (contains(tab.x, tab.y, tab.w, tab.h, mx, my)) {
                if (selectedCategory != tab.category) {
                    selectedCategory = tab.category;
                    scrollOffset = 0;
                }
                return true;
            }
        }

        int contentTop = panelY + HEADER_HEIGHT;
        int contentBottom = panelY + WINDOW_HEIGHT;
        if (!withinContent(my, contentTop, contentBottom)) {
            return false;
        }

        if (editHudButton != null && contains(editHudButton.x(), editHudButton.y(), editHudButton.w(), editHudButton.h(), mx, my)) {
            ModuleManager.saveConfig();
            MinecraftClient.getInstance().setScreen(new HudEditScreen());
            return true;
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
        if (draggingSlider != null) {
            draggingSlider = null;
            ModuleManager.saveConfig();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int contentX = panelX + SIDEBAR_WIDTH;
        int contentTop = panelY + HEADER_HEIGHT;
        if (!contains(contentX, contentTop, WINDOW_WIDTH - SIDEBAR_WIDTH, WINDOW_HEIGHT - HEADER_HEIGHT, mouseX, mouseY)) {
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

    private record TabHit(Category category, int x, int y, int w, int h) {
    }

    private record ButtonHit(int x, int y, int w, int h) {
    }

    private record RowHit(Module module, int x, int y, int w, int h) {
    }

    private record SliderHit(Module module, IntSetting setting, int x, int y, int w, int h) {
    }
}
