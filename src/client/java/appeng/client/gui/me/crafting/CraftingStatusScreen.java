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

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import appeng.client.gui.implementations.AESubScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.CPUSelectionList;
import appeng.client.gui.widgets.Scrollbar;
import appeng.menu.me.crafting.CraftingStatusMenu;

/**
 * This specialized version of the {@link CraftingCPUScreen} allows a player to cycle through the CPUs that are
 * currently working on crafting jobs, and see their crafting status.
 */
public class CraftingStatusScreen extends CraftingCPUScreen<CraftingStatusMenu> {
    private final CPUSelectionList cpuList;
    private final Scrollbar cpuScrollbar;

    private boolean narrowDesktop() {
        return isDesktopHosted() && net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScaledWidth() < 400;
    }

    @Override
    public int desktopToolbarLeft() {
        return narrowDesktop() ? 0 : -88;
    }

    @Override
    protected int desktopTableY() {
        return narrowDesktop() ? 84 : 19;
    }

    public net.minecraft.client.renderer.Rect2i cpuSelectionBounds() {
        return cpuList.getBounds();
    }

    public int cpuVisibleRows() {
        return cpuList.visibleRowCount();
    }

    public int cpuScrollPosition() {
        return cpuScrollbar.getCurrentScroll();
    }

    public CraftingStatusScreen(CraftingStatusMenu menu,
            Inventory playerInventory,
            Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);

        AESubScreen.addBackButton(menu, "back", widgets);

        cpuScrollbar = widgets.addScrollBar("selectCpuScrollbar", Scrollbar.BIG);
        cpuList = new CPUSelectionList(menu, cpuScrollbar, style);
        cpuList.setSelectionChanged(this::clearCraftingStatus);
        widgets.add("selectCpuList", cpuList);
    }

    @Override
    protected void init() {
        if (isDesktopHosted() && usesAeDesktopLayout()) {
            boolean narrow = narrowDesktop();
            var style = getStyle();
            var list = style.getWidget("selectCpuList");
            list.setLeft(narrow ? 8 : -88);
            list.setTop(narrow ? 31 : 0);
            cpuList.setDesktopNarrow(narrow);
            cpuList.setSize(narrow ? imageWidth - 16 : 94, narrow ? 46 : 164);
            var scroll = style.getWidget("selectCpuScrollbar");
            scroll.setLeft(narrow ? imageWidth - 20 : -11);
            scroll.setTop(narrow ? 32 : 19);
            scroll.setHeight(narrow ? 43 : 137);
            cpuScrollbar.setHeight(narrow ? 43 : 137);
            var heading = style.getText().get("cpuListTitle").getPosition();
            heading.setLeft(narrow ? 15 : -81);
            heading.setTop(narrow ? 19 : 7);
        }
        if (isDesktopHosted() && !usesAeDesktopLayout()) {
            boolean narrow = imageWidth < 332;
            var style = getStyle();
            var list = style.getWidget("selectCpuList");
            list.setTop(16);
            cpuList.setSize(narrow ? imageWidth - 8 : 90, narrow ? 48 : imageHeight - 43);
            var scroll = style.getWidget("selectCpuScrollbar");
            scroll.setLeft(narrow ? imageWidth - 20 : 81);
            scroll.setTop(20);
            cpuScrollbar.setHeight(narrow ? 40 : imageHeight - 51);
            var title = style.getText().get(TEXT_ID_DIALOG_TITLE).getPosition();
            title.setLeft(narrow ? 4 : 102);
            title.setTop(narrow ? 70 : 4);
            style.getWidget("suspend").setLeft(imageWidth - 127);
            style.getWidget("cancel").setLeft(imageWidth - 65);
        }
        super.init();
    }

    @Override
    public void drawBG(net.minecraft.client.gui.GuiGraphicsExtractor graphics, int x, int y,
            int mouseX, int mouseY, float partialTicks) {
        if (narrowDesktop() && usesAeDesktopLayout()) {
            appeng.client.gui.style.BackgroundGenerator.draw(imageWidth, imageHeight, graphics, x, y);
            var background = getStyle().getBackground();
            background.copy().src(background.getSrcX() + 9, background.getSrcY() + 19, 204, 69)
                    .dest(x + 9, y + desktopTableY()).blit(graphics);
            drawDesktopTableOutline(graphics, x, y, 68);
        } else
            super.drawBG(graphics, x, y, mouseX, mouseY, partialTicks);
    }

    @Override
    protected Component getGuiDisplayName(Component in) {
        return in; // the cpu name is on the button
    }

}
