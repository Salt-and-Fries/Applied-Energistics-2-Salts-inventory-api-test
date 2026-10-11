package appeng.menu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import net.minecraft.resources.Identifier;

class MenuOpeningDataTest {
    @Test
    void fallbackDataIsCopiedAndBoundToTheMenuType() {
        var owner = new Object();
        var type = Identifier.parse("ae2:inscriber");
        byte[] data = { 1, 2, 3 };
        MenuOpeningData.receive(owner, 7, type, data);
        data[0] = 9;
        assertThat(MenuOpeningData.take(7, type)).containsExactly(1, 2, 3);
        MenuOpeningData.receive(owner, 8, type, new byte[] { 4 });
        assertThatThrownBy(() -> MenuOpeningData.take(8, Identifier.parse("ae2:condenser")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void aNewConnectionCannotConsumeAnOldConnectionsOpening() {
        var type = Identifier.parse("ae2:inscriber");
        MenuOpeningData.receive(new Object(), 3, type, new byte[] { 1 });
        MenuOpeningData.receive(new Object(), 4, type, new byte[] { 2 });
        assertThatThrownBy(() -> MenuOpeningData.take(3, type)).isInstanceOf(IllegalStateException.class);
        assertThat(MenuOpeningData.take(4, type)).containsExactly(2);
    }
}
