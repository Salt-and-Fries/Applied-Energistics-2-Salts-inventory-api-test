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

package appeng.client.gui.widgets;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.RenderPipelines;

import appeng.client.Point;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.ICompositeWidget;
import appeng.client.gui.style.Blitter;
import appeng.core.AppEng;

/**
 * A stacked button panel on the left or right side of our UIs.
 */
public class VerticalButtonBar implements ICompositeWidget {
    // Vertical space between buttons
    private static final int VERTICAL_SPACING = 6;
    // The margin between the right side of the buttons and the GUI
    private static final int MARGIN = 2;
    private final List<Button> buttons = new ArrayList<>();
    // The origin of the last initialized screen in window coordinates
    private Point screenOrigin = Point.ZERO;
    // This bounding rectangle relative to the screens origin
    private Rect2i bounds = new Rect2i(0, 0, 0, 0);
    private Point position;
    private boolean desktop;
    private boolean aeDesktop;
    private final List<Rect2i> desktopColumns = new ArrayList<>();

    public void layoutAeDesktop(Rect2i body) {
        layoutAeDesktop(body, 0);
    }

    public void layoutAeDesktop(Rect2i body, int left) {
        desktop = true;
        aeDesktop = true;
        desktopColumns.clear();
        var visible = buttons.stream().filter(button -> button.visible).toList();
        int rowHeight = visible.stream().mapToInt(button -> desktopPaintBounds(button).getHeight()).max().orElse(21)
                + 1;
        int columnWidth = visible.stream().mapToInt(button -> desktopPaintBounds(button).getWidth()).max().orElse(18)
                + 6;
        // Three pixels around the painted bevel, plus the four-pixel native footer.
        int capacity = Math.max(1, (body.getHeight() - 6) / rowHeight);
        for (int i = 0; i < visible.size(); i++) {
            var button = visible.get(i);
            button.setX(body.getX() + left - columnWidth + 5 - (i / capacity) * columnWidth);
            button.setY(body.getY() + 3 + (i % capacity) * rowHeight);
        }
        int backingHeight = Math.min(capacity, visible.size()) * rowHeight + 6;
        for (int column = 0; column * capacity < visible.size(); column++) {
            desktopColumns.add(new Rect2i(left + 1 - (column + 1) * columnWidth, 0,
                    columnWidth, backingHeight));
        }
        int columns = desktopColumns.size();
        bounds = columns == 0 ? new Rect2i(0, 0, 0, 0)
                : new Rect2i(left + 1 - columns * columnWidth, 0, columns * columnWidth,
                        backingHeight);
    }

    /** The native toolbar bevel includes pixels outside its 16-pixel input rectangle. */
    public static Rect2i desktopPaintBounds(Button button) {
        return new Rect2i(button.getX() - 1, button.getY(), Math.max(18, button.getWidth() + 2),
                Math.max(21, button.getHeight() + 5));
    }

    public List<Rect2i> desktopBackingColumns() {
        return List.copyOf(desktopColumns);
    }

    private static void backingPiece(GuiGraphicsExtractor graphics, Blitter sprite,
            int sx, int sy, int sw, int sh, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0)
            return;
        // Sprite pixels can be larger under a resource pack; the native GUI dimensions remain 21 x 26.
        int sourceX = sprite.getSrcX() + sx * sprite.getSrcWidth() / 21;
        int sourceY = sprite.getSrcY() + sy * sprite.getSrcHeight() / 26;
        int sourceRight = sprite.getSrcX() + (sx + sw) * sprite.getSrcWidth() / 21;
        int sourceBottom = sprite.getSrcY() + (sy + sh) * sprite.getSrcHeight() / 26;
        sprite.copy().src(sourceX, sourceY, sourceRight - sourceX, sourceBottom - sourceY)
                .dest(x, y, width, height).blit(graphics);
    }

    private void drawAeDesktopBacking(GuiGraphicsExtractor graphics, Rect2i screen) {
        var sprite = Blitter.guiSprite(AppEng.makeId("vertical_buttons_bg"));
        // All columns share the tallest column's footer, forming one rectangular backing.
        for (var column : desktopColumns) {
            int x = screen.getX() + column.getX(), y = screen.getY() + column.getY();
            backingPiece(graphics, sprite, 2, 2, 1, 1, x, y, column.getWidth(), column.getHeight());
            backingPiece(graphics, sprite, 5, 0, 1, 2, x, y, column.getWidth(), 2);
            backingPiece(graphics, sprite, 5, 22, 1, 4, x, y + column.getHeight() - 4, column.getWidth(), 4);
        }
        for (int i = desktopColumns.size() - 1; i >= 0; i--) {
            var column = desktopColumns.get(i);
            int x = screen.getX() + column.getX(), y = screen.getY() + column.getY();
            int exposedTop = i == desktopColumns.size() - 1 ? 2 : desktopColumns.get(i + 1).getHeight() - 4;
            int exposedHeight = column.getHeight() - 4 - exposedTop;
            if (exposedHeight > 0) {
                backingPiece(graphics, sprite, 0, 2, 2, 1, x, y + exposedTop, 2, exposedHeight);
                backingPiece(graphics, sprite, 0, 22, 2, 4, x, y + column.getHeight() - 4, 2, 4);
            }
            if (i == desktopColumns.size() - 1)
                backingPiece(graphics, sprite, 0, 0, 2, 2, x, y, 2, 2);
        }
    }

    public List<Button> buttons() {
        return List.copyOf(buttons);
    }

    public void layoutDesktop(Rect2i frame, int reservedTopTabs) {
        desktop = true;
        int row = reservedTopTabs;
        int leftRows = Math.max(1, (frame.getHeight() - 24) / appeng.client.gui.DesktopChrome.TAB_SIZE);
        for (var button : buttons) {
            if (!button.visible) {
                continue;
            }
            if (button instanceof IconButton icon) {
                icon.setDesktopEdge(row < leftRows ? appeng.client.gui.DesktopChrome.Edge.LEFT
                        : appeng.client.gui.DesktopChrome.Edge.BOTTOM);
            }
            if (row < leftRows) {
                button.setX(frame.getX() - appeng.client.gui.DesktopChrome.TAB_SIZE + 1);
                button.setY(frame.getY() + 24 + row * appeng.client.gui.DesktopChrome.TAB_SIZE);
            } else {
                button.setX(frame.getX() + 4 + (row - leftRows) * appeng.client.gui.DesktopChrome.TAB_SIZE);
                button.setY(frame.getY() + frame.getHeight() - 1);
            }
            row++;
        }
    }

    public VerticalButtonBar() {
    }

    public void add(Button button) {
        buttons.add(button);
    }

    public int preferredHeight() {
        return buttons.stream().filter(button -> button.visible)
                .mapToInt(button -> button.getHeight() + VERTICAL_SPACING).sum() + 2 * MARGIN;
    }

    @Override
    public void setPosition(Point position) {
        this.position = position;
    }

    @Override
    public void setSize(int width, int height) {
        // Setting the size for this control is not supported
    }

    @Override
    public Rect2i getBounds() {
        return bounds;
    }

    /**
     * We need to update every frame because buttons can become visible/invisible at any point in time.
     */
    @Override
    public void updateBeforeRender() {
        if (desktop) {
            return;
        }
        int currentY = position.getY() + MARGIN;
        int maxWidth = 0;

        // Align the button's right edge with the UI and account for margin
        for (Button button : buttons) {
            if (!button.visible) {
                continue;
            }

            // Vanilla widgets need to be in window space
            button.setX(screenOrigin.getX() + position.getX() - MARGIN - button.getWidth());
            button.setY(screenOrigin.getY() + currentY);

            currentY += button.getHeight() + VERTICAL_SPACING;
            maxWidth = Math.max(button.getWidth(), maxWidth);
        }

        // Set up a bounding rectangle for JEI exclusion zones
        if (maxWidth == 0) {
            bounds = new Rect2i(0, 0, 0, 0);
        } else {
            int boundX = position.getX() - maxWidth - 2 * MARGIN;
            int boundY = position.getY();
            bounds = new Rect2i(
                    boundX,
                    boundY,
                    maxWidth + 2 * MARGIN,
                    currentY - boundY);
        }
    }

    /**
     * Called when the parent UI is repositioned or resized. All buttons need to be re-added since Vanilla clears the
     * widgets when this happens.
     */
    @Override
    public void populateScreen(Consumer<AbstractWidget> addWidget, Rect2i bounds, AEBaseScreen<?> screen) {
        this.screenOrigin = Point.fromTopLeft(bounds);
        for (var button : this.buttons) {
            if (button.isFocused()) {
                button.setFocused(false);
            }
            addWidget.accept(button);
        }
    }

    @Override
    public void drawBackgroundLayer(GuiGraphicsExtractor guiGraphics, Rect2i bounds, Point mouse) {
        if (aeDesktop) {
            drawAeDesktopBacking(guiGraphics, bounds);
            return;
        }
        if (desktop) {
            return;
        }
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED,
                AppEng.makeId("vertical_buttons_bg"),
                bounds.getX() + this.bounds.getX() - 2,
                bounds.getY() + this.bounds.getY() - 1,
                this.bounds.getWidth() + 1,
                this.bounds.getHeight() + 4);
    }
}
