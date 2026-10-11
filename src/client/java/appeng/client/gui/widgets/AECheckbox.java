package appeng.client.gui.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

import appeng.client.gui.style.Blitter;
import appeng.client.gui.style.PaletteColor;
import appeng.client.gui.style.ScreenStyle;

public class AECheckbox extends AbstractButton implements ITooltip {
    public static final int SIZE = 14;

    private static final Blitter BLITTER = Blitter.texture("guis/checkbox.png", 64, 64);

    private static final Blitter UNCHECKED = BLITTER.copy().src(0, 28, 22, 12);
    private static final Blitter UNCHECKED_FOCUS = BLITTER.copy().src(22, 28, 22, 12);
    private static final Blitter CHECKED = BLITTER.copy().src(0, 40, 22, 12);
    private static final Blitter CHECKED_FOCUS = BLITTER.copy().src(22, 40, 22, 12);
    private static final Blitter RADIO_UNCHECKED = BLITTER.copy().src(2 * SIZE, 0, SIZE, SIZE);
    private static final Blitter RADIO_UNCHECKED_FOCUS = BLITTER.copy().src(3 * SIZE, 0, SIZE, SIZE);
    private static final Blitter RADIO_CHECKED = BLITTER.copy().src(2 * SIZE, SIZE, SIZE, SIZE);
    private static final Blitter RADIO_CHECKED_FOCUS = BLITTER.copy().src(3 * SIZE, SIZE, SIZE, SIZE);

    private final ScreenStyle style;
    private boolean selected;
    private Runnable changeListener;
    private boolean radio;
    private Component desktopLabel;
    private boolean desktopHosted;

    public void setDesktopHosted(boolean hosted) {
        if (hosted && !desktopHosted && !radio) {
            setWidth(getWidth() + 4);
        }
        desktopHosted = hosted;
    }

    public void setDesktopLabel(Component label) {
        desktopLabel = label;
    }

    public Component displayLabel() {
        return style.isDesktopRestyled() && desktopLabel != null ? desktopLabel : getMessage();
    }

    @Override
    public java.util.List<Component> getTooltipMessage() {
        return desktopLabel == null ? java.util.List.of() : java.util.List.of(getMessage());
    }

    @Override
    public net.minecraft.client.renderer.Rect2i getTooltipArea() {
        return new net.minecraft.client.renderer.Rect2i(getX(), getY(), width, height);
    }

    @Override
    public boolean isTooltipAreaVisible() {
        return visible;
    }

    public AECheckbox(int x, int y, int width, int height, ScreenStyle style, Component component) {
        super(x, y, width, height, component);
        this.style = style;
    }

    @Override
    public void onPress(InputWithModifiers input) {
        this.selected = !this.selected;
        if (this.changeListener != null) {
            this.changeListener.run();
        }
    }

    public boolean isRadio() {
        return radio;
    }

    public void setRadio(boolean radio) {
        this.radio = radio;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public void setChangeListener(Runnable listener) {
        this.changeListener = listener;
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        narrationElementOutput.add(NarratedElementType.TITLE, this.createNarrationMessage());
        if (this.active) {
            if (this.isFocused()) {
                narrationElementOutput.add(NarratedElementType.USAGE,
                        Component.translatable("narration.checkbox.usage.focused"));
            } else {
                narrationElementOutput.add(NarratedElementType.USAGE,
                        Component.translatable("narration.checkbox.usage.hovered"));
            }
        }

    }

    @Override
    public void extractContents(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        Blitter icon;
        if (isRadio()) {
            if (active && (isMouseOver(mouseX, mouseY) || desktopHosted && isFocused())) {
                icon = isSelected() ? RADIO_CHECKED_FOCUS : RADIO_UNCHECKED_FOCUS;
            } else {
                icon = isSelected() ? RADIO_CHECKED : RADIO_UNCHECKED;
            }
        } else {
            if (active && (isMouseOver(mouseX, mouseY) || desktopHosted && isFocused())) {
                icon = isSelected() ? CHECKED_FOCUS : UNCHECKED_FOCUS;
            } else {
                icon = isSelected() ? CHECKED : UNCHECKED;
            }
        }

        if (!desktopHosted && !style.isDesktopRestyled() && !isMouseOver(mouseX, mouseY)) {
            setFocused(false);
        }

        var minecraft = Minecraft.getInstance();
        var font = minecraft.font;

        var textColor = isActive() ? PaletteColor.DEFAULT_TEXT_COLOR : PaletteColor.MUTED_TEXT_COLOR;
        var opacity = isActive() ? 1 : 0.5f;

        if (style.isDesktopRestyled()) {
            if (!radio) {
                String name = "widget/checkbox" + (selected ? "_selected" : "")
                        + (isHoveredOrFocused() ? "_highlighted" : "");
                Blitter.guiSprite(net.minecraft.resources.Identifier.fromNamespaceAndPath("minecraft", name))
                        .dest(getX(), getY(), 14, 14).opacity(opacity).blit(guiGraphics);
            } else {
                var bounds = new net.minecraft.client.renderer.Rect2i(getX(), getY(), 14, 14);
                appeng.client.gui.DesktopChrome.button(guiGraphics, bounds, isMouseOver(mouseX, mouseY), isFocused(),
                        active);
                if (selected) {
                    int color = active ? 0xFF404040 : 0xFF777777;
                    if (radio) {
                        guiGraphics.fill(getX() + 4, getY() + 4, getX() + 10, getY() + 10, color);
                    } else {
                        guiGraphics.fill(getX() + 3, getY() + 6, getX() + 6, getY() + 10, color);
                        guiGraphics.fill(getX() + 6, getY() + 8, getX() + 8, getY() + 11, color);
                        for (int i = 0; i < 5; i++) {
                            guiGraphics.fill(getX() + 7 + i, getY() + 8 - i, getX() + 8 + i, getY() + 11 - i, color);
                        }
                    }
                }
            }
        } else {
            icon.dest(getX(), getY()).opacity(opacity).blit(guiGraphics);
        }
        var label = desktopHosted
                ? displayLabel().copy().withStyle(s -> s.withColor(style.getColor(textColor).toARGB()))
                : displayLabel();
        var lines = font.split(label, width - (desktopHosted || style.isDesktopRestyled() ? radio ? 16 : 26 : 22));
        // try to vertically center if it's just one line
        var lineY = getY() + (lines.size() <= 1 ? 4 : 1);
        for (var line : lines) {
            guiGraphics.text(font, line, getX() + (isRadio() ? 16 : 26), lineY,
                    style.getColor(textColor).toARGB(), false);
            lineY += font.lineHeight;
        }
    }
}
