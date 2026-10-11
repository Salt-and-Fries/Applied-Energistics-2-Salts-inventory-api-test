package appeng.client.gui;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.imageio.ImageIO;

import com.mojang.blaze3d.platform.InputConstants;

import org.slf4j.LoggerFactory;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/** Read-only design-reference captures. Only the disposable Minecraft fixture is changed. */
final class DesktopVanillaAtlasQa {
    private static final boolean EXTRAS = Boolean.getBoolean("ae2.desktop.qa.extras");
    private static final Path OUTPUT = Path.of("../build/vanilla-design-analysis" + (EXTRAS ? "/extras" : ""));
    private static final List<net.minecraft.world.entity.EntityType<?>> MOUNTS = List
            .of("horse", "donkey", "mule", "llama", "camel")
            .stream().<net.minecraft.world.entity.EntityType<?>>map(name -> BuiltInRegistries.ENTITY_TYPE
                    .getValue(net.minecraft.resources.Identifier.fromNamespaceAndPath("minecraft", name)))
            .toList();
    private static net.minecraft.world.entity.animal.equine.AbstractHorse mount;
    private static final AtomicBoolean READY = new AtomicBoolean();
    private static List<MenuType<?>> types;
    private static int stage;
    private static int ticks;
    private static int index = -1;
    private static long screenshotStart;
    private static String label;

    static void tick(Minecraft mc) {
        try {
            if (stage == 0) {
                stage = 1;
                Files.createDirectories(OUTPUT);
                Files.writeString(OUTPUT.resolve("manifest.csv"), "index,label,screen,file\n");
                types = BuiltInRegistries.MENU.stream()
                        .filter(t -> BuiltInRegistries.MENU.getKey(t).getNamespace().equals("minecraft")).toList();
                mc.options.guiScale().set(2);
                mc.options.pauseOnLostFocus = false;
                var id = mc.player.getUUID();
                mc.getSingleplayerServer().execute(() -> {
                    var p = mc.getSingleplayerServer().getPlayerList().getPlayer(id);
                    p.setGameMode(GameType.SURVIVAL);
                    p.getInventory().clearContent();
                    for (int x = -8; x < 9; x++)
                        for (int z = -8; z < 9; z++) {
                            p.level().setBlockAndUpdate(new BlockPos(x, 79, z), Blocks.STONE.defaultBlockState());
                            for (int y = 80; y < 85; y++)
                                p.level().setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
                        }
                    p.teleportTo(p.level(), 0, 80, 0, Set.of(), 0, 25, true);
                    READY.set(true);
                });
                return;
            }
            if (!READY.get() || stage >= 100 || ++ticks < 40)
                return;
            ticks = 0;
            if (stage == 1) {
                mc.gui.setScreen(null);
                mc.keyboardHandler.keyPress(mc.getWindow().handle(), InputConstants.PRESS,
                        new KeyEvent(InputConstants.KEY_E, 0, 0));
                mc.keyboardHandler.keyPress(mc.getWindow().handle(), InputConstants.RELEASE,
                        new KeyEvent(InputConstants.KEY_E, 0, 0));
                stage = 2;
            } else if (stage == 2) {
                if (mc.gui.screen() == null) {
                    mc.gui.setScreen(new InventoryScreen(mc.player));
                    stage = 6;
                    return;
                }
                label = "player-inventory";
                screenshotStart = System.currentTimeMillis();
                Screenshot.grab(mc, false);
                stage = 5;
            } else if (stage == 6) {
                label = "player-inventory";
                screenshotStart = System.currentTimeMillis();
                Screenshot.grab(mc, false);
                stage = 5;
            } else if (stage == 3) {
                if (EXTRAS && Boolean.getBoolean("ae2.desktop.qa.tabsOnly") && index == 0) {
                    var id = mc.player.getUUID();
                    mc.getSingleplayerServer().execute(() -> mc.getSingleplayerServer().getPlayerList().getPlayer(id)
                            .setGameMode(GameType.CREATIVE));
                    stage = 9;
                    return;
                }
                if (index >= (EXTRAS ? MOUNTS.size() : types.size())
                        || EXTRAS && Boolean.getBoolean("ae2.desktop.qa.tabsOnly")) {
                    LoggerFactory.getLogger(DesktopVanillaAtlasQa.class)
                            .info("VANILLA ATLAS COMPLETE {} registered menus", types.size());
                    stage = 100;
                    return;
                }
                if (EXTRAS) {
                    var entityType = MOUNTS.get(index);
                    label = BuiltInRegistries.ENTITY_TYPE.getKey(entityType).getPath();
                    var id = mc.player.getUUID();
                    mc.getSingleplayerServer().execute(() -> {
                        var p = mc.getSingleplayerServer().getPlayerList().getPlayer(id);
                        mount = (net.minecraft.world.entity.animal.equine.AbstractHorse) entityType.create(p.level(),
                                net.minecraft.world.entity.EntitySpawnReason.COMMAND);
                        mount.setPos(2, 80, 1);
                        mount.setTamed(true);
                        mount.setNoAi(true);
                        if (mount instanceof net.minecraft.world.entity.animal.equine.AbstractChestedHorse chested)
                            chested.setChest(true);
                        p.level().addFreshEntity(mount);
                    });
                    stage = 7;
                    return;
                }
                var type = types.get(index);
                label = BuiltInRegistries.MENU.getKey(type).getPath();
                var id = mc.player.getUUID();
                mc.getSingleplayerServer().execute(() -> {
                    var p = mc.getSingleplayerServer().getPlayerList().getPlayer(id);
                    p.openMenu(new SimpleMenuProvider(
                            (containerId, inventory, player) -> type.create(containerId, inventory),
                            Component.literal("Vanilla " + BuiltInRegistries.MENU.getKey(type).getPath())));
                });
                stage = 4;
            } else if (stage == 9) {
                mc.gui.setScreen(new net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen(mc.player,
                        mc.getConnection().enabledFeatures(), mc.options.operatorItemsTab().get()));
                label = "creative-inventory";
                stage = 4;
            } else if (stage == 7) {
                var id = mc.player.getUUID();
                mc.getSingleplayerServer().execute(() -> mount
                        .openCustomInventoryScreen(mc.getSingleplayerServer().getPlayerList().getPlayer(id)));
                stage = 4;
            } else if (stage == 8) {
                label = "player-inventory-expanded";
                screenshotStart = System.currentTimeMillis();
                Screenshot.grab(mc, false);
                stage = 5;
            } else if (stage == 4) {
                screenshotStart = System.currentTimeMillis();
                Screenshot.grab(mc, false);
                stage = 5;
            } else if (stage == 5) {
                Path latest;
                try (var stream = Files.list(Path.of("screenshots"))) {
                    latest = stream.filter(p -> p.toString().endsWith(".png"))
                            .filter(p -> modified(p) >= screenshotStart)
                            .max(Comparator.comparingLong(DesktopVanillaAtlasQa::modified)).orElseThrow();
                }
                var filename = String.format("%02d-%s.png", index + 1, label);
                Files.copy(latest, OUTPUT.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
                Files.writeString(OUTPUT.resolve("manifest.csv"), String.format("%d,%s,%s,%s\n", index + 1, label,
                        mc.gui.screen() == null ? "none" : mc.gui.screen().getClass().getName(), filename),
                        StandardOpenOption.APPEND);
                LoggerFactory.getLogger(DesktopVanillaAtlasQa.class).info("VANILLA CAPTURE {} {}", index + 1, label);
                if (EXTRAS && index == -1 && label.equals("player-inventory")) {
                    clickTemplate(latest, mc, Path.of("../build/vanilla-design-analysis/native-expand-template.png"));
                    stage = 8;
                    return;
                }
                closeVisibleWindows(latest, mc);
                index++;
                stage = 3;
            }
        } catch (Exception error) {
            LoggerFactory.getLogger(DesktopVanillaAtlasQa.class).error("VANILLA ATLAS FAILED {} {}", index, label,
                    error);
            stage = 100;
        }
    }

    private static long modified(Path p) {
        try {
            return Files.getLastModifiedTime(p).toMillis();
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static void closeVisibleWindows(Path screenshot, Minecraft mc) throws Exception {
        clickTemplate(screenshot, mc, Path.of("../build/vanilla-design-analysis/native-close-template.png"));
    }

    private static void clickTemplate(Path screenshot, Minecraft mc, Path templatePath) throws Exception {
        // Locate the close icon from its captured pixels; no renderer implementation is inspected.
        var template = ImageIO.read(templatePath.toFile());
        var image = ImageIO.read(screenshot.toFile());
        for (int y = 0; y < image.getHeight() - template.getHeight(); y++) {
            for (int x = 0; x < image.getWidth() - template.getWidth(); x++) {
                if (image.getRGB(x, y) != template.getRGB(0, 0))
                    continue;
                boolean match = true;
                for (int ty = 0; ty < template.getHeight() && match; ty++)
                    for (int tx = 0; tx < template.getWidth(); tx++) {
                        if (image.getRGB(x + tx, y + ty) != template.getRGB(tx, ty)) {
                            match = false;
                            break;
                        }
                    }
                if (match && mc.gui.screen() != null) {
                    double gx = (x + 9.0) * mc.getWindow().getGuiScaledWidth() / image.getWidth();
                    double gy = (y + 9.0) * mc.getWindow().getGuiScaledHeight() / image.getHeight();
                    var event = new MouseButtonEvent(gx, gy, new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0));
                    mc.mouseHandler.onMove(mc.getWindow().handle(), x + 9.0, y + 9.0, 0, 0);
                    mc.gui.screen().mouseMoved(gx, gy);
                    boolean handled = mc.gui.screen().mouseClicked(event, false);
                    mc.gui.screen().mouseReleased(event);
                    LoggerFactory.getLogger(DesktopVanillaAtlasQa.class).info("VANILLA UI control at {}, {} handled {}",
                            gx, gy, handled);
                }
            }
        }
    }
}
