package appeng.client.gui;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.MenuType;

import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import appeng.items.tools.NetworkToolItem;
import appeng.menu.MenuOpener;
import appeng.menu.SlotSemantics;
import appeng.menu.implementations.CellWorkbenchMenu;
import appeng.menu.implementations.IOPortMenu;
import appeng.menu.locator.MenuLocators;

/** Actual server inventories, public-provider snapshots and native mouse/key events. */
final class DesktopCompanionQa {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(DesktopCompanionQa.class);
    private static final AtomicBoolean READY = new AtomicBoolean();
    private static final Path OUTPUT = Path.of("../build/api5-companion-native");
    private static int stage;
    private static int wait;
    private static UUID first;
    private static UUID second;
    private static UUID companion;
    private static UUID replacement;
    private static BlockPos origin;
    private static int oldX, oldY;
    private static boolean firstTitlePrepared, secondTitlePrepared;
    private static int keyboardProbeStage;
    private static boolean previousFocus;
    private static net.minecraft.world.item.ItemStack oldTool;
    private static int sourceDragStep;
    private static java.util.List<UUID> draggedSources;
    private static int sourceDragX, sourceDragY;

    static void begin(Minecraft client) {
        DesktopQaHooks.closeAll.run();
        stage = 1;
        wait = 0;
        firstTitlePrepared = secondTitlePrepared = false;
        keyboardProbeStage = 0;
        sourceDragStep = 0;
        draggedSources = java.util.List.of();
        READY.set(false);
        server(client, player -> {
            if (!player.inventoryMenu.getCarried().isEmpty()) {
                throw new IllegalStateException("Companion fixture requires an empty canonical cursor");
            }
            player.getInventory().clearContent();
            origin = player.blockPosition().offset(1, 0, 2);
            player.level().setBlockAndUpdate(origin, AEBlocks.IO_PORT.block().defaultBlockState());
            player.level().setBlockAndUpdate(origin.offset(1, 0, 0),
                    AEBlocks.CELL_WORKBENCH.block().defaultBlockState());
            var tool = AEItems.NETWORK_TOOL.stack();
            NetworkToolItem.getInventory(tool).setItemDirect(0, AEItems.CAPACITY_CARD.stack(4));
            player.getInventory().setItem(2, tool);
            oldTool = tool;
        });
    }

    static void tick(Minecraft client) {
        if (stage == 0 || stage >= 100 || ++wait < 30)
            return;
        wait = 0;
        if (!READY.get())
            return;
        try {
            switch (stage) {
                case 1 -> {
                    require(DesktopQaHooks.WINDOWS.isEmpty() && DesktopQaHooks.COMPANIONS.isEmpty(),
                            "previous parent and companion lifetimes disposed");
                    open(client, IOPortMenu.TYPE, origin);
                }
                case 2 -> {
                    first = parent(IOPortMenu.TYPE);
                    DesktopQaHooks.WINDOWS.get(first).content().setTextContent(
                            AEBaseScreen.TEXT_ID_DIALOG_TITLE, net.minecraft.network.chat.Component.literal("IO"));
                    if (!firstTitlePrepared) {
                        DesktopQaHooks.windowAction.apply(first, "UNPIN");
                        firstTitlePrepared = true;
                        return;
                    }
                    if (!hasControl(first, "PIN"))
                        return;
                    require(DesktopQaHooks.COMPANIONS.size() == 1, "one actual Toolbox menu acquired");
                    companion = DesktopQaHooks.COMPANIONS.keySet().iterator().next();
                    require(DesktopQaHooks.WINDOWS.get(first).content().hasDesktopSharedToolbox(),
                            "parent hides its duplicate panel only after acquisition");
                    require(!companion.equals(DesktopQaHooks.keyboardOwner), "auto-open does not steal keyboard focus");
                    require(DesktopQaHooks.COMPANION_OWNERS.get(companion).size() == 1,
                            "companion has one authenticated owner");
                    require(toolSlot(companion).getItem().getCount() == 4, "real source supplies its four cards");
                    DesktopQaHooks.unlockWindow.accept(first);
                    clickControl(client, first, "PIN");
                }
                case 3 -> {
                    LOG.info("Companion QA after native Pin: {} controls={}", DesktopQaHooks.windowSummary.apply(first),
                            DesktopQaHooks.MANAGEMENT_CONTROLS.get(first));
                    require(control(first, "PIN").selected(), "custom Pin button activates manager pin state");
                    clickControl(client, first, "PIN");
                }
                case 4 -> {
                    require(!control(first, "PIN").selected(), "custom Pin button unpins");
                    DesktopQaHooks.unlockWindow.accept(first);
                    var window = DesktopQaHooks.COMPANIONS.get(companion);
                    oldX = window.x();
                    oldY = window.y();
                    open(client, CellWorkbenchMenu.TYPE, origin.offset(1, 0, 0));
                }
                case 5 -> {
                    second = parent(CellWorkbenchMenu.TYPE);
                    DesktopQaHooks.WINDOWS.get(second).content().setTextContent(
                            AEBaseScreen.TEXT_ID_DIALOG_TITLE, net.minecraft.network.chat.Component.literal("Cell"));
                    if (!secondTitlePrepared) {
                        secondTitlePrepared = true;
                        return;
                    }
                    if (!hasControl(second, "MINIMIZE"))
                        return;
                    DesktopQaHooks.unlockWindow.accept(second);
                    require(DesktopQaHooks.COMPANIONS.size() == 1
                            && DesktopQaHooks.COMPANION_OWNERS.get(companion).size() == 2,
                            "two parents share one menu and source inventory");
                    var window = DesktopQaHooks.COMPANIONS.get(companion);
                    require(window.x() == oldX && window.y() == oldY,
                            "second acquisition preserves companion position");
                    require(!DesktopQaHooks.windowAction.apply(companion, "USER_CLOSE")
                            && !DesktopQaHooks.windowAction.apply(companion, "PIN")
                            && !DesktopQaHooks.windowAction.apply(companion, "MINIMIZE")
                            && !DesktopQaHooks.windowAction.apply(companion, "MANAGEMENT_MENU"),
                            "companion refuses independent window management");
                    require(DesktopQaHooks.MANAGEMENT_CONTROLS.getOrDefault(companion, java.util.List.of()).isEmpty(),
                            "companion title has no window controls");
                    beginDrag(client, window.x() + 15, window.y() + 8);
                }
                case 6 -> moveDrag(client, oldX + 35, oldY + 23);
                case 7 -> endDrag(client);
                case 8 -> {
                    var window = DesktopQaHooks.COMPANIONS.get(companion);
                    require(window.x() != oldX || window.y() != oldY, "native title dragging moves companion");
                    Screenshot.grab(client, false);
                    clickControl(client, first, "MINIMIZE");
                }
                case 9 -> {
                    LOG.info("Companion QA minimized owner: {} controls={}", DesktopQaHooks.windowSummary.apply(first),
                            DesktopQaHooks.MANAGEMENT_CONTROLS.get(first));
                    Screenshot.grab(client, false);
                    require(control(first, "MINIMIZE").selected(), "custom Minimize button collapses main window");
                    require(DesktopQaHooks.windowVisible.test(companion),
                            "another visible parent keeps companion visible");
                    clickControl(client, second, "MINIMIZE");
                }
                case 10 -> {
                    require(!DesktopQaHooks.windowVisible.test(companion), "all minimized parents hide companion");
                    clickControl(client, first, "MINIMIZE");
                }
                case 11 -> {
                    require(!control(first, "MINIMIZE").selected() && DesktopQaHooks.windowVisible.test(companion),
                            "custom Restore button restores owner and retained companion");
                    clickControl(client, second, "MINIMIZE");
                }
                case 12 -> {
                    DesktopQaHooks.WINDOWS.get(first).content().setTextContent(AEBaseScreen.TEXT_ID_DIALOG_TITLE,
                            net.minecraft.network.chat.Component
                                    .literal("A deliberately long Network Tool owner title"));
                    if (!hasControl(first, "ELLIPSIS"))
                        return;
                    require(DesktopQaHooks.MANAGEMENT_CONTROLS.get(first).stream().map(DesktopQaHooks.Control::kind)
                            .toList().equals(java.util.List.of("ELLIPSIS", "CLOSE")),
                            "full native row compresses together to More and Close");
                    clickControl(client, first, "ELLIPSIS");
                }
                case 13 -> {
                    Screenshot.grab(client, false);
                    pressKey(client, new KeyEvent(InputConstants.KEY_ESCAPE, 0, 0));
                }
                case 14 -> {
                    require(DesktopQaHooks.WINDOWS.containsKey(first),
                            "Escape dismisses native manager menu without closing owner");
                    Screenshot.grab(client, false);
                    DesktopQaHooks.WINDOWS.get(first).content().setTextContent(
                            AEBaseScreen.TEXT_ID_DIALOG_TITLE, net.minecraft.network.chat.Component.literal("IO"));
                    if (!hasControl(first, "PIN"))
                        return;
                    require(!hasControl(first, "ELLIPSIS") && hasControl(first, "FOCUS")
                            && hasControl(first, "LOCK") && hasControl(first, "LINK"),
                            "short title restores complete native controls without More");
                    clickControl(client, first, "PIN");
                }
                case 15 -> {
                    clickControl(client, first, "PIN");
                    pressKey(client, new KeyEvent(InputConstants.KEY_F10, 0, InputConstants.MOD_ALT));
                }
                case 16 -> {
                    require(DesktopQaHooks.MANAGEMENT_CONTROLS.get(first).stream()
                            .anyMatch(DesktopQaHooks.Control::keyboardFocused),
                            "Alt-F10 reaches custom manager controls");
                    if (keyboardProbeStage == 0) {
                        pressKey(client, new KeyEvent(InputConstants.KEY_TAB, 0, 0));
                        keyboardProbeStage++;
                        return;
                    }
                    if (keyboardProbeStage == 1) {
                        require(control(first, "PIN").keyboardFocused(), "Tab reaches native Pin control");
                        pressKey(client, new KeyEvent(InputConstants.KEY_RETURN, 0, 0));
                        keyboardProbeStage++;
                        return;
                    }
                    if (keyboardProbeStage == 2) {
                        require(control(first, "PIN").selected(),
                                "keyboard activation reaches actual manager Pin action");
                        pressKey(client, new KeyEvent(InputConstants.KEY_SPACE, 0, 0));
                        keyboardProbeStage++;
                        return;
                    }
                    require(!control(first, "PIN").selected(), "Space reaches actual manager Unpin action");
                    pressKey(client, new KeyEvent(InputConstants.KEY_ESCAPE, 0, 0));
                    clickTool(client, companion);
                }
                case 17 -> {
                    require(client.player.inventoryMenu.getCarried().is(AEItems.CAPACITY_CARD.asItem())
                            && client.player.inventoryMenu.getCarried().getCount() == 4
                            && !toolSlot(companion).hasItem(),
                            "companion pickup uses canonical cursor and actual source slot");
                    clickControl(client, first, "CLOSE");
                }
                case 18 -> {
                    require(!DesktopQaHooks.WINDOWS.containsKey(first)
                            && DesktopQaHooks.COMPANIONS.containsKey(companion)
                            && DesktopQaHooks.COMPANION_OWNERS.get(companion).size() == 1,
                            "closing first owner preserves the shared menu");
                    require(client.player.inventoryMenu.getCarried().getCount() == 4,
                            "owner closure preserves the canonical cursor");
                    clickTool(client, companion);
                }
                case 19 -> {
                    require(client.player.inventoryMenu.getCarried().isEmpty()
                            && toolSlot(companion).getItem().getCount() == 4,
                            "remaining owner can return cards without duplication");
                    server(client, player -> {
                        require(NetworkToolItem.getInventory(player.getInventory().getItem(2)).getStackInSlot(0)
                                .getCount() == 4,
                                "server backing component contains exactly four returned cards");
                        var tool = AEItems.NETWORK_TOOL.stack();
                        NetworkToolItem.getInventory(tool).setItemDirect(0, AEItems.SPEED_CARD.stack(2));
                        player.getInventory().setItem(2, tool);
                    });
                }
                case 20 -> {
                    require(!DesktopQaHooks.COMPANIONS.containsKey(companion) && DesktopQaHooks.COMPANIONS.size() == 1,
                            "source replacement disposes old companion lifetime");
                    replacement = DesktopQaHooks.COMPANIONS.keySet().iterator().next();
                    require(toolSlot(replacement).getItem().is(AEItems.SPEED_CARD.asItem())
                            && toolSlot(replacement).getItem().getCount() == 2,
                            "replacement source opens its own actual inventory");
                    server(client, player -> {
                        require(NetworkToolItem.getInventory(oldTool).getStackInSlot(0).getCount() == 4,
                                "invalidated old source retains its contents");
                        var other = AEItems.NETWORK_TOOL.stack();
                        NetworkToolItem.getInventory(other).setItemDirect(0, AEItems.SPEED_CARD.stack(2));
                        player.getInventory().setItem(1, other);
                        MenuOpener.open(IOPortMenu.TYPE, player,
                                MenuLocators.forBlockEntity(player.level().getBlockEntity(origin)));
                    });
                }
                case 21 -> {
                    first = parent(IOPortMenu.TYPE);
                    require(DesktopQaHooks.COMPANIONS.size() == 2,
                            "two distinct tools acquire two distinct companions");
                    if (!DesktopQaHooks.COMPANION_READY.containsAll(DesktopQaHooks.COMPANIONS.keySet()))
                        return;
                    if (draggedSources.isEmpty())
                        draggedSources = java.util.List.copyOf(DesktopQaHooks.COMPANIONS.keySet());
                    if (sourceDragStep < 8) {
                        var window = DesktopQaHooks.COMPANIONS.get(draggedSources.get(sourceDragStep / 4));
                        switch (sourceDragStep % 4) {
                            case 0 -> {
                                sourceDragX = window.x();
                                sourceDragY = window.y();
                                beginDrag(client, window.x() + 15, window.y() + 8);
                            }
                            case 1 -> moveDrag(client, sourceDragX + 35, sourceDragY + 23);
                            case 2 -> endDrag(client);
                            case 3 -> require(window.x() != sourceDragX || window.y() != sourceDragY,
                                    "each distinct source can be rearranged through its drag-only title");
                        }
                        sourceDragStep++;
                        return;
                    }
                    require(DesktopQaHooks.COMPANIONS.values().stream()
                            .map(window -> window.content().desktopManagementTitle().getString()).distinct()
                            .count() == 2,
                            "distinct tool sources retain distinct visible title badges after rearrangement/replacement");
                    require(DesktopQaHooks.COMPANION_OWNERS.values().stream().allMatch(owners -> owners.size() == 1),
                            "distinct inventories do not inherit each other's ownership");
                    Screenshot.grab(client, false);
                    clickControl(client, first, "CLOSE");
                }
                case 22 -> {
                    require(DesktopQaHooks.COMPANIONS.size() == 1 && DesktopQaHooks.COMPANIONS.containsKey(replacement),
                            "last owner disposes only its own inventory companion");
                    clickControl(client, second, "CLOSE");
                }
                case 23 -> {
                    require(DesktopQaHooks.WINDOWS.isEmpty() && DesktopQaHooks.COMPANIONS.isEmpty(),
                            "final parent closure disposes final companion");
                    require(client.player.inventoryMenu.getCarried().isEmpty(), "cursor remains empty after teardown");
                    Files.createDirectories(OUTPUT);
                    Files.writeString(OUTPUT.resolve("completed.txt"),
                            "Real companion and custom control assertions passed.\n");
                    LOG.info(
                            "Companion QA COMPLETE: custom controls, keyboard access, sharing, drag, visibility, cursor conservation, source replacement and last-owner disposal");
                    stage = 100;
                    if (Boolean.getBoolean("ae2.desktop.qa.quit"))
                        client.stop();
                    return;
                }
                default -> throw new IllegalStateException("Unknown companion QA stage " + stage);
            }
            stage++;
        } catch (Exception failure) {
            LOG.error("Companion QA FAILED at stage {}", stage, failure);
            stage = 100;
            if (Boolean.getBoolean("ae2.desktop.qa.quit"))
                client.stop();
        }
    }

    private static UUID parent(MenuType<?> type) {
        return DesktopQaHooks.WINDOWS.entrySet().stream()
                .filter(entry -> entry.getValue().content().getMenu().getType() == type)
                .findFirst().orElseThrow().getKey();
    }

    private static boolean hasControl(UUID owner, String kind) {
        return DesktopQaHooks.MANAGEMENT_CONTROLS.getOrDefault(owner, java.util.List.of()).stream()
                .anyMatch(control -> control.kind().equals(kind));
    }

    private static DesktopQaHooks.Control control(UUID owner, String kind) {
        return DesktopQaHooks.MANAGEMENT_CONTROLS.get(owner).stream().filter(c -> c.kind().equals(kind)).findFirst()
                .orElseThrow();
    }

    private static void pressKey(Minecraft client, KeyEvent key) {
        var screen = client.gui.screen();
        var event = new net.neoforged.neoforge.client.event.ScreenEvent.KeyPressed.Pre(screen, key);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event);
        if (!event.isCanceled())
            screen.keyPressed(key);
    }

    private static void clickControl(Minecraft client, UUID owner, String kind) {
        var c = control(owner, kind);
        LOG.info("Companion QA native control click {}: {} state={}", owner, c,
                DesktopQaHooks.windowSummary.apply(owner));
        click(client, c.x() + c.width() / 2, c.y() + c.height() / 2);
    }

    private static net.minecraft.world.inventory.Slot toolSlot(UUID owner) {
        return DesktopQaHooks.COMPANIONS.get(owner).content().getMenu().getSlots(SlotSemantics.STORAGE).getFirst();
    }

    private static void clickTool(Minecraft client, UUID owner) {
        var content = DesktopQaHooks.COMPANIONS.get(owner).content();
        var slot = toolSlot(owner);
        click(client, content.getGuiLeft() + slot.x + 8, content.getGuiTop() + slot.y + 8);
    }

    private static void click(Minecraft client, int x, int y) {
        boolean focused = client.getWindow().isFocused();
        focus(client, true);
        moveDrag(client, x, y);
        var button = new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0);
        client.mouseHandler.onButton(client.getWindow().handle(), button, InputConstants.PRESS);
        client.mouseHandler.onButton(client.getWindow().handle(), button, InputConstants.RELEASE);
        focus(client, focused);
    }

    private static void open(Minecraft client, MenuType<?> type, BlockPos position) {
        server(client, player -> MenuOpener.open(type, player,
                MenuLocators.forBlockEntity(player.level().getBlockEntity(position))));
    }

    private static void server(Minecraft client,
            java.util.function.Consumer<net.minecraft.server.level.ServerPlayer> action) {
        READY.set(false);
        var id = client.player.getUUID();
        client.getSingleplayerServer().execute(() -> {
            try {
                action.accept(client.getSingleplayerServer().getPlayerList().getPlayer(id));
            } catch (Exception failure) {
                LOG.error("Companion QA FAILED in server fixture", failure);
                client.execute(() -> {
                    stage = 100;
                    if (Boolean.getBoolean("ae2.desktop.qa.quit"))
                        client.stop();
                });
            } finally {
                READY.set(true);
            }
        });
    }

    private static void beginDrag(Minecraft client, int x, int y) {
        previousFocus = client.getWindow().isFocused();
        focus(client, true);
        moveDrag(client, x, y);
        client.mouseHandler.onButton(client.getWindow().handle(),
                new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0), InputConstants.PRESS);
    }

    private static void moveDrag(Minecraft client, int x, int y) {
        var window = client.getWindow();
        client.mouseHandler.onMove(window.handle(), (double) x * window.getScreenWidth() / window.getGuiScaledWidth(),
                (double) y * window.getScreenHeight() / window.getGuiScaledHeight(), 0, 0);
        client.mouseHandler.handleAccumulatedMovement();
    }

    private static void endDrag(Minecraft client) {
        client.mouseHandler.onButton(client.getWindow().handle(),
                new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0), InputConstants.RELEASE);
        focus(client, previousFocus);
    }

    private static void focus(Minecraft client, boolean value) {
        try {
            var method = client.getWindow().getClass().getDeclaredMethod("onFocus", boolean.class);
            method.setAccessible(true);
            method.invoke(client.getWindow(), value);
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException(failure);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition)
            throw new IllegalStateException(message);
        LOG.info("Companion QA PASS: {}", message);
    }
}
