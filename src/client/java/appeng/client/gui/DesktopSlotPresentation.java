package appeng.client.gui;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.item.ItemStack;

/** Mod-owned slot visuals; no dependency on an optional desktop implementation. */
public record DesktopSlotPresentation(ItemStack stack, @Nullable String countText, boolean preview) {
    public DesktopSlotPresentation {
        stack = stack.copy();
    }

    @Override
    public ItemStack stack() {
        return stack.copy();
    }
}
