package appeng.client.gui;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import appeng.core.network.ClientboundPacket;
import appeng.core.network.bidirectional.ConfigValuePacket;
import appeng.util.BootstrapMinecraft;

@BootstrapMinecraft
class DesktopContentRegistryTest {
    @Test
    void openingOrderAndSameTypeWindowsHaveIndependentReceivers() {
        var first = UUID.randomUUID();
        var second = UUID.randomUUID();
        var firstUpdates = new ArrayList<ClientboundPacket>();
        var secondUpdates = new ArrayList<ClientboundPacket>();
        var beforeOpen = new ConfigValuePacket("sort_by", "NAME");
        var afterOpen = new ConfigValuePacket("sort_by", "AMOUNT");
        DesktopContentRegistry.awaitOpening(first, beforeOpen, 24);
        try {
            DesktopContentRegistry.register(first, firstUpdates::add);
            DesktopContentRegistry.register(second, secondUpdates::add);
            DesktopContentRegistry.receive(second, afterOpen);
            assertThat(firstUpdates).containsExactly(beforeOpen);
            assertThat(secondUpdates).containsExactly(afterOpen);
        } finally {
            DesktopContentRegistry.remove(first);
            DesktopContentRegistry.remove(second);
        }
    }

    @Test
    void lateUpdatesDoNotResurrectAClosedLifetime() {
        var owner = UUID.randomUUID();
        var updates = new ArrayList<ClientboundPacket>();
        DesktopContentRegistry.register(owner, updates::add);
        DesktopContentRegistry.remove(owner);
        DesktopContentRegistry.awaitOpening(owner, new ConfigValuePacket("sort_by", "NAME"), 24);
        try {
            DesktopContentRegistry.register(owner, updates::add);
            assertThat(updates).isEmpty();
        } finally {
            DesktopContentRegistry.remove(owner);
        }
    }
}
