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

package appeng.client.gui.me.crafting;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.client.api.AEKeyRendering;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.StackWithBounds;
import appeng.client.gui.style.PaletteColor;

/**
 * Renders a 3x5 table where each cell displays an item and some text next to it.
 */
public abstract class AbstractTableRenderer<T> {

    private static final int CELL_WIDTH = 67;
    private static final int CELL_HEIGHT = 22;

    private static final int COLS = 3;

    // This border is only shown in-between cells, not around
    private static final int CELL_BORDER = 1;

    private static final int LINE_SPACING = 1;

    private static final float TEXT_SCALE = 0.5f;

    protected final AEBaseScreen<?> screen;
    private final Font fontRenderer;
    private int x;
    private int y;
    private int rows;
    private StackWithBounds hoveredStack;

    protected boolean readableRows() {
        return false;
    }

    public AbstractTableRenderer(AEBaseScreen<?> screen, int x, int y, int rows) {
        this.screen = screen;
        this.x = x;
        this.y = y;
        this.fontRenderer = Minecraft.getInstance().font;
        this.rows = rows;
    }

    public final void setLayout(int x, int y, int rows) {
        this.x = x;
        this.y = y;
        this.rows = Math.max(1, rows);
    }

    public final void render(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, List<T> entries,
            int scrollOffset) {
        int mouseXRelative = mouseX - screen.getGuiLeft();
        int mouseYRelative = mouseY - screen.getGuiTop();

        final int textColor = screen.getStyle().getColor(PaletteColor.DEFAULT_TEXT_COLOR).toARGB();
        List<Component> tooltipLines = null;
        StackWithBounds hovered = null;

        var pose = guiGraphics.pose();

        int columns = readableRows() ? 1 : COLS;
        int cellWidth = readableRows() ? 203 : CELL_WIDTH;
        int cellHeight = readableRows() ? 37 : CELL_HEIGHT;
        float scale = readableRows() ? 1 : TEXT_SCALE;
        float inverseScale = 1 / scale;
        if (readableRows()) {
            var nativeTable = screen.getStyle().getBackground();
            nativeTable.copy().src(nativeTable.getSrcX() + 15, nativeTable.getSrcY() + 25, 1, 1)
                    .dest(x, y, cellWidth, rows * (cellHeight + 1)).blit(guiGraphics);
            for (int row = 1; row < rows; row++) {
                nativeTable.copy().src(nativeTable.getSrcX() + 9, nativeTable.getSrcY() + 41, 1, 1)
                        .dest(x, y + row * (cellHeight + 1) - 1, cellWidth, 1).blit(guiGraphics);
            }
        }

        for (int row = 0; row < this.rows; row++) {
            for (int col = 0; col < columns; col++) {
                int i = (row + scrollOffset) * columns + col;
                if (i >= entries.size()) {
                    break;
                }

                T entry = entries.get(i);

                int cellX = x + col * (cellWidth + CELL_BORDER);
                int cellY = y + row * (cellHeight + CELL_BORDER);

                int background = getEntryBackgroundColor(entry);
                if (background != 0) {
                    guiGraphics.fill(cellX, cellY, cellX + cellWidth, cellY + cellHeight, background);
                }

                List<Component> lines = getEntryDescription(entry);

                // Compute the full height of the text block to center it vertically
                float textHeight = lines.size() * fontRenderer.lineHeight * scale;
                if (lines.size() > 1) {
                    textHeight += (lines.size() - 1) * LINE_SPACING;
                }
                float textY = Math.round(cellY + (cellHeight - textHeight) / 2.0f);

                // Position the item at the right side of the cell with a 3px margin
                int itemX = readableRows() ? cellX + 4 : cellX + cellWidth - 19;

                pose.pushMatrix();
                pose.scale(scale);
                for (Component line : lines) {
                    if (readableRows() && fontRenderer.width(line) > cellWidth - 28) {
                        line = Component
                                .literal(fontRenderer.plainSubstrByWidth(line.getString(), cellWidth - 40) + "...")
                                .withStyle(line.getStyle());
                    }
                    final int w = fontRenderer.width(line);
                    guiGraphics.text(fontRenderer, line,
                            readableRows() ? cellX + 24 : (int) ((itemX - 2 - w * scale) * inverseScale),
                            (int) (textY * inverseScale), textColor, false);
                    textY += fontRenderer.lineHeight * scale + LINE_SPACING;
                }
                pose.popMatrix();

                var entryStack = getEntryStack(entry);

                int itemY = cellY + (cellHeight - 16) / 2;
                AEKeyRendering.drawInGui(Minecraft.getInstance(), guiGraphics, itemX, itemY, entryStack);

                int overlay = getEntryOverlayColor(entry);
                if (overlay != 0) {
                    guiGraphics.fill(cellX, cellY, cellX + cellWidth, cellY + cellHeight, overlay);
                }

                if (mouseXRelative >= cellX && mouseXRelative <= cellX + cellWidth
                        && mouseYRelative >= cellY && mouseYRelative <= cellY + cellHeight) {
                    tooltipLines = getEntryTooltip(entry);
                    hovered = new StackWithBounds(
                            new GenericStack(entryStack, 0),
                            new Rect2i(screen.getGuiLeft() + cellX, screen.getGuiTop() + cellY, cellWidth,
                                    cellHeight));
                }
            }
        }
        hoveredStack = hovered;

        if (tooltipLines != null) {
            screen.drawTooltipWithHeader(guiGraphics, mouseX, mouseY, tooltipLines);
        }
    }

    public StackWithBounds getHoveredStack() {
        return hoveredStack;
    }

    /**
     * @return The number of rows that are off-screen given a number of entries.
     */
    public int getScrollableRows(int size) {
        return readableRows() ? Math.max(0, size - this.rows) : getScrollableRows(size, this.rows);
    }

    protected static int getScrollableRows(int size, int rows) {
        return (size + COLS - 1) / COLS - rows;
    }

    /**
     * Implement in subclass to determine the text to show next to an entry.
     */
    protected abstract List<Component> getEntryDescription(T entry);

    /**
     * Get the item to show for an entry.
     */
    protected abstract AEKey getEntryStack(T entry);

    /**
     * Get the tooltip lines to show for an entry.
     */
    protected abstract List<Component> getEntryTooltip(T entry);

    /**
     * Override and return a color to draw a colored rectangle behind an entry. Return 0 to not draw a rectangle.
     */
    protected int getEntryBackgroundColor(T entry) {
        return 0;
    }

    /**
     * Override and return a color to draw a colored rectangle above an entry. Return 0 to not draw a rectangle.
     */
    protected int getEntryOverlayColor(T entry) {
        return 0;
    }

}
