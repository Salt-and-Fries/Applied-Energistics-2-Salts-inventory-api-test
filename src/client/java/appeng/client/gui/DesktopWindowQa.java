package appeng.client.gui;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;

import appeng.core.definitions.AEBlocks;
import appeng.menu.MenuOpener;
import appeng.menu.implementations.InscriberMenu;
import appeng.menu.locator.MenuLocators;

/** Opt-in development fixture. Normal launches never activate it; contains no Salt references. */
public final class DesktopWindowQa {
    private static final Logger LOG = LoggerFactory.getLogger(DesktopWindowQa.class);
    private static final AtomicInteger STAGE = new AtomicInteger();
    private static int elapsed;
    private static int startupTicks;
    private static String lastStartupScreen;
    private static BlockPos fixture;

    private DesktopWindowQa() {
    }

    public static void tick(Minecraft minecraft) {
        if (!Boolean.getBoolean("ae2.desktop.qa")) {
            return;
        }
        if (minecraft.player == null || minecraft.level == null) {
            startupTicks++;
            var currentScreen = minecraft.gui.screen();
            String currentScreenName = currentScreen == null ? "none" : currentScreen.getClass().getName();
            if (startupTicks >= 120 && !currentScreenName.equals(lastStartupScreen)) {
                lastStartupScreen = currentScreenName;
                Screenshot.grab(minecraft, false);
                var screen = minecraft.gui.screen();
                LOG.info("Desktop QA startup screen: {}", screen == null ? "none" : screen.getClass().getName());
                if (screen != null) {
                    for (var child : screen.children()) {
                        if (child instanceof net.minecraft.client.gui.components.AbstractWidget widget) {
                            LOG.info("Desktop QA startup control: {}", widget.getMessage().getString());
                        }
                    }
                }
            }
            var startupScreen = minecraft.gui.screen();
            if (startupTicks > 120
                    && startupScreen instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen) {
                for (var child : startupScreen.children()) {
                    if (child instanceof net.minecraft.client.gui.components.Button button
                            && button.getMessage().getString().equals("Continue")) {
                        button.onPress(new net.minecraft.client.input.MouseButtonInfo(
                                com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT, 0));
                        LOG.info("Desktop QA completed scratch-client accessibility onboarding");
                        break;
                    }
                }
            }
            if (startupScreen instanceof net.minecraft.client.gui.screens.BackupConfirmScreen) {
                for (var child : startupScreen.children()) {
                    if (child instanceof net.minecraft.client.gui.components.Button button
                            && button.getMessage().getString().equals("Create Backup and Load")) {
                        button.onPress(new net.minecraft.client.input.MouseButtonInfo(
                                com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT, 0));
                        LOG.info("Desktop QA backed up the disposable copied game-test world");
                        break;
                    }
                }
            }
            return;
        }
        var server = minecraft.getSingleplayerServer();
        if (server == null) {
            return;
        }
        if ("atlas".equals(System.getProperty("ae2.desktop.qa.suite"))) {
            DesktopAtlasQa.tick(minecraft);
            DesktopCompanionQa.tick(minecraft);
            return;
        }
        if ("vanilla-atlas".equals(System.getProperty("ae2.desktop.qa.suite"))) {
            DesktopVanillaAtlasQa.tick(minecraft);
            return;
        }
        if ("terminal".equals(System.getProperty("ae2.desktop.qa.suite"))) {
            DesktopTerminalQa.tick(minecraft);
            DesktopLifecycleQa.tick(minecraft);
            DesktopCompanionQa.tick(minecraft);
            return;
        }
        if (STAGE.compareAndSet(0, 1)) {
            var playerId = minecraft.player.getUUID();
            server.execute(() -> {
                var player = server.getPlayerList().getPlayer(playerId);
                if (player == null) {
                    return;
                }
                BlockPos pos = player.blockPosition().offset(2, 0, 0);
                player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
                fixture = pos;
                for (int dx = -3; dx <= 3; dx++) {
                    for (int dz = -3; dz <= 3; dz++) {
                        player.level().setBlockAndUpdate(player.blockPosition().offset(dx, -1, dz),
                                net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
                    }
                }
                player.level().setBlockAndUpdate(pos, AEBlocks.INSCRIBER.block().defaultBlockState());
                if (player.level()
                        .getBlockEntity(pos) instanceof appeng.blockentity.misc.InscriberBlockEntity inscriber) {
                    inscriber.getInternalInventory().setItemDirect(0,
                            appeng.core.definitions.AEItems.SILICON_PRESS.stack());
                    inscriber.getInternalInventory().setItemDirect(2,
                            appeng.core.definitions.AEItems.SILICON.stack(16));
                }
                LOG.info("Desktop QA fixture placed at {}", pos);
                STAGE.set(2);
            });
        }
        if (STAGE.get() == 2 && elapsed == 0) {
            for (var key : minecraft.options.keyMappings) {
                if (key.getName().toLowerCase(java.util.Locale.ROOT).contains("salt")) {
                    LOG.info("Desktop QA registered key: {} = {}", key.getName(),
                            key.getTranslatedKeyMessage().getString());
                }
            }
            minecraft.gui.setScreen(null);
            net.minecraft.client.KeyMapping.click(minecraft.options.keyInventory.getKey());
            LOG.info("Desktop QA pressed the configured inventory key through Minecraft's normal key routing");
        }
        if (STAGE.get() == 2 && ++elapsed >= 60) {
            LOG.info("Desktop QA documented openView available: {}", DesktopQaHooks.openProbe.getAsBoolean());
            STAGE.set(5);
            elapsed = 0;
        }
        // Give the ordinary mode/capability handshake time to reach the server before opening a menu.
        if (STAGE.get() == 5 && ++elapsed >= 40) {
            LOG.info("Desktop QA inventory screen: {}",
                    minecraft.gui.screen() == null ? "none" : minecraft.gui.screen().getClass().getName());
            STAGE.set(3);
            var playerId = minecraft.player.getUUID();
            server.execute(() -> {
                var player = server.getPlayerList().getPlayer(playerId);
                if (player != null) {
                    var host = player.level().getBlockEntity(fixture);
                    if (host != null) {
                        MenuOpener.open(InscriberMenu.TYPE, player, MenuLocators.forBlockEntity(host));
                        LOG.info("Desktop QA opened inscriber");
                    }
                }
            });
            elapsed = 0;
        }
        if (STAGE.get() == 3 && ++elapsed >= 100) {
            STAGE.set(6);
            Screenshot.grab(minecraft, false);
            LOG.info("Desktop QA active screen: {}",
                    minecraft.gui.screen() == null ? "none" : minecraft.gui.screen().getClass().getName());
            LOG.info("Desktop QA rendered AE windows: {}", DesktopQaHooks.WINDOWS.size());
            elapsed = 0;
        }
        if (STAGE.get() == 6 && ++elapsed >= 30) {
            if (!DesktopQaHooks.WINDOWS.isEmpty() && minecraft.gui.screen() != null) {
                var window = DesktopQaHooks.WINDOWS.values().iterator().next();
                var event = new net.minecraft.client.input.MouseButtonEvent(window.inputX(), window.inputY(),
                        new net.minecraft.client.input.MouseButtonInfo(
                                com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT, 0));
                minecraft.gui.screen().mouseClicked(event, false);
                minecraft.gui.screen().mouseReleased(event);
                LOG.info("Desktop QA clicked the real machine input through the desktop screen");
            } else if (minecraft.gui.screen() instanceof AEBaseScreen<?> content) {
                var inputs = content.getMenu().getSlots(appeng.menu.SlotSemantics.MACHINE_INPUT);
                if (!inputs.isEmpty()) {
                    var slot = inputs.getFirst();
                    var event = new net.minecraft.client.input.MouseButtonEvent(content.getGuiLeft() + slot.x + 8,
                            content.getGuiTop() + slot.y + 8,
                            new net.minecraft.client.input.MouseButtonInfo(
                                    com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT, 0));
                    content.mouseClicked(event, false);
                    content.mouseReleased(event);
                    LOG.info("Desktop QA clicked the real machine input through the ordinary AE screen");
                }
            }
            STAGE.set(7);
            elapsed = 0;
        }
        if (STAGE.get() == 7 && ++elapsed >= 30) {
            server.execute(() -> {
                var host = server.overworld().getBlockEntity(fixture);
                if (host instanceof appeng.blockentity.misc.InscriberBlockEntity inscriber) {
                    LOG.info("Desktop QA authoritative input count after pickup: {}",
                            inscriber.getInternalInventory().getStackInSlot(2).getCount());
                }
                var secondPos = fixture.offset(0, 0, 2);
                server.overworld().setBlockAndUpdate(secondPos, AEBlocks.INSCRIBER.block().defaultBlockState());
                if (server.overworld()
                        .getBlockEntity(secondPos) instanceof appeng.blockentity.misc.InscriberBlockEntity second) {
                    second.getInternalInventory().setItemDirect(2,
                            net.minecraft.world.item.Items.REDSTONE.getDefaultInstance().copyWithCount(7));
                }
            });
            STAGE.set(8);
            elapsed = 0;
            Screenshot.grab(minecraft, false);
        }
        if (STAGE.get() == 8 && ++elapsed >= 60) {
            var playerId = minecraft.player.getUUID();
            server.execute(() -> {
                var player = server.getPlayerList().getPlayer(playerId);
                var host = server.overworld().getBlockEntity(fixture.offset(0, 0, 2));
                if (player != null && host != null) {
                    MenuOpener.open(InscriberMenu.TYPE, player, MenuLocators.forBlockEntity(host));
                }
            });
            STAGE.set(9);
            elapsed = 0;
        }
        if (STAGE.get() == 9 && ++elapsed >= 100) {
            LOG.info("Desktop QA simultaneous same-type windows: {}", DesktopQaHooks.WINDOWS.size());
            Screenshot.grab(minecraft, false);
            STAGE.set(4);
        }
    }
}
