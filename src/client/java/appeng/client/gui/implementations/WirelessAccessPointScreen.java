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

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.CommonButtons;
import appeng.core.localization.GuiText;
import appeng.menu.implementations.WirelessAccessPointMenu;
import appeng.util.Platform;

public class WirelessAccessPointScreen extends AEBaseScreen<WirelessAccessPointMenu> {

    public WirelessAccessPointScreen(WirelessAccessPointMenu menu, Inventory playerInventory, Component title,
            ScreenStyle style) {
        super(menu, playerInventory, title, style);
        this.addToLeftToolbar(CommonButtons.togglePowerUnit());

        if (!style.isDesktopRestyled()) {
            widgets.addBackgroundPanel("linkPanel");
        }
    }

    @Override
    public void drawBG(net.minecraft.client.gui.GuiGraphicsExtractor graphics, int x, int y, int mouseX,
            int mouseY, float partialTicks) {
        super.drawBG(graphics, x, y, mouseX, mouseY, partialTicks);
        if (isDesktopHosted() && !usesAeDesktopLayout()) {
            var inputs = menu.getSlots(appeng.menu.SlotSemantics.MACHINE_INPUT);
            var outputs = menu.getSlots(appeng.menu.SlotSemantics.MACHINE_OUTPUT);
            if (!inputs.isEmpty() && !outputs.isEmpty()) {
                int arrowX = x + inputs.getFirst().x + 8;
                int start = y + inputs.getFirst().y + 22;
                int end = y + outputs.getFirst().y - 6;
                graphics.fill(arrowX - 2, start, arrowX + 2, end - 5, 0xFF8B8B8B);
                for (int i = 0; i < 6; i++) {
                    graphics.fill(arrowX - 5 + i, end - 5 + i, arrowX + 6 - i, end - 4 + i, 0xFF8B8B8B);
                }
            }
        }
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();

        Component rangeText = Component.empty();
        Component energyUseText = Component.empty();
        if (menu.getRange() > 0) {
            double rangeBlocks = menu.getRange() / 10.0;
            rangeText = GuiText.WirelessRange.text(rangeBlocks);
            energyUseText = GuiText.PowerUsageRate.text(Platform.formatPowerLong(menu.getDrain(), true));
        }

        setTextContent("range", rangeText);
        setTextContent("energy_use", energyUseText);
    }

    @Override
    protected void drawDesktopBottomEdge(net.minecraft.client.gui.GuiGraphicsExtractor graphics) {
        if (usesAeDesktopLayout()) {
            var body = desktopContentBounds();
            int join = getStyle().getWidget("linkPanel")
                    .resolve(new net.minecraft.client.renderer.Rect2i(0, 0, imageWidth, imageHeight)).getX();
            // Extend the original clear machine surface, rather than exposing the removed inventory row.
            var source = getStyle().getBackground();
            if (body.getHeight() > 74) {
                source.copy().src(source.getSrcX(), source.getSrcY() + 70, join, 1)
                        .dest(body.getX(), body.getY() + 70, join, body.getHeight() - 74).blit(graphics);
            }
            appeng.client.gui.style.DesktopFrameEdges.bottomRun(graphics, getStyle().getBackground(),
                    body.getX(), body.getY() + body.getHeight() - 4, join);
        } else
            super.drawDesktopBottomEdge(graphics);
    }

    @Override
    protected void drawDesktopOverlayHints(net.minecraft.client.gui.GuiGraphicsExtractor graphics) {
        if (!usesAeDesktopLayout())
            return;
        var body = desktopContentBounds();
        var position = getStyle().getWidget("linkPanel")
                .resolve(new net.minecraft.client.renderer.Rect2i(0, 0, imageWidth, imageHeight));
        if (position.getY() + getStyle().getImage("linkPanel").getSrcHeight() != body.getHeight())
            return;
        // Turn the panel's white joining rail into the shared footer instead of leaving a one-pixel spur.
        var source = getStyle().getBackground();
        source.copy().src(source.getSrcX() + 4, source.getSrcY() + source.getSrcHeight() - 4, 1, 4)
                .dest(body.getX() + position.getX(), body.getY() + body.getHeight() - 4).blit(graphics);
    }

}
