package appeng.menu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import net.neoforged.fml.ModList;

import appeng.util.BootstrapMinecraft;

@BootstrapMinecraft
class OptionalDesktopLoadingTest {
    @Test
    void runtimeReallyMatchesTheRequestedConfiguration() throws Exception {
        boolean expected = Boolean.getBoolean("ae2.test.saltPresent");
        assertThat(ModList.get().isLoaded("salts_inventory_update")).isEqualTo(expected);
        if (!expected) {
            assertThatThrownBy(() -> Class.forName("com.salts_inventory_update.api.integration.DesktopIntegration"))
                    .isInstanceOf(ClassNotFoundException.class);
        }
        assertThat(Class.forName("appeng.menu.AEBaseMenu")).isNotNull();
        assertThat(Class.forName("appeng.menu.implementations.MenuTypeBuilder")).isNotNull();
        assertThat(Class.forName("appeng.core.network.InitNetwork")).isNotNull();
    }
}
