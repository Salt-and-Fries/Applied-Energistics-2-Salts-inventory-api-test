package appeng.client.gui;

import net.minecraft.world.inventory.ContainerInput;

/** AE-owned callbacks; implementations belong to optional integrations and live for one input callback. */
public interface DesktopContentActions {
    boolean clickSlot(int slot, int button, ContainerInput action);

    void close();

    void switchContent(AEBaseScreen<?> content);
}
