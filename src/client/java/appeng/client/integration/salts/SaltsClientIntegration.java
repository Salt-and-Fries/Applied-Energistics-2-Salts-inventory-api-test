package appeng.client.integration.salts;

import com.salts_inventory_update.api.client.desktop.DesktopClientApi;
import com.salts_inventory_update.api.client.desktop.DesktopMenuFactories;
import com.salts_inventory_update.api.client.desktop.DesktopRenderContext;
import com.salts_inventory_update.api.client.desktop.DesktopWindowDefinition;
import com.salts_inventory_update.api.client.desktop.DesktopWindowSetupContext;
import com.salts_inventory_update.api.client.desktop.DesktopWindowSize;
import com.salts_inventory_update.api.integration.DesktopIntegration;
import com.salts_inventory_update.api.integration.DesktopIntegrationContext;

import io.netty.buffer.Unpooled;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.network.connection.ConnectionType;

import appeng.client.gui.DesktopQaHooks;
import appeng.menu.AEBaseMenu;
import appeng.menu.implementations.MenuTypeBuilder;

/** Salt resolves this class only on clients, after AE registries exist. */
public final class SaltsClientIntegration implements DesktopIntegration {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(SaltsClientIntegration.class);

    @Override
    public void register(DesktopIntegrationContext context) {
        LOG.info("Registering {} AE desktop client definitions", MenuTypeBuilder.registeredBuilders().size());
        MenuTypeBuilder.registeredBuilders().forEach(this::registerMenu);
        AEWindow.installHeaderKeyboard();
        if (Boolean.getBoolean("ae2.desktop.qa")) {
            registerPopupProbe();
            var view = Identifier.parse("ae2:desktop_api_probe");
            DesktopClientApi.registerView(view, String.class, new DesktopWindowDefinition<String, Void>() {
                @Override
                public DesktopWindowSize defaultSize(DesktopWindowSetupContext<String> setup) {
                    return DesktopWindowSize.of(160, 90);
                }

                @Override
                public void render(DesktopRenderContext<String, Void> render) {
                    render.text(render.model(), render.contentX() + 4, render.contentY() + 4, 0xFFFFFFFF, false);
                }
            });
            DesktopQaHooks.openProbe = () -> DesktopClientApi.openView(view, "qa", "Documented API view",
                    Component.literal("AE desktop API probe")).isPresent();
        }
    }

    private void registerPopupProbe() {
        var view = Identifier.parse("ae2:desktop_popup_probe");
        DesktopClientApi.registerView(view, DesktopQaHooks.PopupState.class,
                new DesktopWindowDefinition<DesktopQaHooks.PopupState, Void>() {
                    @Override
                    public DesktopWindowSize defaultSize(DesktopWindowSetupContext<DesktopQaHooks.PopupState> setup) {
                        return DesktopWindowSize.of(200, 100);
                    }

                    @Override
                    public com.salts_inventory_update.api.client.desktop.DesktopWindowSurface surface(
                            com.salts_inventory_update.api.client.desktop.DesktopWindowContext<DesktopQaHooks.PopupState, Void> context) {
                        return com.salts_inventory_update.api.client.desktop.DesktopWindowSurface
                                .modOwned(java.util.List.of(
                                        com.salts_inventory_update.api.client.desktop.DesktopWindowRegion
                                                .visualAndInteractive(0, 0,
                                                        context.windowWidth(), context.windowHeight())));
                    }

                    @Override
                    public java.util.List<com.salts_inventory_update.api.client.desktop.DesktopPopup> popups(
                            com.salts_inventory_update.api.client.desktop.DesktopWindowContext<DesktopQaHooks.PopupState, Void> context) {
                        return context.model().open
                                ? java.util.List.of(new com.salts_inventory_update.api.client.desktop.DesktopPopup(
                                        "confirmation",
                                        java.util.List
                                                .of(com.salts_inventory_update.api.client.desktop.DesktopWindowRegion
                                                        .visualAndInteractive(8, 24, 184, 68)),
                                        true, false, true, false))
                                : java.util.List.of();
                    }

                    @Override
                    public boolean dismissPopup(
                            com.salts_inventory_update.api.client.desktop.DesktopWindowContext<DesktopQaHooks.PopupState, Void> context,
                            String id, com.salts_inventory_update.api.client.desktop.DesktopPopup.Dismissal reason) {
                        if (!context.model().open)
                            return false;
                        context.model().open = false;
                        context.model().dismissals++;
                        context.model().dismissal = reason.name();
                        return true;
                    }

                    @Override
                    public boolean charTyped(
                            com.salts_inventory_update.api.client.desktop.DesktopInputContext<DesktopQaHooks.PopupState, Void> context,
                            net.minecraft.client.input.CharacterEvent event) {
                        context.model().characters++;
                        return true;
                    }

                    @Override
                    public void render(DesktopRenderContext<DesktopQaHooks.PopupState, Void> context) {
                        int x = context.windowX(), y = context.windowY();
                        context.fill(x, y, x + context.windowWidth(), y + context.windowHeight(), 0xFF26394C);
                        context.text("Modal popup probe", x + 8, y + 8, 0xFFFFFFFF, false);
                        context.text(context.model().open ? "Escape dismisses this popup" : "Popup dismissed", x + 8,
                                y + 34, 0xFFFFFFFF, false);
                    }

                    @Override
                    public void closed(
                            com.salts_inventory_update.api.client.desktop.DesktopWindowContext<DesktopQaHooks.PopupState, Void> context) {
                        context.model().closed = true;
                    }
                });
        DesktopQaHooks.openModalProbe = () -> {
            var state = new DesktopQaHooks.PopupState();
            DesktopQaHooks.popupState = state;
            var handle = DesktopClientApi.openView(view, "modal-qa", state, Component.literal("Modal probe"))
                    .orElseThrow();
            DesktopQaHooks.closeModalProbe = handle::close;
        };
    }

    @SuppressWarnings("unchecked")
    private void registerMenu(MenuType<?> untyped, MenuTypeBuilder<?, ?> builder) {
        var type = (MenuType<AEBaseMenu>) untyped;
        DesktopClientApi.registerWindow(type, new AEWindow(type));
        DesktopMenuFactories.register(type, (minecraft, player, id, data) -> {
            var buffer = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(data), player.registryAccess(),
                    ConnectionType.NEOFORGE);
            try {
                var owner = buffer.readUUID();
                boolean companion = buffer.readBoolean();
                var menu = builder.createDesktopMenu(id, player.getInventory(), buffer);
                if (buffer.isReadable()) {
                    throw new IllegalArgumentException("Trailing AE menu opening data");
                }
                menu.setDesktopSyncId(owner);
                menu.setDesktopToolboxCompanion(companion);
                if (Boolean.getBoolean("ae2.desktop.qa")) {
                    LOG.info("Desktop QA reconstructed menu {}, id={}, owner={}", type, id, owner);
                }
                return menu;
            } finally {
                buffer.release();
            }
        });
    }
}
