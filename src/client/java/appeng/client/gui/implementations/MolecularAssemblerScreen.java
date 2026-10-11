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

package appeng.client.gui.implementations;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.ProgressBar;
import appeng.client.gui.widgets.ProgressBar.Direction;
import appeng.menu.implementations.MolecularAssemblerMenu;

public class MolecularAssemblerScreen extends UpgradeableScreen<MolecularAssemblerMenu> {

    private final ProgressBar pb;

    public MolecularAssemblerScreen(MolecularAssemblerMenu menu, Inventory playerInventory,
            Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);

        this.pb = new ProgressBar(this.menu, style.getImage("progressBar"), Direction.VERTICAL);
        widgets.add("progressBar", this.pb);
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();

        this.pb.setFullMsg(Component.literal(this.menu.getCurrentProgress() + "%"));
    }

    @Override
    public void drawBG(net.minecraft.client.gui.GuiGraphicsExtractor graphics, int x, int y, int mouseX, int mouseY,
            float partialTicks) {
        super.drawBG(graphics, x, y, mouseX, mouseY, partialTicks);
        if (isDesktopHosted() && !usesAeDesktopLayout()) {
            appeng.client.gui.DesktopChrome.arrow(graphics, x + 90, y + 33);
        }
    }

    @Override
    public void drawFG(net.minecraft.client.gui.GuiGraphicsExtractor graphics, int x, int y, int mouseX, int mouseY) {
        super.drawFG(graphics, x, y, mouseX, mouseY);
        if (isDesktopHosted() && !usesAeDesktopLayout()
                && menu.getSlots(appeng.menu.SlotSemantics.MACHINE_CRAFTING_GRID).stream()
                        .noneMatch(this::desktopSlotVisible)) {
            int lineY = 60;
            for (var line : font.split(Component.translatable("gui.ae2.DesktopHint.insertPattern"), imageWidth - 16)) {
                graphics.text(font, line, 8, lineY, appeng.client.gui.DesktopChrome.TEXT, false);
                lineY += font.lineHeight + 2;
            }
        }
    }
}
