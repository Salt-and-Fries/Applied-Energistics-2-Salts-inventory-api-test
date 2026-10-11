package appeng.menu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;

import appeng.core.network.bidirectional.ConfigValuePacket;
import appeng.core.network.serverbound.GuiActionPacket;
import appeng.core.network.serverbound.SwapSlotsPacket;
import appeng.util.BootstrapMinecraft;

@BootstrapMinecraft
class DesktopMenuPacketsTest {
    @Test
    void actionsRoundTripAndRejectTrailingBytes() {
        var action = new GuiActionPacket(43, "example", new byte[] { 1, 2, 3 });
        var data = DesktopMenuPackets.encodeAction(action, RegistryAccess.EMPTY);
        var decoded = (GuiActionPacket) DesktopMenuPackets.decodeAction(action.type().id(), data, RegistryAccess.EMPTY);
        assertThat(decoded.containerId()).isEqualTo(43);
        assertThat(decoded.actionName()).isEqualTo("example");
        assertThat(decoded.argumentPayload()).containsExactly(1, 2, 3);
        assertThatThrownBy(() -> DesktopMenuPackets.decodeAction(action.type().id(),
                Arrays.copyOf(data, data.length + 1), RegistryAccess.EMPTY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void channelsAndPayloadSizesAreBounded() {
        assertThatThrownBy(() -> DesktopMenuPackets.decodeAction(Identifier.parse("ae2:world_action"),
                new byte[0], RegistryAccess.EMPTY)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DesktopMenuPackets.decodeAction(SwapSlotsPacket.TYPE.id(),
                new byte[32769], RegistryAccess.EMPTY)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void configurationUpdatesRoundTrip() {
        var packet = new ConfigValuePacket("sort_by", "NAME");
        var encoded = DesktopMenuPackets.encodeUpdate(packet, RegistryAccess.EMPTY);
        assertThat(DesktopMenuPackets.decodeUpdate(packet.type().id(), encoded, RegistryAccess.EMPTY))
                .isEqualTo(packet);
    }

    @Test
    void nestedWidgetCallbacksKeepTheirOwnMenu() {
        var first = mock(AEBaseMenu.class);
        var second = mock(AEBaseMenu.class);
        var a = new SwapSlotsPacket(1, 2);
        var b = new SwapSlotsPacket(3, 4);
        var c = new SwapSlotsPacket(5, 6);
        MenuPacketScope.run(first, () -> {
            MenuPacketScope.send(a);
            MenuPacketScope.run(second, () -> MenuPacketScope.send(b));
            MenuPacketScope.send(c);
        });
        verify(first).sendPacketToServer(a);
        verify(second).sendPacketToServer(b);
        verify(first).sendPacketToServer(c);
    }

    @Test
    void authenticatedSessionOverridesEmbeddedMenuId() {
        var action = new GuiActionPacket(-1234, "example", new byte[] { 9 });
        var routed = (GuiActionPacket) DesktopMenuPackets.forMenu(action, 17);
        assertThat(routed.containerId()).isEqualTo(17);
        assertThat(routed.actionName()).isEqualTo(action.actionName());
        assertThat(routed.argumentPayload()).containsExactly(9);
    }
}
