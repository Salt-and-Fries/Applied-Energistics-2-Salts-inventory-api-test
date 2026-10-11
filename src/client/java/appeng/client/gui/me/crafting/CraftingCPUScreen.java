/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import appeng.api.config.CpuSelectionMode;
import appeng.api.config.Settings;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.StackWithBounds;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.Scrollbar;
import appeng.client.gui.widgets.ServerSettingToggleButton;
import appeng.client.gui.widgets.SettingToggleButton;
import appeng.core.localization.GuiText;
import appeng.menu.me.crafting.CraftingCPUMenu;
import appeng.menu.me.crafting.CraftingStatus;
import appeng.menu.me.crafting.CraftingStatusEntry;

/**
 * This screen shows the current crafting job that a crafting CPU is working on (if any).
 */
public class CraftingCPUScreen<T extends CraftingCPUMenu> extends AEBaseScreen<T> {

    private static final Logger LOG = LoggerFactory.getLogger(CraftingCPUScreen.class);

    protected final CraftingStatusTableRenderer table;

    private final Button cancel;
    private final Button suspend;

    private final Scrollbar scrollbar;

    private final SettingToggleButton<CpuSelectionMode> schedulingModeButton;

    private CraftingStatus status;

    protected int desktopTableY() {
        return 19;
    }

    protected void clearCraftingStatus() {
        status = null;
    }

    public CraftingCPUScreen(T menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);

        this.table = new CraftingStatusTableRenderer(this,
                style.isDesktopRestyled() && menu instanceof appeng.menu.me.crafting.CraftingStatusMenu ? 102 : 9,
                19);

        this.scrollbar = widgets.addScrollBar("scrollbar", Scrollbar.BIG);

        this.cancel = this.widgets.addButton("cancel", GuiText.Cancel.text(), menu::cancelCrafting);
        this.suspend = this.widgets.addButton("suspend", GuiText.Suspend.text(), menu::toggleScheduling);

        this.schedulingModeButton = new ServerSettingToggleButton<>(Settings.CPU_SELECTION_MODE,
                CpuSelectionMode.ANY);

        // This screen is reused for the crafting status in Terminals, where it should not show the config buttons
        if (menu.allowConfiguration()) {
            this.addToLeftToolbar(this.schedulingModeButton);
        }
    }

    @Override
    protected void init() {
        if (isDesktopHosted() && usesAeDesktopLayout()) {
            int tableY = desktopTableY();
            int rowCount = Math.max(1, (imageHeight - 28 - tableY) / 23);
            table.setLayout(9, tableY, rowCount);
            getStyle().getWidget("scrollbar").setTop(tableY);
            getStyle().getWidget("scrollbar").setHeight(rowCount * 23 - 1);
            scrollbar.setHeight(rowCount * 23 - 1);
        }
        if (isDesktopHosted() && !usesAeDesktopLayout()) {
            boolean statusMenu = menu instanceof appeng.menu.me.crafting.CraftingStatusMenu;
            boolean narrow = statusMenu && imageWidth < 332;
            int tableY = narrow ? 85 : 19;
            table.setLayout(statusMenu && !narrow ? 102 : 9, tableY,
                    Math.max(1, (imageHeight - 28 - tableY) / 23));
            var scroll = getStyle().getWidget("scrollbar");
            scroll.setLeft(imageWidth - 20);
            scroll.setTop(tableY);
            scrollbar.setHeight(Math.max(16, imageHeight - 28 - tableY));
        }
        super.init();
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        this.cancel.active = !getVisualEntries().isEmpty();
        this.suspend.active = this.cancel.active;

        // Update the dialog title with an ETA if possible
        Component title = this.getGuiDisplayName(GuiText.CraftingStatus.text());
        if (status != null) {
            final long elapsedTime = status.getElapsedTime();
            final double remainingItems = status.getRemainingItemCount();
            final double startItems = status.getStartItemCount();
            final long eta = (long) (elapsedTime / Math.max(1d, startItems - remainingItems)
                    * remainingItems);

            if (eta > 0 && !getVisualEntries().isEmpty()) {
                final long etaInMilliseconds = TimeUnit.MILLISECONDS.convert(eta, TimeUnit.NANOSECONDS);
                final String etaTimeText = DurationFormatUtils.formatDuration(etaInMilliseconds,
                        GuiText.ETAFormat.getLocal());
                title = title.copy().append(" - " + etaTimeText);
            }
        }
        // Mention that items can't be stored if applicable
        if (menu.isCantStoreItems()) {
            title = title.copy().append(" - ").append(GuiText.CantStoreItems.text().withStyle(ChatFormatting.RED));
        }
        setTextContent(TEXT_ID_DIALOG_TITLE, title);

        final int size = this.status != null ? this.status.getEntries().size() : 0;
        scrollbar.setRange(0, this.table.getScrollableRows(size), 1);

        this.schedulingModeButton.set(this.menu.getSchedulingMode());
    }

    private List<CraftingStatusEntry> getVisualEntries() {
        if (isDesktopHosted() && (!menu.cpuStatusReady || !menu.cpuAvailable || !menu.hasActiveJob
                || menu instanceof appeng.menu.me.crafting.CraftingStatusMenu statusMenu
                        && menu.cpuStatusSerial != statusMenu.getSelectedCpuSerial())) {
            return Collections.emptyList();
        }
        return this.status != null ? status.getEntries() : Collections.emptyList();
    }

    public String desktopCraftingMessageKey() {
        if (!getVisualEntries().isEmpty())
            return "";
        boolean waitingForSelection = menu instanceof appeng.menu.me.crafting.CraftingStatusMenu statusMenu
                && menu.cpuStatusSerial != statusMenu.getSelectedCpuSerial();
        return !menu.cpuStatusReady || waitingForSelection ? "waitingCraftingStatus"
                : !menu.cpuAvailable ? "noCraftingCpu"
                        : menu.hasActiveJob ? "waitingCraftingStatus" : "noCraftingJob";
    }

    protected final void drawDesktopTableOutline(GuiGraphicsExtractor graphics, int x, int y, int tableHeight) {
        var source = getStyle().getBackground();
        var edge = source.copy().src(source.getSrcX() + 8, source.getSrcY() + 20, 1, 1);
        edge.copy().dest(x + 8, y + desktopTableY() - 1, 206, 1).blit(graphics);
        edge.copy().dest(x + 8, y + desktopTableY() + tableHeight, 206, 1).blit(graphics);
        edge.copy().dest(x + 8, y + desktopTableY(), 1, tableHeight).blit(graphics);
        edge.copy().dest(x + 213, y + desktopTableY(), 1, tableHeight).blit(graphics);
    }

    @Override
    public void drawFG(GuiGraphicsExtractor guiGraphics, int offsetX, int offsetY, int mouseX, int mouseY) {
        super.drawFG(guiGraphics, offsetX, offsetY, mouseX, mouseY);

        this.table.render(guiGraphics, mouseX, mouseY, getVisualEntries(), scrollbar.getCurrentScroll());
        if (isDesktopHosted() && getVisualEntries().isEmpty()) {
            int textX = 15;
            String messageKey = desktopCraftingMessageKey();
            var message = Component.translatable("gui.ae2.DesktopHint." + messageKey);
            var lines = font.split(message, 192);
            int tableHeight = Math.max(1, (imageHeight - 28 - desktopTableY()) / 23) * 23 - 1;
            var source = getStyle().getBackground();
            source.copy().src(source.getSrcX() + 15, source.getSrcY() + 25, 1, 1)
                    .dest(9, desktopTableY(), 204, tableHeight).blit(guiGraphics);
            drawDesktopTableOutline(guiGraphics, 0, 0, tableHeight);
            int textY = desktopTableY() + (tableHeight - lines.size() * (font.lineHeight + 2)) / 2;
            for (var line : lines) {
                guiGraphics.text(font, line, textX, textY, appeng.client.gui.DesktopChrome.TEXT, false);
                textY += font.lineHeight + 2;
            }
        }
    }

    @org.jetbrains.annotations.Nullable
    @Override
    public StackWithBounds getStackUnderMouse(double mouseX, double mouseY) {
        var hovered = table.getHoveredStack();
        if (hovered != null) {
            return hovered;
        }
        return super.getStackUnderMouse(mouseX, mouseY);
    }

    public void postUpdate(CraftingStatus status) {
        Map<Long, CraftingStatusEntry> entries;
        if (this.status == null || status.isFullStatus()) {
            // Start from scratch.
            // We can't just reuse the status because we have to filter out deleted entries.
            entries = new LinkedHashMap<>();
        } else {
            // Merge the status entries.
            entries = new LinkedHashMap<>(this.status.getEntries().size());
            for (CraftingStatusEntry entry : this.status.getEntries()) {
                entries.put(entry.getSerial(), entry);
            }
        }

        for (CraftingStatusEntry entry : status.getEntries()) {
            if (entry.isDeleted()) {
                entries.remove(entry.getSerial());
                continue;
            }

            CraftingStatusEntry existingEntry = entries.get(entry.getSerial());
            if (existingEntry != null) {
                entries.put(entry.getSerial(), new CraftingStatusEntry(
                        existingEntry.getSerial(),
                        existingEntry.getWhat(),
                        entry.getStoredAmount(),
                        entry.getActiveAmount(),
                        entry.getPendingAmount()));
            } else if (entry.getWhat() == null) {
                LOG.warn("Received an updated crafting status entry {}, but no current entry exists. {}",
                        entry,
                        status.isFullStatus());
            } else {
                entries.put(entry.getSerial(), entry);
            }
        }

        List<CraftingStatusEntry> sortedEntries = new ArrayList<>(entries.values());
        Collections.sort(sortedEntries);
        this.status = new CraftingStatus(
                true,
                status.getElapsedTime(),
                status.getRemainingItemCount(),
                status.getStartItemCount(),
                sortedEntries,
                status.isSuspended());
        this.suspend.setMessage(status.isSuspended() ? GuiText.Resume.text() : GuiText.Suspend.text());
    }

}
