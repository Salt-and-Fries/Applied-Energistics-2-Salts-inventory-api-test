/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2021, TeamAppliedEnergistics, All rights reserved.
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.google.common.base.Preconditions;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button.OnPress;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;

import appeng.client.Point;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.style.WidgetStyle;
import appeng.client.gui.widgets.AE2Button;
import appeng.client.gui.widgets.AECheckbox;
import appeng.client.gui.widgets.AETextField;
import appeng.client.gui.widgets.BackgroundPanel;
import appeng.client.gui.widgets.IResizableWidget;
import appeng.client.gui.widgets.IconButton;
import appeng.client.gui.widgets.NumberEntryWidget;
import appeng.client.gui.widgets.Scrollbar;
import appeng.client.gui.widgets.TabButton;
import appeng.client.gui.widgets.VerticalButtonBar;
import appeng.core.localization.GuiText;
import appeng.core.network.ServerboundPacket;
import appeng.core.network.serverbound.SwitchGuisPacket;
import appeng.menu.MenuPacketScope;
import appeng.menu.implementations.PriorityMenu;
import appeng.util.Icon;

/**
 * This utility class helps with positioning commonly used Minecraft {@link AbstractWidget} instances on a screen
 * without having to recreate them everytime the screen resizes in the <code>init</code> method.
 * <p/>
 * This class sources the positioning and sizing for widgets from the {@link ScreenStyle}, and correlates between the
 * screen's JSON file and the widget using a string id.
 */
public class WidgetContainer {
    private final ScreenStyle style;
    private final Map<String, AbstractWidget> widgets = new LinkedHashMap<>();
    private final Map<String, ICompositeWidget> compositeWidgets = new LinkedHashMap<>();
    private final Map<String, ResolvedTooltipArea> tooltips = new LinkedHashMap<>();
    private final Map<String, TabButton> desktopPanelTabs = new LinkedHashMap<>();
    private final Map<String, Rect2i> desktopPanels = new LinkedHashMap<>();
    private final java.util.Set<String> desktopCompactPanels = new java.util.HashSet<>();
    private final java.util.Set<AbstractWidget> desktopExternalButtons = new java.util.HashSet<>();
    private final java.util.Set<String> desktopExternalComposites = new java.util.HashSet<>();
    private final java.util.Set<String> desktopHiddenAePanels = new java.util.HashSet<>();
    private AEBaseScreen<?> desktopScreen;
    private String desktopOpenPanel;

    public int desktopVisibilityStamp() {
        int stamp = 1;
        for (var button : widgets.values()) {
            stamp = 31 * stamp + (button.visible ? 1 : 0);
        }
        for (var widget : compositeWidgets.values()) {
            stamp = 31 * stamp + (widget.isVisible() ? 1 : 0);
            if (widget instanceof VerticalButtonBar toolbar) {
                for (var button : toolbar.buttons()) {
                    stamp = 31 * stamp + (button.visible ? 1 : 0);
                }
            }
        }
        if (desktopScreen != null) {
            for (String id : List.of("upgrades", "viewCells", "toolbox")) {
                for (var slot : desktopScreen.getMenu().getSlots(panelSemantic(id))) {
                    stamp = 31 * stamp + (slot.isActive()
                            && (!(slot instanceof appeng.menu.slot.AppEngSlot ae) || ae.isSlotEnabled()) ? 1 : 0);
                }
            }
        }
        return stamp;
    }

    private static appeng.menu.SlotSemantic panelSemantic(String id) {
        return switch (id) {
            case "upgrades" -> appeng.menu.SlotSemantics.UPGRADE;
            case "viewCells" -> appeng.menu.SlotSemantics.VIEW_CELL;
            case "toolbox" -> appeng.menu.SlotSemantics.TOOLBOX;
            default -> null;
        };
    }

    private boolean desktopShown(String id, ICompositeWidget widget) {
        return widget.isVisible() && !desktopHiddenAePanels.contains(id)
                && (desktopScreen == null || desktopScreen.usesAeDesktopLayout()
                        || panelSemantic(id) == null);
    }

    private List<ICompositeWidget> visibleCompositeWidgets() {
        return compositeWidgets.entrySet().stream().filter(e -> desktopShown(e.getKey(), e.getValue()))
                .map(Map.Entry::getValue).toList();
    }

    public WidgetContainer(ScreenStyle style) {
        this.style = style;
    }

    public void add(String id, AbstractWidget widget) {
        Preconditions.checkState(!compositeWidgets.containsKey(id), "%s already used for composite widget", id);

        // Size the widget, as this doesn't change when the parent is resized
        WidgetStyle widgetStyle = style.getWidget(id);
        int width = widgetStyle.getWidth() != 0 ? widgetStyle.getWidth() : widget.getWidth();
        int height = widgetStyle.getHeight() != 0 ? widgetStyle.getHeight() : widget.getHeight();
        if (widget instanceof IResizableWidget resizableWidget) {
            resizableWidget.resize(width, height);
        } else {
            widget.setWidth(width);
            widget.setHeight(height);
        }

        if (widget instanceof TabButton tabButton) {
            if (widgetStyle.isHideEdge()) {
                tabButton.setStyle(TabButton.Style.CORNER);
            }
        }

        if (widgets.put(id, widget) != null) {
            throw new IllegalStateException("Duplicate id: " + id);
        }
    }

    public void add(String id, ICompositeWidget widget) {
        Preconditions.checkState(!widgets.containsKey(id), "%s already used for widget", id);

        // Size the widget, as this doesn't change when the parent is resized
        WidgetStyle widgetStyle = style.getWidget(id);
        widget.setSize(widgetStyle.getWidth(), widgetStyle.getHeight());

        if (compositeWidgets.put(id, widget) != null) {
            throw new IllegalStateException("Duplicate id: " + id);
        }
    }

    /**
     * Convenient way to add Vanilla buttons without having to specify x,y,width and height. The actual
     * position/rectangle is instead sourced from the screen style.
     */
    public AE2Button addButton(String id, Component text, OnPress action) {
        var button = new AE2Button(text, action);
        add(id, button);
        return button;
    }

    public AE2Button addButton(String id, Component text, Runnable action) {
        return addButton(id, text, btn -> action.run());
    }

    public AECheckbox addCheckbox(String id, Component text, Runnable changeListener) {
        var checkbox = new AECheckbox(0, 0, 0, AECheckbox.SIZE, style, text);
        add(id, checkbox);
        checkbox.setChangeListener(changeListener);
        return checkbox;
    }

    public NumberEntryWidget addNumberEntryWidget(String id, NumberEntryType type) {
        var numberEntry = new NumberEntryWidget(style, type);
        add(id, numberEntry);
        return numberEntry;
    }

    /**
     * Adds a {@link Scrollbar} to the screen.
     */
    public Scrollbar addScrollBar(String id) {
        return addScrollBar(id, Scrollbar.DEFAULT);
    }

    /**
     * Adds a {@link Scrollbar} to the screen.
     */
    public Scrollbar addScrollBar(String id, Scrollbar.Style style) {
        Scrollbar scrollbar = new Scrollbar(style);
        add(id, scrollbar);
        return scrollbar;
    }

    /**
     * Adds a panel to the screen, which takes its background from the style's "images" section, and it's position from
     * the widget section.
     *
     * @param id The id used to look up the background image and bounds in the style.
     */
    public void addBackgroundPanel(String id) {
        var background = style.getImage(id).copy();
        add(id, new BackgroundPanel(background));
    }

    void populateScreen(Consumer<AbstractWidget> addWidget, Rect2i bounds, AEBaseScreen<?> screen) {
        if (screen.isDesktopHosted() && screen.usesAeDesktopLayout()) {
            populateAeDesktop(addWidget, bounds, screen);
            return;
        }
        Consumer<AbstractWidget> leafWidget = widget -> {
            if (screen.isDesktopHosted() && widget instanceof IconButton button) {
                button.setDesktopControl();
            }
            if (screen.isDesktopHosted() && widget instanceof AE2Button button) {
                button.setDesktop(true);
            }
            if (screen.isDesktopHosted() && widget instanceof appeng.client.gui.widgets.ProgressBar progress) {
                progress.setDesktop(true);
            }
            addWidget.accept(widget);
        };
        if (screen.isDesktopHosted()) {
            desktopScreen = screen;
            desktopExternalButtons.clear();
            desktopPanels.clear();
            desktopCompactPanels.clear();
        }
        int rightTab = 0;
        int backTabs = widgets.containsKey("back") ? 1 : 0;
        for (var entry : widgets.entrySet()) {
            var widget = entry.getValue();
            if (widget.isFocused()) {
                widget.setFocused(false); // Minecraft already cleared focus on the screen
            }

            // Position the widget
            WidgetStyle widgetStyle = style.getWidget(entry.getKey());
            Point pos = widgetStyle.resolve(bounds);
            if (screen.isDesktopHosted()) {
                var frame = screen.desktopFrame();
                if (widget instanceof AE2Button button) {
                    button.setDesktop(true);
                }
                if (widget instanceof TabButton tab) {
                    if (entry.getKey().startsWith("modeTabButton")) {
                        int index = Integer.parseInt(entry.getKey().substring("modeTabButton".length()));
                        tab.setDesktopEdge(DesktopChrome.Edge.TOP);
                        pos = new Point(frame.getX() + 4 + index * DesktopChrome.TAB_SIZE,
                                frame.getY() - DesktopChrome.TAB_SIZE + 1);
                    } else if (entry.getKey().equals("back")) {
                        tab.setDesktopEdge(DesktopChrome.Edge.LEFT);
                        pos = new Point(frame.getX() - DesktopChrome.TAB_SIZE + 1, frame.getY() + 24);
                    } else {
                        pos = positionDesktopRightTab(tab, frame, rightTab);
                        if (tab.visible) {
                            rightTab++;
                        }
                    }
                    desktopExternalButtons.add(tab);
                }
            }
            if (widget instanceof IResizableWidget resizableWidget) {
                resizableWidget.move(pos);
            } else {
                widget.setX(pos.getX());
                widget.setY(pos.getY());
            }

            leafWidget.accept(widget);
        }

        // For composite widgets, just position them. Positions for these widgets are generally relative to the dialog
        Rect2i relativeBounds = new Rect2i(0, 0, bounds.getWidth(), bounds.getHeight());
        for (var entry : compositeWidgets.entrySet()) {
            var widget = entry.getValue();
            var widgetStyle = style.getWidget(entry.getKey());
            var position = widgetStyle.resolve(relativeBounds);
            widget.setPosition(position);
            if (screen.isDesktopHosted() && panelSemantic(entry.getKey()) != null) {
                continue;
            }
            if (screen.isDesktopHosted() && widget instanceof Scrollbar scrollbar) {
                scrollbar.setDesktop(true);
            }
            widget.populateScreen(leafWidget, bounds, screen);
            if (screen.isDesktopHosted() && widget instanceof VerticalButtonBar toolbar) {
                toolbar.layoutDesktop(screen.desktopFrame(), backTabs);
                desktopExternalButtons.addAll(toolbar.buttons());
            }
        }

        if (screen.isDesktopHosted()) {
            layoutDesktopPanels(leafWidget, bounds, screen, rightTab);
        }

        tooltips.clear();
        for (var entry : style.getTooltips().entrySet()) {
            var pos = entry.getValue().resolve(relativeBounds);
            var area = new Rect2i(
                    pos.getX(), pos.getY(),
                    entry.getValue().getWidth(),
                    entry.getValue().getHeight());
            tooltips.put(entry.getKey(), new ResolvedTooltipArea(
                    area, new Tooltip(entry.getValue().getTooltip())));
        }
    }

    /** Keep the ordinary AE coordinate system, including bottom-anchored controls above the cut. */
    private void populateAeDesktop(Consumer<AbstractWidget> addWidget, Rect2i bounds, AEBaseScreen<?> screen) {
        desktopScreen = screen;
        desktopExternalButtons.clear();
        desktopExternalComposites.clear();
        desktopHiddenAePanels.clear();
        desktopPanels.clear();
        desktopCompactPanels.clear();
        var body = screen.desktopContentBounds();
        var relative = new Rect2i(0, 0, bounds.getWidth(), bounds.getHeight());
        Consumer<AbstractWidget> add = widget -> {
            if (widget instanceof appeng.client.gui.widgets.AECheckbox checkbox) {
                checkbox.setDesktopHosted(true);
            }
            addWidget.accept(widget);
            if (outsideBody(widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight(), body)) {
                desktopExternalButtons.add(widget);
            }
        };
        for (var entry : widgets.entrySet()) {
            var widget = entry.getValue();
            var point = style.getWidget(entry.getKey()).resolve(bounds);
            if (widget instanceof IResizableWidget resizable) {
                resizable.move(point);
            } else {
                widget.setX(point.getX());
                widget.setY(point.getY());
            }
            add.accept(widget);
        }
        tooltips.clear();
        for (var entry : style.getTooltips().entrySet()) {
            var point = entry.getValue().resolve(relative);
            tooltips.put(entry.getKey(), new ResolvedTooltipArea(
                    new Rect2i(point.getX(), point.getY(), entry.getValue().getWidth(), entry.getValue().getHeight()),
                    new Tooltip(entry.getValue().getTooltip())));
        }
        for (var entry : compositeWidgets.entrySet()) {
            var widget = entry.getValue();
            var position = style.getWidget(entry.getKey()).resolve(relative);
            widget.setPosition(position);
            var semantic = panelSemantic(entry.getKey());
            // Some ordinary AE styles deliberately park an unused panel far off-screen.
            // Keep that presentation hidden without creating new tabs or changing logical slot ids.
            if (semantic != null && (semantic == appeng.menu.SlotSemantics.TOOLBOX && screen.hasDesktopSharedToolbox()
                    || position.getX() < -bounds.getWidth() || position.getX() > 2 * bounds.getWidth())) {
                desktopHiddenAePanels.add(entry.getKey());
                for (var slot : screen.getMenu().getSlots(semantic)) {
                    slot.x = -9999;
                    slot.y = -9999;
                }
                continue;
            }
            widget.populateScreen(add, bounds, screen);
            if (widget instanceof VerticalButtonBar toolbar) {
                toolbar.layoutAeDesktop(body, screen.desktopToolbarLeft());
            }
            // Mode changes can expose previously inactive processing rows. Normalize their
            // positions and visibility before deriving external regions and resize constraints.
            // Tooltip areas must already exist because mode panels update their tooltip state.
            if (desktopShown(entry.getKey(), widget)) {
                widget.updateBeforeRender();
            }
            var rect = widget.getBounds();
            int x = bounds.getX() + rect.getX();
            int y = bounds.getY() + rect.getY();
            if (widget.isVisible() && outsideBody(x, y, rect.getWidth(), rect.getHeight(), body)) {
                desktopExternalComposites.add(entry.getKey());
                if (widget instanceof VerticalButtonBar toolbar) {
                    int columnIndex = 0;
                    for (var column : toolbar.desktopBackingColumns()) {
                        desktopPanels.put(entry.getKey() + "-" + columnIndex++, new Rect2i(
                                bounds.getX() + column.getX(), bounds.getY() + column.getY(),
                                column.getWidth(), column.getHeight()));
                    }
                } else {
                    desktopPanels.put(entry.getKey(), new Rect2i(x - 3, y - 2,
                            rect.getWidth() + 6, rect.getHeight() + 6));
                }
            }
            if (widget instanceof VerticalButtonBar toolbar) {
                desktopExternalButtons.addAll(toolbar.buttons());
            }
        }
        for (var slot : screen.desktopSlots()) {
            if (screen.desktopSlotVisible(slot) && screen.desktopExternalSlot(slot)) {
                int x = bounds.getX() + slot.x;
                int y = bounds.getY() + slot.y;
                if (desktopPanels.values().stream().noneMatch(rect -> x >= rect.getX() && y >= rect.getY()
                        && x + 16 <= rect.getX() + rect.getWidth() && y + 16 <= rect.getY() + rect.getHeight())) {
                    desktopPanels.put("slot-" + slot.index, new Rect2i(x - 1, y - 1, 18, 18));
                }
            }
        }
    }

    private static boolean outsideBody(int x, int y, int width, int height, Rect2i body) {
        return x < body.getX() || y < body.getY() || x + width > body.getX() + body.getWidth()
                || y + height > body.getY() + body.getHeight();
    }

    private void layoutDesktopPanels(Consumer<AbstractWidget> addWidget, Rect2i body, AEBaseScreen<?> screen,
            int firstTab) {
        var frame = screen.desktopFrame();
        int index = firstTab;
        for (String id : List.of("upgrades", "viewCells", "toolbox")) {
            var semantic = panelSemantic(id);
            var slots = screen.getMenu().getSlots(semantic);
            // Presentation-only hiding preserves logical slot ids and authoritative menu topology.
            for (var slot : slots) {
                slot.x = -9999;
                slot.y = -9999;
            }
            var visible = slots.stream().filter(s -> s.isActive()
                    && screen.desktopSemanticVisible(semantic)
                    && (!(s instanceof appeng.menu.slot.AppEngSlot ae) || ae.isSlotEnabled())).toList();
            if (visible.isEmpty()) {
                continue;
            }
            var tab = desktopPanelTabs.computeIfAbsent(id, key -> {
                var icon = switch (key) {
                    case "upgrades" -> appeng.core.definitions.AEItems.CAPACITY_CARD.stack();
                    case "viewCells" -> appeng.core.definitions.AEItems.VIEW_CELL.stack();
                    default -> appeng.core.definitions.AEItems.NETWORK_TOOL.stack();
                };
                var button = new TabButton(icon, Component.translatable("gui.ae2.DesktopPanel." + key), ignored -> {
                    desktopOpenPanel = key.equals(desktopOpenPanel) ? null : key;
                    screen.refreshDesktopLayout();
                });
                button.setDesktopEdge(DesktopChrome.Edge.RIGHT);
                return button;
            });
            tab.visible = true;
            tab.setSelected(id.equals(desktopOpenPanel));
            var position = positionDesktopRightTab(tab, frame, index++);
            tab.setX(position.getX());
            tab.setY(position.getY());
            addWidget.accept(tab);
            desktopExternalButtons.add(tab);
            if (!id.equals(desktopOpenPanel)) {
                continue;
            }
            int columns = id.equals("toolbox") ? 3 : (visible.size() + 7) / 8;
            int rows = id.equals("toolbox") ? (visible.size() + 2) / 3 : Math.min(8, visible.size());
            boolean bottom = tab.getY() >= frame.getY() + frame.getHeight() - 1;
            int captionWidth = Minecraft.getInstance().font.width(Component.translatable("gui.ae2.DesktopPanel." + id));
            int availableWidth = Math.max(26, Minecraft.getInstance().getWindow().getGuiScaledWidth()
                    - frame.getWidth() - 2 * DesktopChrome.TAB_SIZE - 16);
            if (bottom) {
                availableWidth = frame.getWidth() - 8;
            }
            boolean compact = captionWidth + 10 > availableWidth || columns * 18 + 8 > availableWidth;
            if (compact) {
                columns = Math.min(columns, Math.max(1, (availableWidth - 8) / 18));
                rows = (visible.size() + columns - 1) / columns;
                desktopCompactPanels.add(id);
            }
            int panelWidth = compact ? columns * 18 + 8 : Math.max(captionWidth + 10, columns * 18 + 8);
            int panelX = bottom ? tab.getX() + DesktopChrome.TAB_SIZE - panelWidth
                    : tab.getX() + DesktopChrome.TAB_SIZE - 1;
            int panelY = bottom ? tab.getY() + DesktopChrome.TAB_SIZE - 1 : tab.getY();
            var rect = new Rect2i(panelX, panelY, panelWidth, rows * 18 + (compact ? 8 : 24));
            desktopPanels.put(id, rect);
            for (int n = 0; n < visible.size(); n++) {
                int col = n % columns;
                int row = n / columns;
                visible.get(n).x = panelX - body.getX() + 5 + col * 18;
                visible.get(n).y = panelY - body.getY() + (compact ? 5 : 19) + row * 18;
            }
        }
    }

    private static Point positionDesktopRightTab(TabButton tab, Rect2i frame, int index) {
        int rows = Math.max(1, (frame.getHeight() - 24) / DesktopChrome.TAB_SIZE);
        if (index < rows) {
            tab.setDesktopEdge(DesktopChrome.Edge.RIGHT);
            return new Point(frame.getX() + frame.getWidth() - 1,
                    frame.getY() + 24 + index * DesktopChrome.TAB_SIZE);
        }
        tab.setDesktopEdge(DesktopChrome.Edge.BOTTOM);
        return new Point(frame.getX() + frame.getWidth() - 4 - (index - rows + 1) * DesktopChrome.TAB_SIZE,
                frame.getY() + frame.getHeight() - 1);
    }

    public boolean isDesktopExternalButton(Object widget) {
        return desktopExternalButtons.contains(widget);
    }

    public boolean isDesktopExternalSlot(net.minecraft.world.inventory.Slot slot) {
        return desktopScreen != null && List.of("upgrades", "viewCells", "toolbox").stream()
                .anyMatch(id -> desktopScreen.getMenu().getSlots(panelSemantic(id)).contains(slot));
    }

    public List<Rect2i> desktopExternalBounds() {
        var bounds = new java.util.ArrayList<Rect2i>();
        for (var button : desktopExternalButtons) {
            if (button.visible) {
                bounds.add(new Rect2i(button.getX(), button.getY(), button.getWidth(), button.getHeight()));
            }
        }
        bounds.addAll(desktopPanels.values());
        return bounds;
    }

    List<VerticalButtonBar> desktopToolbars() {
        return visibleCompositeWidgets().stream().filter(VerticalButtonBar.class::isInstance)
                .map(VerticalButtonBar.class::cast).toList();
    }

    public void drawDesktopAttachments(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (desktopScreen.usesAeDesktopLayout()) {
            var bounds = desktopScreen.getBounds(true);
            var mouse = new Point(mouseX - bounds.getX(), mouseY - bounds.getY());
            for (var id : desktopExternalComposites) {
                var widget = compositeWidgets.get(id);
                if (widget.isVisible()) {
                    widget.drawBackgroundLayer(graphics, bounds, mouse);
                }
            }
            for (var button : desktopExternalButtons) {
                if (button.visible) {
                    button.extractRenderState(graphics, mouseX, mouseY, 0);
                }
            }
            return;
        }
        for (var entry : desktopPanels.entrySet()) {
            DesktopChrome.panel(graphics, entry.getValue());
            if (!desktopCompactPanels.contains(entry.getKey())) {
                graphics.text(Minecraft.getInstance().font,
                        Component.translatable("gui.ae2.DesktopPanel." + entry.getKey()),
                        entry.getValue().getX() + 5, entry.getValue().getY() + 5, DesktopChrome.TEXT, false);
            }
        }
        for (var button : desktopExternalButtons) {
            if (button.visible) {
                button.extractRenderState(graphics, mouseX, mouseY, 0);
            }
        }
        for (var entry : desktopPanels.entrySet()) {
            var tab = desktopPanelTabs.get(entry.getKey());
            var panel = entry.getValue();
            if (panel.getY() > tab.getY()) {
                graphics.fill(tab.getX() + 4, panel.getY() - 1,
                        tab.getX() + DesktopChrome.TAB_SIZE - 4, panel.getY() + 2, DesktopChrome.SURFACE);
            } else {
                graphics.fill(panel.getX() - 1, tab.getY() + 4,
                        panel.getX() + 2, tab.getY() + DesktopChrome.TAB_SIZE - 4, DesktopChrome.SURFACE);
            }
        }
    }

    int[] desktopRailSize() {
        int width = 0;
        int height = 0;
        var upgrades = compositeWidgets.get("upgrades");
        var viewCells = compositeWidgets.get("viewCells");
        var toolbox = compositeWidgets.get("toolbox");
        if (upgrades != null && upgrades.isVisible()) {
            width = 4 + upgrades.getBounds().getWidth();
            height = upgrades.getBounds().getHeight();
        }
        if (viewCells != null && viewCells.isVisible()) {
            width = Math.max(width, 38 + viewCells.getBounds().getWidth());
            height = Math.max(height, viewCells.getBounds().getHeight());
        }
        if (toolbox != null && toolbox.isVisible()) {
            width = Math.max(width, 38 + toolbox.getBounds().getWidth());
            height = Math.max(height, toolbox.getBounds().getHeight()
                    + (viewCells == null ? 0 : viewCells.getBounds().getHeight() + 4));
        }
        return new int[] { width, height };
    }

    /** Refresh geometry after movement without adding duplicate listeners or discarding editing focus. */
    void populateDesktop(Consumer<AbstractWidget> addWidget, Rect2i bounds, AEBaseScreen<?> screen) {
        var focus = screen.getFocused();
        screen.clearDesktopWidgets();
        populateScreen(addWidget, bounds, screen);
        if (focus != null) {
            screen.setFocused(focus);
            focus.setFocused(true);
        }
    }

    public void cancelPointer() {
        for (var widget : compositeWidgets.values()) {
            widget.cancelPointer();
        }
    }

    void saveDesktopPreferences(net.minecraft.nbt.CompoundTag tag) {
        tag.putString("ae2.panel", desktopOpenPanel == null ? "" : desktopOpenPanel);
        widgets.forEach((id, widget) -> {
            if (widget instanceof EditBox box) {
                tag.putString("ae2.field." + id, box.getValue());
            }
        });
    }

    void loadDesktopPreferences(net.minecraft.nbt.CompoundTag tag) {
        String panel = tag.getStringOr("ae2.panel", "");
        desktopOpenPanel = panelSemantic(panel) == null ? null : panel;
        if (desktopScreen != null) {
            desktopScreen.refreshDesktopLayout();
        }
        widgets.forEach((id, widget) -> {
            if (widget instanceof EditBox box) {
                box.setValue(tag.getStringOr("ae2.field." + id, box.getValue()));
            }
        });
    }

    /**
     * Tick {@link ICompositeWidget} instances that are not automatically ticked as part of being a normal widget.
     */
    public void tick() {
        for (var widget : visibleCompositeWidgets()) {
            if (widget.isVisible()) {
                widget.tick();
            }
        }
    }

    /**
     * @see ICompositeWidget#updateBeforeRender()
     */
    public void updateBeforeRender() {
        for (var entry : compositeWidgets.entrySet()) {
            if (desktopShown(entry.getKey(), entry.getValue())) {
                entry.getValue().updateBeforeRender();
            }
        }
    }

    /**
     * @see ICompositeWidget#drawBackgroundLayer(GuiGraphicsExtractor, Rect2i, Point)
     */
    public void drawBackgroundLayer(GuiGraphicsExtractor guiGraphics, Rect2i bounds, Point mouse) {
        for (var entry : compositeWidgets.entrySet()) {
            if (desktopShown(entry.getKey(), entry.getValue())) {
                entry.getValue().drawBackgroundLayer(guiGraphics, bounds, mouse);
            }
        }
    }

    /**
     * @see ICompositeWidget#drawForegroundLayer(GuiGraphicsExtractor, Rect2i, Point)
     */
    public void drawForegroundLayer(GuiGraphicsExtractor poseStack, Rect2i bounds, Point mouse) {
        for (var widget : visibleCompositeWidgets()) {
            if (widget.isVisible()) {
                widget.drawForegroundLayer(poseStack, bounds, mouse);
            }
        }
    }

    /**
     * @see ICompositeWidget#onMouseDown(Point, int)
     */
    public boolean onMouseDown(Point mousePos, int btn) {
        for (var widget : visibleCompositeWidgets()) {
            if (widget.isVisible()
                    && (widget.wantsAllMouseDownEvents() || mousePos.isIn(widget.getBounds()))
                    && widget.onMouseDown(mousePos, btn)) {
                return true;
            }
        }

        return false;
    }

    /**
     * @see ICompositeWidget#onMouseUp(Point, int)
     */
    public boolean onMouseUp(Point mousePos, int btn) {
        for (var widget : visibleCompositeWidgets()) {
            if (widget.isVisible()
                    && (widget.wantsAllMouseUpEvents() || mousePos.isIn(widget.getBounds()))
                    && widget.onMouseUp(mousePos, btn)) {
                return true;
            }
        }

        return false;
    }

    /**
     * @see ICompositeWidget#onMouseDrag(Point, int)
     */
    public boolean onMouseDrag(Point mousePos, int btn) {
        for (var widget : visibleCompositeWidgets()) {
            if (widget.isVisible() && widget.onMouseDrag(mousePos, btn)) {
                return true;
            }
        }

        return false;
    }

    /**
     * @see ICompositeWidget#onMouseWheel(Point, double)
     */
    boolean onMouseWheel(Point mousePos, double wheelDelta) {
        // First pass: dispatch wheel event to widgets the mouse is over
        for (var widget : visibleCompositeWidgets()) {
            if (widget.isVisible()
                    && mousePos.isIn(widget.getBounds())
                    && widget.onMouseWheel(mousePos, wheelDelta)) {
                return true;
            }
        }

        // Second pass: send the event to capturing widgets
        for (var widget : visibleCompositeWidgets()) {
            if (widget.isVisible()
                    && widget.wantsAllMouseWheelEvents()
                    && widget.onMouseWheel(mousePos, wheelDelta)) {
                return true;
            }
        }

        return false;
    }

    /**
     * @see ICompositeWidget#addExclusionZones(List, Rect2i)
     */
    public void addExclusionZones(List<Rect2i> exclusionZones, Rect2i bounds) {
        for (var widget : visibleCompositeWidgets()) {
            if (widget.isVisible()) {
                widget.addExclusionZones(exclusionZones, bounds);
            }
        }
    }

    /**
     * Adds a button named "openPriority" that opens the priority GUI for the current menu host.
     */
    public void addOpenPriorityButton() {
        add("openPriority", new TabButton(Icon.PRIORITY, GuiText.Priority.text(),
                btn -> openPriorityGui()));
    }

    private void openPriorityGui() {
        ServerboundPacket message = SwitchGuisPacket.openSubMenu(PriorityMenu.TYPE);
        MenuPacketScope.send(message);
    }

    /**
     * Enables or disables a tooltip area that is defined in the widget styles.
     */
    public void setTooltipAreaEnabled(String id, boolean enabled) {
        var tooltip = tooltips.get(id);
        Preconditions.checkArgument(tooltip != null, "No tooltip with id '%s' is defined", id);
        tooltip.enabled = enabled;
    }

    @Nullable
    public Tooltip getTooltip(int mouseX, int mouseY) {
        for (var c : visibleCompositeWidgets()) {
            if (!c.isVisible()) {
                continue;
            }

            Rect2i bounds = c.getBounds();
            if (mouseX >= bounds.getX() && mouseX < bounds.getX() + bounds.getWidth()
                    && mouseY >= bounds.getY() && mouseY < bounds.getY() + bounds.getHeight()) {
                Tooltip tooltip = c.getTooltip(mouseX, mouseY);
                if (tooltip != null) {
                    return tooltip;
                }
            }
        }

        for (var tooltipArea : tooltips.values()) {
            if (tooltipArea.enabled && contains(tooltipArea.area, mouseX, mouseY)) {
                return tooltipArea.tooltip;
            }
        }

        return null;
    }

    /**
     * Check if there's any content or compound widget at the given screen-relative mouse position.
     */
    public boolean hitTest(Point mousePos) {
        for (var widget : visibleCompositeWidgets()) {
            if (mousePos.isIn(widget.getBounds())) {
                return true;
            }
        }
        return false;
    }

    // NOTE: Vanilla's implementation of Rect2i is broken since it uses less-than-equal to compare against x+width,
    // rather than less-than.
    private static boolean contains(Rect2i area, int mouseX, int mouseY) {
        return mouseX >= area.getX() && mouseX < area.getX() + area.getWidth()
                && mouseY >= area.getY() && mouseY < area.getY() + area.getHeight();
    }

    public AETextField addTextField(String id) {
        var searchField = new AETextField(style, Minecraft.getInstance().font,
                0, 0, 0, 0);
        searchField.setBordered(false);
        searchField.setMaxLength(25);
        searchField.setTextColor(0xFFFFFFFF);
        searchField.setVisible(true);
        add(id, searchField);
        return searchField;
    }

    private static class ResolvedTooltipArea {
        private final Rect2i area;
        private final Tooltip tooltip;
        private boolean enabled = true;

        public ResolvedTooltipArea(Rect2i area, Tooltip tooltip) {
            this.area = area;
            this.tooltip = tooltip;
        }
    }
}
