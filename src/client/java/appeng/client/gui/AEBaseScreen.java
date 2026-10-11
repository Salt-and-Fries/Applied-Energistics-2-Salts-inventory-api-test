/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2015, AlgorithmX2, All rights reserved.
 *
 * Applied Energistics 2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Applied Energistics 2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Applied Energistics 2.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.client.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Stopwatch;
import com.mojang.blaze3d.platform.InputConstants;

import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ComponentRenderUtils;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import guideme.GuidesCommon;
import guideme.PageAnchor;
import guideme.color.SymbolicColor;
import guideme.compiler.IdUtils;
import guideme.document.DefaultStyles;
import guideme.indices.ItemIndex;
import guideme.style.ResolvedTextStyle;
import guideme.style.TextStyle;

import appeng.api.behaviors.ContainerItemStrategies;
import appeng.api.behaviors.EmptyingAction;
import appeng.api.implementations.menuobjects.ItemMenuHost;
import appeng.api.parts.IPart;
import appeng.api.stacks.GenericStack;
import appeng.client.AppEngClient;
import appeng.client.Point;
import appeng.client.gui.layout.SlotGridLayout;
import appeng.client.gui.style.BackgroundGenerator;
import appeng.client.gui.style.Blitter;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.style.SlotPosition;
import appeng.client.gui.style.Text;
import appeng.client.gui.style.TextAlignment;
import appeng.client.gui.widgets.ITickingWidget;
import appeng.client.gui.widgets.ITooltip;
import appeng.client.gui.widgets.OpenGuideButton;
import appeng.client.gui.widgets.VerticalButtonBar;
import appeng.core.AEConfig;
import appeng.core.AELog;
import appeng.core.AppEng;
import appeng.core.localization.ButtonToolTips;
import appeng.core.localization.Tooltips;
import appeng.core.network.ServerboundPacket;
import appeng.core.network.serverbound.InventoryActionPacket;
import appeng.core.network.serverbound.SwapSlotsPacket;
import appeng.helpers.InventoryAction;
import appeng.items.tools.GuideItem;
import appeng.menu.AEBaseMenu;
import appeng.menu.MenuPacketScope;
import appeng.menu.SlotSemantic;
import appeng.menu.SlotSemantics;
import appeng.menu.slot.AppEngSlot;
import appeng.menu.slot.CraftingTermSlot;
import appeng.menu.slot.DisabledSlot;
import appeng.menu.slot.FakeSlot;
import appeng.menu.slot.IOptionalSlot;
import appeng.menu.slot.ResizableSlot;
import appeng.util.ConfigMenuInventory;
import appeng.util.Icon;

public abstract class AEBaseScreen<T extends AEBaseMenu> extends AbstractContainerScreen<T> {
    private boolean desktopHosted;
    private boolean desktopInitialized;
    private int desktopLayoutWidth;
    private int desktopLayoutHeight;
    private int desktopX = Integer.MIN_VALUE;
    private int desktopY = Integer.MIN_VALUE;
    private int desktopNaturalWidth;
    private int desktopNaturalHeight;
    private DesktopContentActions desktopActions;
    private java.util.function.Consumer<AEBaseScreen<?>> desktopNavigation;
    private static final ThreadLocal<AEBaseScreen<?>> CURRENT_DESKTOP_CONTENT = new ThreadLocal<>();
    protected final List<Slot> desktopVirtualSlots = new ArrayList<>();
    private int[] desktopRequestedSize;
    private Rect2i desktopFrame = new Rect2i(0, 0, 0, 0);
    private boolean desktopWidgetsDirty;
    private int desktopVisibilityStamp;
    private boolean desktopSharedToolbox;
    private int desktopTitleRight = Integer.MAX_VALUE;
    private Component desktopToolboxLabel;

    public final void setDesktopToolboxBadge(String badge) {
        desktopToolboxLabel = badge == null ? null
                : Component.translatable("gui.ae2.DesktopPanel.toolboxWithBadge", badge);
        if (menu.isDesktopToolboxCompanion() && desktopToolboxLabel != null) {
            setTextContent(TEXT_ID_DIALOG_TITLE, desktopToolboxLabel);
        }
    }

    public final boolean hasDesktopSharedToolbox() {
        return desktopSharedToolbox;
    }

    public final void setDesktopSharedToolbox(boolean shared) {
        if (desktopSharedToolbox != shared) {
            desktopSharedToolbox = shared;
            refreshDesktopContentLayout();
        }
    }

    /** Local title boundary reserved by the optional window manager's actual control rectangles. */
    public final void setDesktopTitleRight(int right) {
        desktopTitleRight = right;
    }

    /** Full styled title, before presentation-only shortening, for the optional desktop manager. */
    public final Component desktopManagementTitle() {
        var text = style.getText().get(TEXT_ID_DIALOG_TITLE);
        var override = textOverrides.get(TEXT_ID_DIALOG_TITLE);
        if (text == null || override != null && override.isHidden()) {
            return Component.empty();
        }
        return override != null && override.getContent() != null
                ? override.getContent().copy().withStyle(text.getText().getStyle())
                : text.getText();
    }

    public final Point desktopManagementTitlePosition() {
        var text = style.getText().get(TEXT_ID_DIALOG_TITLE);
        return text == null ? new Point(8, 6) : text.getPosition().resolve(getBounds(false));
    }

    public final Rect2i desktopFrame() {
        return desktopFrame;
    }

    public final Rect2i desktopContentBounds() {
        return new Rect2i(leftPos, topPos, Math.max(0, desktopLayoutWidth), Math.max(0, desktopLayoutHeight));
    }

    public final void refreshDesktopLayout() {
        desktopWidgetsDirty = true;
    }

    public final void reloadDesktopResources() {
        appeng.client.gui.style.StyleManager.refreshDesktopResources(style);
        var generated = style.getGeneratedBackground();
        var background = style.getBackground();
        if (generated != null) {
            desktopNaturalWidth = generated.getWidth();
            desktopNaturalHeight = generated.getHeight();
        } else if (background != null) {
            desktopNaturalWidth = background.getSrcWidth();
            desktopNaturalHeight = background.getSrcHeight();
        }
        prepareDesktop();
        refreshDesktopContentLayout();
    }

    public final void narrateDesktop(net.minecraft.client.gui.narration.NarrationElementOutput output) {
        for (var child : children()) {
            if (child instanceof net.minecraft.client.gui.components.AbstractWidget widget
                    && widget.visible && widget.active && widget.isFocused()) {
                widget.updateNarration(output.nest());
            }
        }
    }

    public final void refreshDesktopContentLayout() {
        desktopLayoutWidth = -1;
        desktopWidgetsDirty = true;
    }

    public final String desktopResizeKind() {
        return style.getDesktop() == null ? "fixed" : style.getDesktop().resize();
    }

    public final int desktopBackgroundColor() {
        return style.getDesktop() == null ? DesktopChrome.SURFACE : style.getDesktop().backgroundColor();
    }

    public final boolean desktopExternalSlot(Slot slot) {
        if (usesAeDesktopLayout()) {
            return slot.x < 0 || slot.y < 0 || slot.x + 16 > desktopLayoutWidth
                    || slot.y + 16 > desktopLayoutHeight;
        }
        return widgets.isDesktopExternalSlot(slot);
    }

    public final boolean desktopSemanticVisible(SlotSemantic semantic) {
        return !hiddenSlots.contains(semantic)
                && (!(this instanceof AESubScreen<?, ?>) || style.getSlots().containsKey(semantic.id()));
    }

    public final boolean desktopSlotVisible(Slot slot) {
        var semantic = menu.getSlotSemantic(slot);
        return slot.isActive() && slot.x > -1000 && !menu.isPlayerInventorySlot(slot)
                && (semantic == null || desktopSemanticVisible(semantic))
                && (!(slot instanceof AppEngSlot ae) || ae.isSlotEnabled());
    }

    public final List<Rect2i> desktopExternalBounds() {
        return widgets.desktopExternalBounds();
    }

    public final void requestDesktopSize(int width, int height) {
        desktopRequestedSize = new int[] { width, height };
    }

    public final int[] desktopRequestedSize() {
        return desktopRequestedSize;
    }

    public final void prepareDesktop() {
        if (!desktopHosted) {
            desktopNaturalWidth = imageWidth;
            desktopNaturalHeight = style.getTerminalStyle() == null ? imageHeight
                    : style.getTerminalStyle().getScreenHeight(2);
        }
        desktopHosted = true;
        if (style.getTerminalStyle() != null) {
            style.getTerminalStyle().setDesktopHeaderExtraHeight(14);
        }
        setTextHidden(TEXT_ID_DIALOG_TITLE, style.getDesktop() != null && !style.getDesktop().showTitle());
        setTextHidden("player_inventory_title", true);
    }

    private boolean desktopPlayerPanel() {
        return style.getSlots().containsKey(SlotSemantics.PLAYER_INVENTORY.id())
                && !menu.getSlots(SlotSemantics.PLAYER_INVENTORY).isEmpty();
    }

    public final int[] desktopPreferredSize() {
        if (style.getTerminalStyle() != null) {
            return new int[] { style.getTerminalStyle().getScreenWidth(),
                    style.getTerminalStyle().getScreenHeight(desktopMinimumTerminalRows()) - desktopInventoryCut() };
        }
        var desktop = style.getDesktop();
        if (usesAeDesktopLayout()) {
            var generated = style.getGeneratedBackground();
            if (generated != null && style.getBackground() == null && generated.getHeight() > 0) {
                return new int[] { generated.getWidth(), generated.getHeight() - desktopInventoryCut() };
            }
            return new int[] { desktop.width(), desktop.height() };
        }
        if (desktop != null && style.getGeneratedBackground() != null && desktop.resize().equals("fixed")) {
            return new int[] { style.getGeneratedBackground().getWidth(), desktop.fittedHeight(menu, style) };
        }
        return desktop == null ? new int[] { desktopNaturalWidth, desktopNaturalHeight }
                : new int[] { desktop.minWidth(), desktop.minHeight() };
    }

    public final int desktopMinimumTerminalRows() {
        var terminal = style.getTerminalStyle();
        if (terminal == null || !usesAeDesktopLayout())
            return 2;
        long upgrades = menu.getSlots(SlotSemantics.UPGRADE).stream()
                .filter(slot -> slot.isActive() && (!(slot instanceof AppEngSlot ae) || ae.isSlotEnabled())).count();
        int housingHeight = upgrades == 0 ? 0 : 10 + (int) Math.min(8, upgrades) * 18 + 4;
        int rows = 2;
        while (terminal.getScreenHeight(rows) - desktopInventoryCut() < housingHeight)
            rows++;
        return rows;
    }

    /** The toolbar follows the outermost connected body, including a CPU selector when present. */
    public int desktopToolbarLeft() {
        return 0;
    }

    public final int[] desktopDefaultSize() {
        var desktop = style.getDesktop();
        int extra = style.getTerminalStyle() == null ? 0 : style.getTerminalStyle().getDesktopHeaderExtraHeight();
        return desktop == null ? desktopPreferredSize() : new int[] { desktop.width(), desktop.height() + extra };
    }

    /** Explicit visible surface segments, in local GUI pixels, independent of the menu's logical slots. */
    public List<Rect2i> desktopSurfaceSegments() {
        return List.of(new Rect2i(0, 0, Math.max(0, desktopLayoutWidth), Math.max(0, desktopLayoutHeight)));
    }

    /** Complete the cut surface using the same resource-pack-aware AE artwork as the original screen. */
    protected void drawDesktopBottomEdge(GuiGraphicsExtractor graphics) {
        if (!usesAeDesktopLayout() || desktopInventoryCut() == 0) {
            return;
        }
        var source = style.getTerminalStyle() == null ? style.getBackground() : style.getTerminalStyle().getBottom();
        if (source != null && source.getSrcHeight() >= 4) {
            source.copy().src(source.getSrcX(), source.getSrcY() + source.getSrcHeight() - 4,
                    source.getSrcWidth(), 4).dest(leftPos, topPos + desktopLayoutHeight - 4).blit(graphics);
        }
    }

    public final int[] desktopSnapSize(int width, int height) {
        var terminal = style.getTerminalStyle();
        if (usesAeDesktopLayout() && desktopResizeKind().equals("list")) {
            var minimum = desktopPreferredSize();
            return new int[] { minimum[0], minimum[1] + Math.max(0, (height - minimum[1]) / 18) * 18 };
        }
        if (terminal != null) {
            int baseWidth = terminal.getScreenWidth();
            if (usesAeDesktopLayout()) {
                return new int[] { baseWidth,
                        terminal.getScreenHeight(Math.max(desktopMinimumTerminalRows(),
                                terminal.getPossibleRows(height + desktopInventoryCut())))
                                - desktopInventoryCut() };
            }
            return new int[] { baseWidth + Math.max(0, (width - baseWidth) / 18) * 18,
                    terminal.getScreenHeight(Math.max(2, terminal.getPossibleRows(height))) };
        }
        return new int[] { width, height };
    }

    final void repositionDesktopToolbox(Point origin) {
        int index = 0;
        for (var slot : menu.getSlots(SlotSemantics.TOOLBOX)) {
            slot.x = origin.getX() + (index % 3) * 18;
            slot.y = origin.getY() + (index / 3) * 18;
            index++;
        }
    }

    public final boolean isDesktopHosted() {
        return desktopHosted;
    }

    public final boolean usesAeDesktopLayout() {
        return style.getDesktop() != null && style.getDesktop().preserveAeLayout();
    }

    public final int desktopInventoryCut() {
        return usesAeDesktopLayout() ? style.getDesktop().inventoryCut() : 0;
    }

    /** Opt-in atlas evidence, normalized to the same origin as an ordinary AE screen. */
    public final Map<String, int[]> desktopFidelityGeometry() {
        var result = new java.util.TreeMap<String, int[]>();
        result.put("layout", new int[] { imageWidth, imageHeight });
        if (desktopHosted && desktopTitleArea != null) {
            result.put("text/dialog_title", new int[] { desktopTitleArea.getX(), desktopTitleArea.getY(),
                    desktopRenderedTitleWidth, desktopTitleArea.getHeight() });
        }
        var occurrences = new java.util.HashMap<String, Integer>();
        for (var child : children()) {
            if (child instanceof net.minecraft.client.gui.components.AbstractWidget widget && widget.visible) {
                String key = "widget/" + widget.getClass().getSimpleName() + "/" + widget.getMessage().getString();
                int occurrence = occurrences.merge(key, 1, Integer::sum);
                result.put(key + "/" + occurrence, new int[] { widget.getX() - leftPos, widget.getY() - topPos,
                        widget.getWidth(), widget.getHeight() });
            }
        }
        occurrences.clear();
        if (usesAeDesktopLayout()) {
            int barIndex = 0;
            for (var toolbar : widgets.desktopToolbars()) {
                int columnIndex = 0, buttonIndex = 0;
                for (var column : toolbar.desktopBackingColumns()) {
                    result.put("backing/toolbar/" + barIndex + "/" + columnIndex++, new int[] {
                            column.getX(), column.getY(), column.getWidth(), column.getHeight() });
                }
                for (var button : toolbar.buttons()) {
                    if (button.visible) {
                        var paint = appeng.client.gui.widgets.VerticalButtonBar.desktopPaintBounds(button);
                        result.put("paint/toolbar/" + barIndex + "/" + buttonIndex++, new int[] {
                                paint.getX() - leftPos, paint.getY() - topPos, paint.getWidth(), paint.getHeight() });
                    }
                }
                barIndex++;
            }
        }
        for (var slot : desktopSlots()) {
            if (menu.isPlayerInventorySlot(slot) || slot.x < -1000 || !slot.isActive()) {
                continue;
            }
            var semantic = menu.getSlotSemantic(slot);
            String key = "slot/" + (semantic == null ? slot.getClass().getSimpleName() : semantic.id());
            int occurrence = occurrences.merge(key, 1, Integer::sum);
            result.put(key + "/" + occurrence, new int[] { slot.x, slot.y });
        }
        return result;
    }

    public static AEBaseScreen<?> currentDesktopContent() {
        return CURRENT_DESKTOP_CONTENT.get();
    }

    public final void setDesktopNavigation(java.util.function.Consumer<AEBaseScreen<?>> navigation) {
        desktopNavigation = navigation;
    }

    final void clearDesktopWidgets() {
        clearWidgets();
    }

    private boolean desktopMouseClicked(MouseButtonEvent event, boolean doubleClick) {
        drag_click.clear();
        hoveredSlot = desktopSlotAt(event.x(), event.y());
        handlingRightClick = event.button() == InputConstants.MOUSE_BUTTON_RIGHT;
        var widgetEvent = handlingRightClick
                ? new MouseButtonEvent(event.x(), event.y(),
                        new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, event.buttonInfo().modifiers()))
                : event;
        try {
            if (widgets.onMouseDown(getMousePoint(event.x(), event.y()), event.button())) {
                return true;
            }
            for (var widget : children()) {
                if (widget.mouseClicked(widgetEvent, doubleClick)) {
                    setFocused(widget);
                    setDragging(event.button() == InputConstants.MOUSE_BUTTON_LEFT);
                    return true;
                }
            }
        } finally {
            handlingRightClick = false;
        }
        setFocused(null);
        var slot = hoveredSlot;
        if (slot == null) {
            return false;
        }
        var pick = InputConstants.Type.MOUSE.getOrCreate(event.button());
        if (minecraft.options.keyPickItem.isActiveAndMatches(pick) && handlePickBlock(slot)) {
            return true;
        }
        boolean custom = desktopVirtualSlots.contains(slot) || slot instanceof FakeSlot
                || slot instanceof CraftingTermSlot || InputConstants.isKeyDown(InputConstants.KEY_SPACE)
                || (event.button() == InputConstants.MOUSE_BUTTON_RIGHT
                        && getEmptyingAction(slot, menu.getCarried()) != null);
        if (!custom) {
            return false; // Real slot gestures belong exclusively to the desktop.
        }
        if (minecraft.options.keyPickItem.isActiveAndMatches(pick)) {
            slotClicked(slot, slot.index, 2, ContainerInput.CLONE);
            return true;
        }
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT
                && event.button() != InputConstants.MOUSE_BUTTON_RIGHT) {
            return false;
        }
        int button = event.button() == InputConstants.MOUSE_BUTTON_RIGHT ? 1 : 0;
        slotClicked(slot, slot.index, button,
                minecraft.hasShiftDown() ? ContainerInput.QUICK_MOVE : ContainerInput.PICKUP);
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (!desktopHosted) {
            return super.keyPressed(event);
        }
        var focused = getFocused();
        if (focused != null && focused.keyPressed(event)) {
            return true;
        }
        if (event.key() == InputConstants.KEY_TAB) {
            var navigation = new net.minecraft.client.gui.navigation.FocusNavigationEvent.TabNavigation(
                    !event.hasShiftDown());
            var path = nextFocusPath(navigation);
            if (path == null) {
                clearFocus();
                path = nextFocusPath(navigation);
            }
            if (path != null) {
                changeFocus(path);
            }
            return true;
        }
        if (event.isEscape() || (!desktopWantsTextInput()
                && minecraft.options.keyInventory.isActiveAndMatches(InputConstants.getKey(event)))) {
            closeCurrentMenu();
            return true;
        }
        var slot = getSlotUnderMouse();
        if (slot != null && (desktopVirtualSlots.contains(slot) || slot instanceof FakeSlot
                || slot instanceof CraftingTermSlot)) {
            if (checkHotbarKeyPressed(event)) {
                return true;
            }
            if (minecraft.options.keyPickItem.matches(event)) {
                slotClicked(slot, slot.index, 0, ContainerInput.CLONE);
                return true;
            }
            if (minecraft.options.keyDrop.matches(event)) {
                slotClicked(slot, slot.index, event.hasControlDown() ? 1 : 0, ContainerInput.THROW);
                return true;
            }
        }
        return false;
    }

    /** Positions the actual AE controls in a desktop content rectangle; never installs a game screen. */
    public final void layoutDesktop(int x, int y, int contentWidth, int contentHeight, Rect2i frame) {
        desktopFrame = frame;
        if (desktopInitialized) {
            updateBeforeRender();
        }
        int visibility = widgets.desktopVisibilityStamp();
        desktopWidgetsDirty |= desktopVisibilityStamp != visibility;
        if (desktopRequestedSize != null && contentWidth == desktopRequestedSize[0]
                && contentHeight == desktopRequestedSize[1]) {
            desktopRequestedSize = null;
        }
        boolean rebuild = !desktopInitialized || desktopLayoutWidth != contentWidth
                || desktopLayoutHeight != contentHeight;
        if (rebuild) {
            var focus = getFocused();
            imageWidth = contentWidth;
            imageHeight = contentHeight + desktopInventoryCut();
            this.init(contentWidth, imageHeight + 2 * config.getTerminalMargin());
            desktopInitialized = true;
            desktopLayoutWidth = contentWidth;
            desktopLayoutHeight = contentHeight;
            if (focus != null) {
                setFocused(focus);
            }
        }
        leftPos = x;
        topPos = y;
        if (rebuild || desktopWidgetsDirty || desktopX != x || desktopY != y) {
            widgets.populateDesktop(this::addRenderableWidget, getBounds(true), this);
            desktopX = x;
            desktopY = y;
            desktopWidgetsDirty = false;
            desktopVisibilityStamp = visibility;
        }
    }

    public final void saveDesktopPreferences(net.minecraft.nbt.CompoundTag tag) {
        widgets.saveDesktopPreferences(tag);
    }

    public final void loadDesktopPreferences(net.minecraft.nbt.CompoundTag tag) {
        widgets.loadDesktopPreferences(tag);
    }

    /** Content-specific cleanup without invoking an ordinary container screen's close protocol. */
    public void desktopClosed() {
        cancelDesktopPointer();
    }

    public final void withDesktopActions(DesktopContentActions actions, Runnable callback) {
        var previous = desktopActions;
        var previousContent = CURRENT_DESKTOP_CONTENT.get();
        desktopActions = actions;
        CURRENT_DESKTOP_CONTENT.set(this);
        try {
            callback.run();
        } finally {
            desktopActions = previous;
            if (previousContent == null) {
                CURRENT_DESKTOP_CONTENT.remove();
            } else {
                CURRENT_DESKTOP_CONTENT.set(previousContent);
            }
        }
    }

    public final List<Slot> desktopSlots() {
        var result = new ArrayList<>(menu.slots);
        result.addAll(desktopVirtualSlots);
        for (var slot : menu.clientSideSlots()) {
            if (!result.contains(slot)) {
                result.add(slot);
            }
        }
        return result;
    }

    public final Slot desktopSlotAt(double x, double y) {
        for (var slot : desktopSlots()) {
            if (desktopSlotVisible(slot) && isHovering(slot, x, y)) {
                return slot;
            }
        }
        return null;
    }

    public final void extractDesktopContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        updateBeforeRender();
        widgets.updateBeforeRender();
        hoveredSlot = desktopSlotAt(mouseX, mouseY);
        drawBG(graphics, leftPos, topPos, mouseX, mouseY, 0);
        drawDesktopBottomEdge(graphics);
        if (usesAeDesktopLayout()) {
            for (var slot : desktopSlots()) {
                if (!menu.isPlayerInventorySlot(slot) && slot instanceof IOptionalSlot optional) {
                    drawOptionalSlotBackground(graphics, optional, false);
                }
            }
        }
        if (!usesAeDesktopLayout() && (menu instanceof appeng.menu.implementations.InscriberMenu
                || menu instanceof appeng.menu.implementations.CondenserMenu
                || menu instanceof appeng.menu.implementations.QuartzKnifeMenu
                || menu instanceof appeng.menu.implementations.SpatialIOPortMenu)) {
            var input = menu.getSlots(SlotSemantics.MACHINE_INPUT).getFirst();
            var output = menu.getSlots(SlotSemantics.MACHINE_OUTPUT).getFirst();
            DesktopChrome.arrow(graphics, leftPos + (input.x + 16 + output.x - 23) / 2,
                    topPos + output.y);
        }
        widgets.drawBackgroundLayer(graphics, getBounds(true), getMousePoint(mouseX, mouseY));
        graphics.pose().pushMatrix();
        try {
            graphics.pose().translate(leftPos, topPos);
            extractLabels(graphics, mouseX, mouseY);
            for (var slot : desktopSlots()) {
                if (!menu.slots.contains(slot) && desktopSlotVisible(slot)) {
                    if (!usesAeDesktopLayout()) {
                        DesktopChrome.slot(graphics, slot.x, slot.y);
                    }
                    extractSlot(graphics, slot, mouseX, mouseY);
                }
            }
            drawDesktopBodyOverlayHints(graphics);
        } finally {
            graphics.pose().popMatrix();
        }
        for (var renderable : renderables) {
            if (!widgets.isDesktopExternalButton(renderable)) {
                renderable.extractRenderState(graphics, mouseX, mouseY, 0);
            }
        }
    }

    public final void extractDesktopAttachments(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        widgets.drawDesktopAttachments(graphics, mouseX, mouseY);
        if (usesAeDesktopLayout()) {
            graphics.pose().pushMatrix();
            graphics.pose().translate(leftPos, topPos);
            for (var entry : style.getText().entrySet()) {
                var position = entry.getValue().getPosition().resolve(getBounds(false));
                if (position.getX() < 0 || position.getY() < 0 || position.getX() >= desktopLayoutWidth) {
                    drawText(graphics, entry.getValue(), textOverrides.get(entry.getKey()));
                }
            }
            graphics.pose().popMatrix();
        }
        drawDesktopOverlayHints(graphics);
    }

    /** Draw the original AE item/icon appearance; hit testing and gestures still belong to the API. */
    public final void extractDesktopRealSlots(GuiGraphicsExtractor graphics, boolean external, int mouseX, int mouseY) {
        extractDesktopRealSlots(graphics, external, mouseX, mouseY, slot -> null);
    }

    public final void extractDesktopRealSlots(GuiGraphicsExtractor graphics, boolean external, int mouseX, int mouseY,
            java.util.function.Function<Slot, DesktopSlotPresentation> presentation) {
        graphics.pose().pushMatrix();
        try {
            graphics.pose().translate(leftPos, topPos);
            for (var slot : menu.slots) {
                if (desktopSlotVisible(slot) && desktopExternalSlot(slot) == external) {
                    if (slot == hoveredSlot && slot.isHighlightable()) {
                        graphics.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
                                net.minecraft.resources.Identifier
                                        .withDefaultNamespace("container/slot_highlight_back"),
                                slot.x - 4, slot.y - 4, 24, 24);
                    }
                    var appearance = presentation.apply(slot);
                    if (appearance == null) {
                        extractSlot(graphics, slot, mouseX, mouseY);
                    } else {
                        var displayed = appearance.stack();
                        if (slot instanceof AppEngSlot aeSlot) {
                            drawAppEngSlotArtwork(graphics, aeSlot, displayed);
                        }
                        if (appearance.preview()) {
                            graphics.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, 0x80FFFFFF);
                        }
                        graphics.item(displayed, slot.x, slot.y);
                        graphics.itemDecorations(font, displayed, slot.x, slot.y, appearance.countText());
                    }
                    if (slot == hoveredSlot && slot.isHighlightable()) {
                        graphics.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
                                net.minecraft.resources.Identifier
                                        .withDefaultNamespace("container/slot_highlight_front"),
                                slot.x - 4, slot.y - 4, 24, 24);
                    }
                }
            }
        } finally {
            graphics.pose().popMatrix();
        }
    }

    protected void drawDesktopOverlayHints(GuiGraphicsExtractor graphics) {
    }

    protected void drawDesktopBodyOverlayHints(GuiGraphicsExtractor graphics) {
    }

    public final void extractDesktopTooltip(GuiGraphicsExtractor graphics, int x, int y) {
        hoveredSlot = desktopSlotAt(x, y);
        renderTooltips(graphics, x, y);
    }

    public final void extractDesktopSlotDecorations(GuiGraphicsExtractor graphics) {
        extractDesktopSlotDecorations(graphics, false);
    }

    public final void extractDesktopSlotDecorations(GuiGraphicsExtractor graphics, boolean external) {
        graphics.pose().pushMatrix();
        try {
            graphics.pose().translate(leftPos, topPos);
            for (var slot : menu.slots) {
                if (slot instanceof AppEngSlot ae && slot.isActive() && !menu.isPlayerInventorySlot(slot)
                        && ae.x > -1000 && desktopExternalSlot(slot) == external) {
                    if ((ae.renderIconWithItem() || ae.getItem().isEmpty()) && ae.isSlotEnabled()
                            && ae.getIcon() != null) {
                        Blitter.icon(ae.getIcon()).dest(ae.x, ae.y).opacity(ae.getOpacityOfIcon()).blit(graphics);
                    }
                    if (!ae.isValid()) {
                        graphics.fill(ae.x, ae.y, ae.x + 16, ae.y + 16, 0x66FF6666);
                    }
                }
            }
        } finally {
            graphics.pose().popMatrix();
        }
    }

    public final boolean desktopWantsTextInput() {
        return getFocused() instanceof EditBox box && box.isFocused();
    }

    public final void cancelDesktopPointer() {
        drag_click.clear();
        setDragging(false);
        widgets.cancelPointer();
    }

    protected final void closeCurrentMenu() {
        if (desktopHosted) {
            if (desktopActions != null) {
                desktopActions.close();
            }
        } else {
            getPlayer().closeContainer();
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(AEBaseScreen.class);

    private static final Point HIDDEN_SLOT_POS = new Point(-9999, -9999);

    /**
     * Commonly used id for text that is used to show the dialog title.
     */
    public static final String TEXT_ID_DIALOG_TITLE = "dialog_title";

    protected static final ResolvedTextStyle ERROR_TEXT_STYLE = TextStyle.builder()
            .color(SymbolicColor.ERROR_TEXT)
            .font(FontDescription.DEFAULT)
            .dropShadow(true)
            .build()
            .mergeWith(DefaultStyles.BASE_STYLE);

    private final VerticalButtonBar verticalToolbar;
    private final OpenGuideButton helpButton;

    // drag y
    private final Set<Slot> drag_click = new HashSet<>();
    private boolean disableShiftClick = false;
    private Stopwatch dbl_clickTimer = Stopwatch.createStarted();
    private ItemStack dbl_whichItem = ItemStack.EMPTY;
    private Slot bl_clicked;
    private boolean handlingRightClick;
    private final Map<String, TextOverride> textOverrides = new HashMap<>();
    private Component desktopTitleTooltip;
    private Rect2i desktopTitleArea;
    private int desktopRenderedTitleWidth;
    private final Set<SlotSemantic> hiddenSlots = new HashSet<>();
    protected final WidgetContainer widgets;
    protected final ScreenStyle style;
    protected final AEConfig config = AEConfig.instance();
    /**
     * The positions of all slots when a subscreen is opened.
     */
    private final List<SavedSlotInfo> savedSlotInfos = new ArrayList<>();

    public AEBaseScreen(T menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title);

        this.style = Objects.requireNonNull(style, "style");
        this.widgets = new WidgetContainer(style);
        this.verticalToolbar = new VerticalButtonBar();
//        this.widgets.add("verticalToolbar", this.verticalToolbar = new VerticalButtonBar());

        // TODO (RID): Added a check if a Screen should have the Vertical Tool Bar. This was added to avoid rendering
        // the bar from the SkyChestScreen.
        if (shouldAddToolbar()) {
            this.widgets.add("verticalToolbar", this.verticalToolbar);
        }

        // Add a help-button to the vertical button bar
        this.helpButton = addToLeftToolbar(new OpenGuideButton(btn -> openHelp()));

        if (style.getGeneratedBackground() != null) {
            this.imageWidth = style.getGeneratedBackground().getWidth();
            this.imageHeight = style.getGeneratedBackground().getHeight();
        } else if (style.getBackground() != null) {
            this.imageWidth = style.getBackground().getSrcWidth();
            this.imageHeight = style.getBackground().getSrcHeight();
        }
    }

    @Override
    @MustBeInvokedByOverriders
    protected void init() {
        super.init();
        positionSlots();

        widgets.populateScreen(this::addRenderableWidget, getBounds(true), this);
    }

    protected boolean shouldAddToolbar() {
        return true; // Default behavior is to add the toolbar
    }

    private void positionSlots() {
        for (var entry : style.getSlots().entrySet()) {
            var semantic = SlotSemantics.getOrThrow(entry.getKey());

            // Do not position slots that are hidden
            if (hiddenSlots.contains(semantic)) {
                continue;
            }

            repositionSlots(semantic);
        }
    }

    private Point getSlotPosition(SlotPosition position, int semanticIndex) {
        Point pos = position.resolve(getBounds(false));

        SlotGridLayout grid = position.getGrid();
        if (grid != null) {
            pos = grid.getPosition(pos.getX(), pos.getY(), semanticIndex);
        }
        return pos;
    }

    public final void repositionSlots(SlotSemantic semantic) {
        if (desktopHosted && (semantic == SlotSemantics.PLAYER_INVENTORY || semantic == SlotSemantics.PLAYER_HOTBAR)) {
            for (var slot : menu.getSlots(semantic)) {
                slot.x = -9999;
                slot.y = -9999;
            }
            return;
        }
        var position = style.getSlots().get(semantic.id());

        if (position.isHidden()) {
            menu.hideSlot(semantic.id());
            setSlotsHidden(semantic, true);
            return;
        }

        var slots = menu.getSlots(semantic);
        for (int i = 0; i < slots.size(); i++) {
            Slot slot = slots.get(i);

            // Special case for slots with overridable width/height, which use widget styles instead of
            // semantic based slot positioning
            if (slot instanceof ResizableSlot resizableSlot) {
                var widgetStyle = style.getWidget(resizableSlot.getStyleId());
                var pos = widgetStyle.resolve(getBounds(false));
                slot.x = pos.getX();
                slot.y = pos.getY();
                resizableSlot.setWidth(widgetStyle.getWidth());
                resizableSlot.setHeight(widgetStyle.getHeight());
            } else {
                Point pos = getSlotPosition(position, i);

                slot.x = pos.getX();
                slot.y = pos.getY();
            }
        }
    }

    final Rect2i getBounds(boolean absolute) {
        if (absolute) {
            return new Rect2i(leftPos, topPos, imageWidth, imageHeight);
        } else {
            return new Rect2i(0, 0, imageWidth, imageHeight);
        }
    }

    protected List<Slot> getInventorySlots() {
        return desktopHosted ? desktopSlots() : this.menu.slots;
    }

    /**
     * This method is called directly before rendering the screen, and should be used to perform layout, and other
     * rendering-related updates.
     */
    @MustBeInvokedByOverriders
    protected void updateBeforeRender() {
        helpButton.setVisibility(getHelpTopic() != null);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.updateBeforeRender();
        this.widgets.updateBeforeRender();

        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTicks);

        renderTooltips(guiGraphics, mouseX, mouseY);

        if (AEConfig.instance().isShowDebugGuiOverlays()) {
            // Show a green overlay on exclusion zones
            List<Rect2i> exclusionZones = getExclusionZones();
            for (Rect2i rectangle2d : exclusionZones) {
                fillRect(guiGraphics, rectangle2d, 0x7f00FF00);
            }

            guiGraphics.horizontalLine(leftPos, leftPos + imageWidth - 1, topPos, 0xFFFFFFFF);
            guiGraphics.horizontalLine(leftPos, leftPos + imageWidth - 1, topPos + imageHeight - 1, 0xFFFFFFFF);
            guiGraphics.verticalLine(leftPos, topPos, topPos + imageHeight, 0xFFFFFFFF);
            guiGraphics.verticalLine(leftPos + imageWidth - 1, topPos, topPos + imageHeight - 1, 0xFFFFFFFF);
        }
    }

    /**
     * Get the potential result of emptying an item for the purposes of setting a filter. Null even if the item could be
     * emptied, but the filter doesn't support the resulting key.
     */
    protected EmptyingAction getEmptyingAction(Slot slot, ItemStack carried) {
        if (!(slot instanceof AppEngSlot appEngSlot) || carried.isEmpty()) {
            return null;
        }

        if (!(appEngSlot.getInventory() instanceof ConfigMenuInventory configInv)) {
            return null;
        }

        // See if we should offer the left-/right-click differentiation for setting a different filter
        var emptyingAction = ContainerItemStrategies.getEmptyingAction(carried);
        if (emptyingAction != null) {
            var wrappedStack = GenericStack.wrapInItemStack(new GenericStack(emptyingAction.what(), 1));
            if (configInv.isItemValid(slot.getSlotIndex(), wrappedStack)) {
                return emptyingAction;
            }
        }

        return null;
    }

    private boolean renderEmptyingTooltip(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        // See if we should offer the left-/right-click differentiation for setting a different filter
        var emptyingAction = getEmptyingAction(this.hoveredSlot, menu.getCarried());
        if (emptyingAction != null) {
            drawTooltip(
                    guiGraphics,
                    mouseX,
                    mouseY,
                    Tooltips.getEmptyingTooltip(ButtonToolTips.SetAction, menu.getCarried(), emptyingAction));
            return true;
        }

        return false;
    }

    /**
     * Renders a potential tooltip (from one of the possible tooltip sources)
     */
    private void renderTooltips(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        if (desktopHosted && desktopTitleArea != null && desktopTitleTooltip != null
                && mouseX >= leftPos + desktopTitleArea.getX()
                && mouseX < leftPos + desktopTitleArea.getX() + desktopTitleArea.getWidth()
                && mouseY >= topPos + desktopTitleArea.getY()
                && mouseY < topPos + desktopTitleArea.getY() + desktopTitleArea.getHeight()) {
            var lines = new ArrayList<Component>();
            lines.add(desktopTitleTooltip);
            lines.add(Component.translatable(menu.isDesktopToolboxCompanion()
                    ? "gui.ae2.DesktopHint.companionDrag"
                    : "gui.ae2.DesktopHint.windowManagement")
                    .withStyle(ChatFormatting.GRAY));
            if (desktopToolboxLabel != null && !menu.isDesktopToolboxCompanion()) {
                lines.add(Component.translatable("gui.ae2.DesktopHint.toolboxSource", desktopToolboxLabel)
                        .withStyle(ChatFormatting.GRAY));
            }
            drawTooltip(guiGraphics, mouseX, mouseY, lines);
            return;
        }
        if (renderEmptyingTooltip(guiGraphics, mouseX, mouseY)) {
            return;
        } else if (this.hoveredSlot instanceof AppEngSlot appEngSlot) {
            var customTooltip = appEngSlot.getCustomTooltip(menu.getCarried());
            if (customTooltip != null) {
                drawTooltip(guiGraphics, mouseX, mouseY, customTooltip);
            }
        }

        this.extractTooltip(guiGraphics, mouseX, mouseY);

        // The line above should have render a tooltip if this condition is true, and no
        // additional tooltips should be shown
        if (this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
            return;
        }

        for (var c : this.renderables) {
            if (c instanceof ITooltip tooltipWidget) {
                if (!tooltipWidget.isTooltipAreaVisible()) {
                    continue;
                }

                var area = tooltipWidget.getTooltipArea();
                if (mouseX >= area.getX() && mouseY >= area.getY() &&
                        mouseX < area.getX() + area.getWidth()
                        && mouseY < area.getY() + area.getHeight()) {
                    var tooltip = new Tooltip(tooltipWidget.getTooltipMessage());
                    if (!tooltip.getContent().isEmpty()) {
                        drawTooltipWithHeader(guiGraphics, tooltip, mouseX, mouseY);
                    }
                }
            }
        }

        // Widget-container uses screen-relative coordinates while the rest uses window-relative
        Tooltip tooltip = this.widgets.getTooltip(mouseX - leftPos, mouseY - topPos);
        if (tooltip != null) {
            drawTooltipWithHeader(guiGraphics, tooltip, mouseX, mouseY);
        }
    }

    private void drawTooltipWithHeader(GuiGraphicsExtractor guiGraphics, Tooltip tooltip, int mouseX, int mouseY) {
        drawTooltipWithHeader(guiGraphics, mouseX, mouseY, tooltip.getContent());
    }

    /**
     * Draws a tooltip and word-wraps it to a maximum width.
     */
    public void drawTooltip(GuiGraphicsExtractor guiGraphics, int x, int y, List<Component> lines) {
        if (lines.isEmpty()) {
            return;
        }

        // Max width should be half screen with some padding.
        // Vanilla will place the tooltip on the right or left of the cursor
        // automatically, but uses a 12px offset (we use 40px for some extra space)
        int maxWidth = Math.max(120,
                (desktopHosted ? minecraft.getWindow().getGuiScaledWidth() : width) / 2 - 40);

        // Make the first line white
        // All lines after the first are colored gray
        List<FormattedCharSequence> styledLines = new ArrayList<>(lines.size());
        for (Component line : lines) {
            styledLines.addAll(ComponentRenderUtils.wrapComponents(line, maxWidth, font));
        }
        guiGraphics.setTooltipForNextFrame(font, styledLines, x, y);

    }

    /**
     * Draws a tooltip and word-wraps it to a maximum width.
     */
    public void drawTooltipWithHeader(GuiGraphicsExtractor guiGraphics, int x, int y, List<Component> lines) {
        if (lines.isEmpty()) {
            return;
        }

        var formattedLines = new ArrayList<Component>(lines.size());
        for (int i = 0; i < lines.size(); i++) {
            if (i == 0) {
                formattedLines.add(lines.get(i).copy().withStyle(s -> s.withColor(ChatFormatting.WHITE)));
            } else {
                formattedLines.add(lines.get(i).copy().withStyle(s -> {
                    if (s.getColor() != null) {
                        return s;
                    } else {
                        return s.withColor(ChatFormatting.GRAY);
                    }
                }));
            }
        }
        drawTooltip(guiGraphics, x, y, formattedLines);
    }

    @Override
    protected final void extractLabels(GuiGraphicsExtractor guiGraphics, int x, int y) {
        final int ox = this.leftPos;
        final int oy = this.topPos;

        widgets.drawForegroundLayer(guiGraphics, getBounds(false), new Point(x - ox, y - oy));

        this.drawFG(guiGraphics, ox, oy, x, y);

        if (style != null) {
            for (var entry : style.getText().entrySet()) {
                // Process text overrides
                var override = textOverrides.get(entry.getKey());
                drawText(guiGraphics, entry.getValue(), override);
            }
        }
    }

    private void drawText(GuiGraphicsExtractor guiGraphics, Text text, @Nullable TextOverride override) {
        // Don't draw if the screen decided to hide this
        if (override != null && override.isHidden()) {
            return;
        }

        int color = style.getColor(text.getColor()).toARGB();

        Point pos = text.getPosition().resolve(getBounds(false));

        float scale = text.getScale();

        // Allow overrides for which content is shown
        Component content = text.getText();
        if (override != null && override.getContent() != null) {
            content = override.getContent().copy().withStyle(content.getStyle());
        }
        if (desktopHosted && text == style.getText().get(TEXT_ID_DIALOG_TITLE)
                && text.getAlign() == TextAlignment.LEFT) {
            int available = Math.min(imageWidth - 8, desktopTitleRight) - pos.getX();
            for (var child : children()) {
                if (child instanceof net.minecraft.client.gui.components.AbstractWidget widget && widget.visible
                        && widget.getY() - topPos < pos.getY() + font.lineHeight
                        && widget.getY() + widget.getHeight() - topPos > pos.getY()
                        && widget.getX() - leftPos > pos.getX()) {
                    available = Math.min(available, widget.getX() - leftPos - pos.getX() - 4);
                }
            }
            available = Math.max(1, available);
            desktopTitleTooltip = content;
            desktopTitleArea = new Rect2i(pos.getX(), pos.getY(), available, font.lineHeight);
            if (font.width(content) > available) {
                String suffix = font.plainSubstrByWidth("...", available);
                String prefix = font.plainSubstrByWidth(content.getString(),
                        Math.max(0, available - font.width(suffix)));
                content = Component.literal(prefix + suffix).withStyle(content.getStyle());
                while (font.width(content) > available && !prefix.isEmpty()) {
                    prefix = prefix.substring(0, prefix.offsetByCodePoints(prefix.length(), -1));
                    content = Component.literal(prefix + suffix).withStyle(content.getStyle());
                }
            }
        }

        if (desktopHosted && text == style.getText().get(TEXT_ID_DIALOG_TITLE)) {
            desktopRenderedTitleWidth = font.width(content);
        }

        // Account for max width and split into lines
        List<FormattedCharSequence> lines;
        if (text.getMaxWidth() <= 0) {
            var line = content.getVisualOrderText();
            lines = List.of(line);
        } else {
            lines = this.font.split(content, text.getMaxWidth());
        }

        int y = pos.getY();
        for (var line : lines) {
            int lineWidth = this.font.width(line);
            int x = pos.getX();
            if (text.getAlign() == TextAlignment.CENTER) {
                int textWidth = Math.round(lineWidth * scale);
                x -= textWidth / 2;
            } else if (text.getAlign() == TextAlignment.RIGHT) {
                int textWidth = Math.round(lineWidth * scale);
                x -= textWidth;
            }

            if (text.getScale() == 1) {
                guiGraphics.text(font, line, x, y, color, false);
            } else {
                guiGraphics.pose().pushMatrix();
                guiGraphics.pose().translate(x, y);
                guiGraphics.pose().scale(scale);
                guiGraphics.text(
                        font,
                        line,
                        0,
                        0,
                        color,
                        false);
                guiGraphics.pose().popMatrix();
            }
            y += text.getScale() * this.font.lineHeight;
        }
    }

    public void drawFG(GuiGraphicsExtractor guiGraphics, int offsetX, int offsetY, int mouseX, int mouseY) {
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        this.drawBG(graphics, leftPos, topPos, mouseX, mouseY, a);

        widgets.drawBackgroundLayer(graphics, getBounds(true), new Point(mouseX - leftPos, mouseY - topPos));

        for (Slot slot : this.getInventorySlots()) {
            if (slot instanceof IOptionalSlot) {
                drawOptionalSlotBackground(graphics, (IOptionalSlot) slot, false);
            }
        }

        // TODO 26.1 - Mithi83 - double-check the order of rendering in here
        super.extractContents(graphics, mouseX, mouseY, a);

        graphics.nextStratum();
    }

    private void drawOptionalSlotBackground(GuiGraphicsExtractor guiGraphics, IOptionalSlot slot, boolean alwaysDraw) {
        // If a slot is optional and doesn't currently render, we still need to provide a background for it
        if (alwaysDraw || slot.isRenderDisabled()) {
            // If the slot is disabled, shade the background overlay
            float alpha = slot.isSlotEnabled() ? 1.0f : 0.2f;

            Point pos = slot.getBackgroundPos();

            Blitter.icon(Icon.SLOT_BACKGROUND)
                    .dest(leftPos + pos.getX(), topPos + pos.getY())
                    .color(1, 1, 1, alpha)
                    .blit(guiGraphics);
        }
    }

    // Convert global mouse x,y to relative Point
    private Point getMousePoint(double x, double y) {
        return new Point((int) Math.round(x - leftPos), (int) Math.round(y - topPos));
    }

    private boolean focusChangedToSomething = false;

    @Override
    public void setFocused(@Nullable GuiEventListener listener) {
        if (listener != null) {
            this.focusChangedToSomething = true;
        }
        super.setFocused(listener);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double deltaX, double deltaY) {
        if (deltaY != 0 && widgets.onMouseWheel(getMousePoint(x, y), deltaY)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (desktopHosted) {
            return desktopMouseClicked(event, doubleClick);
        }
        this.drag_click.clear();

        // Handler for Pick Block mouse click in Survival mode
        // (Key presses & Creative clicks all reach slotClicked handler as CLONE slot clicks)
        var pickBlockButton = InputConstants.Type.MOUSE.getOrCreate(event.button());
        if (getMinecraft().options.keyPickItem.isActiveAndMatches(pickBlockButton)
                && handlePickBlock(this.getHoveredSlot(event.x(), event.y()))) {
            return true;
        }

        // Forward right-clicks as-if they were left-clicks
        if (event.button() == InputConstants.MOUSE_BUTTON_RIGHT) {
            handlingRightClick = true;
            try {
                for (var widget : this.children()) {
                    if (widget.isMouseOver(event.x(), event.y())) {
                        return super.mouseClicked(new MouseButtonEvent(
                                event.x(), event.y(),
                                new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, event.buttonInfo().modifiers())),
                                doubleClick);
                    }
                }
            } finally {
                handlingRightClick = false;
            }
        }

        if (widgets.onMouseDown(getMousePoint(event.x(), event.y()), event.button())) {
            return true;
        }

        // super.mouseClicked will always return true, so we try to capture if
        // anything received focus due to the mouse click (see setFocused override)
        focusChangedToSomething = false;
        var result = super.mouseClicked(event, doubleClick);
        if (!focusChangedToSomething) {
            var currentFocus = getCurrentFocusPath();
            if (currentFocus != null) {
                currentFocus.applyFocus(false);
            }
        }
        return result;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (widgets.onMouseUp(getMousePoint(event.x(), event.y()), event.button())) {
            return true;
        }

        if (desktopHosted) {
            var focused = getFocused();
            setDragging(false);
            drag_click.clear();
            return focused != null && focused.mouseReleased(event);
        }
        return super.mouseReleased(event);
    }

    /**
     * Called when the player uses the Pick Block binding (mouse button or key) while hovering the given slot.
     *
     * @return whether the event was handled and should not be processed further
     */
    protected boolean handlePickBlock(@Nullable Slot slot) {
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        final Slot slot = desktopHosted ? desktopSlotAt(event.x(), event.y())
                : this.getHoveredSlot(event.x(), event.y());
        var itemstack = getMenu().getCarried();

        Point mousePos = new Point((int) Math.round(event.x() - leftPos), (int) Math.round(event.y() - topPos));
        if (widgets.onMouseDrag(mousePos, event.button())) {
            return true;
        }

        if (slot instanceof FakeSlot && !itemstack.isEmpty()) {
            this.drag_click.add(slot);
            if (this.drag_click.size() > 1) {
                for (Slot dr : this.drag_click) {
                    var p = new InventoryActionPacket(
                            event.button() == InputConstants.MOUSE_BUTTON_LEFT ? InventoryAction.PICKUP_OR_SET_DOWN
                                    : InventoryAction.PLACE_SINGLE,
                            dr.index, 0);
                    MenuPacketScope.send(p);
                }
            }

            return true;
        } else {
            if (desktopHosted) {
                return getFocused() != null && getFocused().mouseDragged(event, dragX, dragY);
            }
            return super.mouseDragged(event, dragX, dragY);
        }
    }

    @Override
    protected void slotClicked(@Nullable Slot slot, int slotIdx, int mouseButton, ContainerInput clickType) {

        // Do not further process clicks on client-only slots
        if (getMenu().isClientSideSlot(slot)) {
            return;
        }

        // Do not allow clicks on disabled player inventory slots
        if (slot instanceof DisabledSlot) {
            return;
        }

        if (clickType == ContainerInput.CLONE) {
            // Pick Block bound to a keyboard key, or to a mouse button in Creative
            if (handlePickBlock(slot)) {
                return;
            }

            // Cloning would empty a FakeSlot
            if (slot instanceof FakeSlot) {
                return;
            }

            // Wrapped stacks (fluids, etc.) cannot be cloned into the player's hand
            if (slot != null && GenericStack.isWrapped(slot.getItem())) {
                return;
            }
        }

        if (this.drag_click.size() <= 1
                && mouseButton == 1 // Right-click (container click numbering)
                && getEmptyingAction(slot, menu.getCarried()) != null) {
            var p = new InventoryActionPacket(InventoryAction.EMPTY_ITEM, slotIdx, 0);
            MenuPacketScope.send(p);
            return;
        }

        if (slot instanceof FakeSlot) {
            if (this.drag_click.size() > 1) {
                return;
            }

            var action = mouseButton == 1 ? InventoryAction.SPLIT_OR_PLACE_SINGLE
                    : InventoryAction.PICKUP_OR_SET_DOWN;
            var p = new InventoryActionPacket(action, slotIdx, 0);
            MenuPacketScope.send(p);
            return;
        }

        if (slot instanceof CraftingTermSlot) {
            InventoryAction action;
            if (getMinecraft().hasShiftDown()) {
                action = InventoryAction.CRAFT_SHIFT;
            } else if (InputConstants.isKeyDown(InputConstants.KEY_SPACE)) {
                action = InventoryAction.CRAFT_ALL;
            } else {
                // Craft stack on right-click, craft single on left-click
                action = mouseButton == 1 ? InventoryAction.CRAFT_STACK : InventoryAction.CRAFT_ITEM;
            }

            final InventoryActionPacket p = new InventoryActionPacket(action, slotIdx, 0);
            MenuPacketScope.send(p);

            return;
        }

        if (slot != null && InputConstants.isKeyDown(InputConstants.KEY_SPACE)) {
            int slotNum = slot.index;
            final InventoryActionPacket p = new InventoryActionPacket(InventoryAction.MOVE_REGION, slotNum, 0);
            MenuPacketScope.send(p);
            return;
        }

        if (slot != null && !this.disableShiftClick && getMinecraft().hasShiftDown() && mouseButton == 0) {
            this.disableShiftClick = true;

            if (this.dbl_whichItem.isEmpty() || this.bl_clicked != slot
                    || this.dbl_clickTimer.elapsed(TimeUnit.MILLISECONDS) > 250) {
                // some simple double click logic.
                this.bl_clicked = slot;
                this.dbl_clickTimer = Stopwatch.createStarted();
                this.dbl_whichItem = slot.hasItem() ? slot.getItem().copy() : ItemStack.EMPTY;
            } else if (!this.dbl_whichItem.isEmpty()) {
                // a replica of the weird broken vanilla feature.

                final List<Slot> slots = this.getInventorySlots();
                for (Slot inventorySlot : slots) {
                    if (inventorySlot != null && inventorySlot.mayPickup(getPlayer()) && inventorySlot.hasItem()
                            && isSameInventory(inventorySlot, slot)
                            && AbstractContainerMenu.canItemQuickReplace(inventorySlot, this.dbl_whichItem, true)) {
                        this.slotClicked(inventorySlot, inventorySlot.index, 0, ContainerInput.QUICK_MOVE);
                    }
                }
                this.dbl_whichItem = ItemStack.EMPTY;
            }

            this.disableShiftClick = false;
        }

        if (desktopHosted) {
            if (desktopActions != null) {
                desktopActions.clickSlot(slotIdx, mouseButton, clickType);
            }
        } else {
            super.slotClicked(slot, slotIdx, mouseButton, clickType);
        }
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int screenX, int screenY) {
        // Consider clicks within attached compound widgets as still inside the screen
        var mousePos = new Point((int) Math.round(mouseX - screenX), (int) Math.round(mouseY - screenY));
        if (widgets.hitTest(mousePos)) {
            return false;
        }

        return super.hasClickedOutside(mouseX, mouseY, screenX, screenY);
    }

    protected LocalPlayer getPlayer() {
        // Our UIs are usually not opened when not in-game, so this should not be a
        // problem
        return Objects.requireNonNull(getMinecraft().player);
    }

    @Override
    protected boolean checkHotbarKeyPressed(KeyEvent event) {
        final Slot theSlot = this.getSlotUnderMouse();

        if (getMenu().getCarried().isEmpty() && theSlot != null) {
            if (this.minecraft.options.keySwapOffhand.matches(event)) {
                this.slotClicked(theSlot, theSlot.index, Inventory.SLOT_OFFHAND, ContainerInput.SWAP);
                return true;
            }

            for (int j = 0; j < 9; ++j) {
                if (getMinecraft().options.keyHotbarSlots[j].matches(event)) {
                    final List<Slot> slots = this.getInventorySlots();
                    for (Slot s : slots) {
                        if (s.getSlotIndex() == j && s.container == this.menu.getPlayerInventory()
                                && !s.mayPickup(this.menu.getPlayerInventory().player)) {
                            return false;
                        }
                    }

                    if (theSlot.getMaxStackSize() == 64) {
                        this.slotClicked(theSlot, theSlot.index, j, ContainerInput.SWAP);
                        return true;
                    } else {
                        for (Slot s : slots) {
                            if (s.getSlotIndex() == j
                                    && s.container == this.menu.getPlayerInventory()) {
                                ServerboundPacket message = new SwapSlotsPacket(s.index, theSlot.index);
                                MenuPacketScope.send(message);
                                return true;
                            }
                        }
                    }
                }
            }
        }

        return false;
    }

    @Override
    protected boolean isHovering(Slot slot, double x, double y) {
        if (slot instanceof ResizableSlot resizableSlot) {
            var width = resizableSlot.getWidth();
            var height = resizableSlot.getHeight();
            return this.isHovering(slot.x, slot.y, width, height, x, y);
        }
        return super.isHovering(slot, x, y);
    }

    public void drawBG(GuiGraphicsExtractor guiGraphics, int offsetX, int offsetY, int mouseX, int mouseY,
            float partialTicks) {
        if (desktopHosted && !usesAeDesktopLayout()) {
            return;
        }

        guiGraphics.nextStratum();

        var generatedBackground = style.getGeneratedBackground();
        if (generatedBackground != null) {
            BackgroundGenerator.draw(
                    generatedBackground.getWidth(),
                    generatedBackground.getHeight(),
                    guiGraphics,
                    offsetX,
                    offsetY);
        }

        var background = style.getBackground();
        if (background != null) {
            background.dest(offsetX, offsetY).blit(guiGraphics);
        }

    }

    public void drawItem(GuiGraphicsExtractor guiGraphics, int x, int y, ItemStack is) {
        guiGraphics.item(is, x, y);
        guiGraphics.itemDecorations(font, is, x, y);
    }

    protected Component getGuiDisplayName(Component in) {
        return title.getString().isEmpty() ? in : title;
    }

    /**
     * This overrides the base-class method through some access transformer hackery...
     */
    @Override
    public void extractSlot(GuiGraphicsExtractor guiGraphics, Slot s, int mouseX, int mouseY) {
        if (s instanceof AppEngSlot appEngSlot) {
            try {
                renderAppEngSlot(guiGraphics, appEngSlot, mouseX, mouseY);
            } catch (Exception err) {
                AELog.warn("[AppEng] AE prevented crash while drawing slot: " + err);
            }
        } else {
            super.extractSlot(guiGraphics, s, mouseX, mouseY);
        }
    }

    private void renderAppEngSlot(GuiGraphicsExtractor guiGraphics, AppEngSlot s, int mouseX, int mouseY) {
        var is = s.getItem();
        drawAppEngSlotArtwork(guiGraphics, s, is);
        super.extractSlot(guiGraphics, s, mouseX, mouseY);
    }

    private void drawAppEngSlotArtwork(GuiGraphicsExtractor guiGraphics, AppEngSlot s, ItemStack is) {

        // If the slot has a background icon, render it, but only if the slot is empty
        // or it requests the icon to be always drawn
        if ((s.renderIconWithItem() || is.isEmpty()) && s.isSlotEnabled() && s.getIcon() != null) {
            Blitter.icon(s.getIcon())
                    .dest(s.x, s.y)
                    .opacity(s.getOpacityOfIcon())
                    .blit(guiGraphics);
        }

        // Draw a red background for slots that are in an invalid state
        if (!s.isValid()) {
            guiGraphics.fill(s.x, s.y, 16 + s.x, 16 + s.y, 0x66ff6666);
        }

    }

    @Override
    public void containerTick() {
        super.containerTick();

        widgets.tick();

        for (var child : children()) {
            if (child instanceof ITickingWidget) {
                ((ITickingWidget) child).tick();
            }
        }
    }

    /**
     * Returns true while the current event being handled is a click of the right mouse button.
     */
    public boolean isHandlingRightClick() {
        return handlingRightClick;
    }

    /**
     * Adds a button to the vertical toolbar to the left of the screen and returns that button to the caller. The button
     * will automatically be positioned. This button will automatically be re-added to the screen when it's resized.
     */
    protected final <B extends Button> B addToLeftToolbar(B button) {
        verticalToolbar.add(button);
        return button;
    }

    /**
     * Returns rectangles in UI-space that define areas of the screen occluded by this GUI, in addition to the rectangle
     * defined by [guiLeft, guiTop, xSize, ySize], which is assumed to be occluded. This is used for moving JEI items
     * out of the way.
     */
    public List<Rect2i> getExclusionZones() {
        List<Rect2i> result = new ArrayList<>(2);
        widgets.addExclusionZones(result, getBounds(true));
        return result;
    }

    protected void fillRect(GuiGraphicsExtractor guiGraphics, Rect2i rect, int color) {
        guiGraphics.fill(rect.getX(), rect.getY(), rect.getX() + rect.getWidth(), rect.getY() + rect.getHeight(),
                color);
    }

    private TextOverride getOrCreateTextOverride(String id) {
        return textOverrides.computeIfAbsent(id, x -> new TextOverride());
    }

    /**
     * Hides (or shows) a text that is defined in this screen's style file.
     */
    protected final void setTextHidden(String id, boolean hidden) {
        getOrCreateTextOverride(id).setHidden(hidden);
    }

    /**
     * Hides (or shows) a group of slots based on semantic.
     */
    public final void setSlotsHidden(SlotSemantic semantic, boolean hidden) {
        if (hidden) {
            if (hiddenSlots.add(semantic)) {
                // This isn't the greatest tactic but allows us to do this for every slot-type.
                // This approach has been used to hide slots since 1.7
                for (Slot slot : menu.getSlots(semantic)) {
                    slot.x = HIDDEN_SLOT_POS.getX();
                    slot.y = HIDDEN_SLOT_POS.getY();
                }
            }
        } else if (hiddenSlots.remove(semantic) && style != null) {
            positionSlots();
        }
    }

    /**
     * Changes the text that will be displayed for a text defined in this screen's style file.
     */
    protected final void setTextContent(String id, Component content) {
        getOrCreateTextOverride(id).setContent(content);
    }

    public ScreenStyle getStyle() {
        return style;
    }

    /**
     * Return the key of the resource if there is one at the given window coordinates. This is used by JEI/REI to allow
     * the U and R hotkeys to work for ingredients (such as fluids) that are not stored in normal slots, and for
     * ingredients wrapped in {@link appeng.items.misc.WrappedGenericStack}.
     * <p/>
     * The given coordinates are in window space.
     */
    @Nullable
    public StackWithBounds getStackUnderMouse(double mouseX, double mouseY) {
        // First check the vanilla slots
        if (hoveredSlot != null) {
            return StackWithBounds.fromSlot(this, hoveredSlot);
        }

        return null;
    }

    public final int getGuiLeft() {
        return this.leftPos;
    }

    public final int getGuiTop() {
        return this.topPos;
    }

    public final Minecraft getMinecraft() {
        return minecraft;
    }

    public final Slot getSlotUnderMouse() {
        return hoveredSlot;
    }

    public static boolean isSameInventory(Slot a, Slot b) {
        if (a instanceof AppEngSlot appEngSlotA && b instanceof AppEngSlot appEngSlotB) {
            return appEngSlotA.container == appEngSlotB.container;
        }
        return a.container == b.container;
    }

    /**
     * Renders a highlight for the given slot to indicate the mouse is currently hovering over it.
     */
    // TODO 1.21.8 Unused
    protected void renderSlotHighlight(GuiGraphicsExtractor guiGraphics, Slot slot, int mouseX, int mouseY,
            float partialTick) {
        if (!slot.isHighlightable()) {
            return;
        }

        int x = slot.x;
        int y = slot.y;
        int w, h;
        if (slot instanceof ResizableSlot resizableSlot) {
            w = resizableSlot.getWidth();
            h = resizableSlot.getHeight();
        } else {
            w = 16;
            h = 16;
        }

        // Same as the Vanilla method, just with dynamic width and height
        // Added a custom slot highlight effect - RID
        guiGraphics.horizontalLine(x, x + w, y - 1, 0xFFdaffff);
        guiGraphics.horizontalLine(x - 1, x + w, y + h, 0xFFdaffff);
        guiGraphics.verticalLine(x - 1, y - 2, y + h, 0xFFdaffff);
        guiGraphics.verticalLine(x + w, y - 2, y + h, 0xFFdaffff);
        guiGraphics.fillGradient(x, y, x + w, y + h, 0x669cd3ff, 0x669cd3ff);
    }

    public final void switchToScreen(AEBaseScreen<?> screen) {
        savedSlotInfos.clear();
        for (var slot : menu.slots) {
            savedSlotInfos.add(new SavedSlotInfo(slot));
            // Hide all slots and give the sub-screen a chance to reposition whatever slots it wants
            slot.x = HIDDEN_SLOT_POS.getX();
            slot.y = HIDDEN_SLOT_POS.getY();
        }

        if (desktopHosted) {
            if (desktopNavigation == null) {
                throw new IllegalStateException("Desktop navigation is unavailable");
            }
            desktopNavigation.accept(screen);
        } else {
            minecraft.gui.screen = null;
            minecraft.gui.setScreen(screen);
        }

        if (!screen.savedSlotInfos.isEmpty()) {
            // Restore slot state to that of the new screen
            for (var savedSlotInfo : screen.savedSlotInfos) {
                savedSlotInfo.restore();
            }
            screen.savedSlotInfos.clear();
        }
    }

    /**
     * Called when a sub-screen returns to this screen.
     */
    protected <P extends AEBaseScreen<T>> void onReturnFromSubScreen(AESubScreen<T, P> subScreen) {
    }

    record SavedSlotInfo(Slot slot, boolean active, int x, int y) {
        public SavedSlotInfo(Slot slot) {
            this(slot, slot.isActive(), slot.x, slot.y);
        }

        public void restore() {
            if (slot instanceof AppEngSlot appEngSlot) {
                appEngSlot.setActive(active);
            }
            slot.x = x;
            slot.y = y;
        }
    }

    protected void openHelp() {
        var topic = getHelpTopic();
        if (topic != null) {
            GuidesCommon.openGuide(getPlayer(), GuideItem.GUIDE_ID, topic);
        } else {
            LOG.warn("No topic assigned to screen {}, but button was clicked", this);
        }
    }

    @Nullable
    protected PageAnchor getHelpTopic() {
        // Help topic may be overridden via screen style
        String helpTopic = style.getHelpTopic();
        if (helpTopic != null) {
            var sep = helpTopic.indexOf('#');
            String fragment = null;
            if (sep != -1) {
                fragment = helpTopic.substring(sep + 1);
                helpTopic = helpTopic.substring(0, sep);
            }
            try {
                return new PageAnchor(IdUtils.resolveId(helpTopic, AppEng.MOD_ID), fragment);
            } catch (Exception e) {
                LOG.warn("Invalid helpTopic for screen {}: {}", this, helpTopic);
            }
        }

        // Try finding the help topic automatically via the guidebook item index
        var guide = AppEngClient.instance().getGuide();
        var itemIndex = guide.getIndex(ItemIndex.class);

        Object target = getMenu().getTarget();
        if (target instanceof BlockEntity be) {
            var block = be.getBlockState().getBlock();
            var blockId = BuiltInRegistries.BLOCK.getKey(block);
            return itemIndex.get(blockId);
        } else if (target instanceof IPart part) {
            var item = part.getPartItem().asItem();
            var itemId = BuiltInRegistries.ITEM.getKey(item);
            return itemIndex.get(itemId);
        } else if (target instanceof ItemMenuHost<?> menuHost) {
            var item = menuHost.getItem();
            var itemId = BuiltInRegistries.ITEM.getKey(item);
            return itemIndex.get(itemId);
        }
        return null;
    }
}
