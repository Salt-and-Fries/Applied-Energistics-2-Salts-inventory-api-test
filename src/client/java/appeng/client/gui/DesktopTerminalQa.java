package appeng.client.gui;

import java.util.concurrent.atomic.AtomicBoolean;

import com.mojang.blaze3d.platform.InputConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

import appeng.api.config.Actionable;
import appeng.api.config.Settings;
import appeng.api.parts.PartHelper;
import appeng.api.stacks.AEItemKey;
import appeng.client.gui.me.common.Repo;
import appeng.client.gui.me.common.RepoSlot;
import appeng.client.gui.me.common.TerminalSettingsScreen;
import appeng.client.gui.widgets.ActionButton;
import appeng.client.gui.widgets.SettingToggleButton;
import appeng.client.gui.widgets.TabButton;
import appeng.core.definitions.AEParts;
import appeng.me.helpers.BaseActionSource;
import appeng.menu.MenuOpener;
import appeng.menu.MenuPacketScope;
import appeng.menu.SlotSemantics;
import appeng.menu.implementations.PatternAccessTermMenu;
import appeng.menu.locator.MenuLocators;
import appeng.menu.me.common.MEStorageMenu;
import appeng.menu.me.crafting.CraftAmountMenu;
import appeng.menu.me.crafting.CraftConfirmMenu;
import appeng.menu.me.items.CraftingTermMenu;
import appeng.menu.me.items.PatternEncodingTermMenu;
import appeng.parts.AEBasePart;
import appeng.parts.reporting.CraftingTerminalPart;
import appeng.server.testworld.Plot;

/** Real client inputs and authoritative server checks in an explicitly requested disposable QA world. */
final class DesktopTerminalQa {
    private static final Logger LOG = LoggerFactory.getLogger(DesktopTerminalQa.class);
    private static final AtomicBoolean READY = new AtomicBoolean();
    private static int stage;
    private static appeng.client.gui.widgets.AECheckbox settingsFocusProbe;
    private static boolean settingsFocusOriginalValue;
    private static int wait;
    private static BlockPos origin;
    private static Object firstEntry;
    private static Object sortBefore;
    private static DesktopQaHooks.Window beforeResize;
    private static DesktopQaHooks.Window beforeMove;
    private static DesktopQaHooks.Window beforeBorderResize;
    private static DesktopQaHooks.Window firstOverlap;
    private static DesktopQaHooks.Window secondOverlap;
    private static String firstSearch;
    private static double[] dragMotion;
    private static boolean dragFocus;
    private static int afterDragStage;
    private static int frameProbe;
    private static boolean moveUnlockRetried;
    private static int ownershipStep;
    private static int cancellationBefore;

    private DesktopTerminalQa() {
    }

    static void tick(Minecraft minecraft) {
        if (stage == 0) {
            stage = 1;
            var playerId = minecraft.player.getUUID();
            minecraft.getSingleplayerServer().execute(() -> {
                var player = minecraft.getSingleplayerServer().getPlayerList().getPlayer(playerId);
                player.setGameMode(GameType.SURVIVAL);
                player.getInventory().clearContent();
                player.containerMenu.setCarried(net.minecraft.world.item.ItemStack.EMPTY);
                origin = player.blockPosition().offset(1, 0, 2);
                var plot = new Plot(Identifier.fromNamespaceAndPath("ae2", "desktop_terminal_qa"));
                plot.block("[-2,4] [-1,2] [-2,2]", net.minecraft.world.level.block.Blocks.AIR);
                plot.block("[-2,4] -2 [-2,2]", net.minecraft.world.level.block.Blocks.STONE);
                plot.creativeEnergyCell("0 -1 0");
                plot.block("0 -1 1", appeng.core.definitions.AEBlocks.CONTROLLER);
                plot.cable("[0,3] 0 0", AEParts.GLASS_CABLE);
                plot.drive(new BlockPos(0, 0, 1)).addItemCell64k().and().addItemCell64k();
                plot.part("0 0 0", Direction.NORTH, AEParts.TERMINAL);
                plot.part("1 0 0", Direction.NORTH, AEParts.CRAFTING_TERMINAL);
                plot.part("2 0 0", Direction.NORTH, AEParts.PATTERN_ENCODING_TERMINAL);
                plot.part("3 0 0", Direction.NORTH, AEParts.PATTERN_ACCESS_TERMINAL);
                plot.block("1 0 1", appeng.core.definitions.AEBlocks.PATTERN_PROVIDER);
                plot.block("1 1 1", appeng.core.definitions.AEBlocks.MOLECULAR_ASSEMBLER);
                plot.block("2 0 1", appeng.core.definitions.AEBlocks.CRAFTING_STORAGE_64K);
                plot.afterGridInitAt(new BlockPos(0, 0, 1), (grid, node) -> {
                    var storage = grid.getStorageService().getInventory();
                    var source = new BaseActionSource();
                    int inserted = 0;
                    for (var item : BuiltInRegistries.ITEM) {
                        if (item != Items.AIR && !BuiltInRegistries.ITEM.getKey(item).getPath().contains("acacia")
                                && item != Items.DIAMOND_PICKAXE
                                && inserted < 109
                                && storage.insert(AEItemKey.of(item), 64, Actionable.MODULATE, source) == 64) {
                            inserted++;
                        }
                    }
                    storage.insert(AEItemKey.of(Items.ACACIA_LOG), 64, Actionable.MODULATE, source);
                    storage.insert(AEItemKey.of(Items.DIAMOND_PICKAXE), 10, Actionable.MODULATE, source);
                    var crafting = (CraftingTerminalPart) PartHelper.getPart(player.level(), origin.offset(1, 0, 0),
                            Direction.NORTH);
                    crafting.getSubInventory(CraftingTerminalPart.INV_CRAFTING).setItemDirect(0,
                            Items.ACACIA_LOG.getDefaultInstance());
                    var provider = (appeng.blockentity.crafting.PatternProviderBlockEntity) player.level()
                            .getBlockEntity(origin.offset(1, 0, 1));
                    provider.getLogic().getPatternInv().addItems(
                            appeng.server.testplots.CraftingPatternHelper.encodeShapelessCraftingRecipe(player.level(),
                                    Items.ACACIA_LOG.getDefaultInstance()));
                    LOG.info("Terminal QA powered fixture ready: {} distinct seeded keys", inserted + 2);
                    READY.set(true);
                });
                plot.build(player.level(), player, origin);
            });
            return;
        }
        if (!READY.get() || stage >= 40 || ++wait < 60) {
            return;
        }
        wait = 0;
        try {
            switch (stage++) {
                case 1 -> {
                    minecraft.gui.setScreen(null);
                    KeyMapping.click(minecraft.options.keyInventory.getKey());
                }
                case 2 -> LOG.info("Terminal QA documented desktop probe: {}", DesktopQaHooks.openProbe.getAsBoolean());
                case 3 -> open(minecraft, 0, MEStorageMenu.TYPE);
                case 4 -> {
                    var content = content(minecraft, MEStorageMenu.TYPE);
                    if (content.isDesktopHosted() && Boolean.getBoolean("ae2.desktop.qa.frameProbe")) {
                        beforeMove = window(content);
                        qaFocus(minecraft, true);
                        hoverFrameControl(minecraft);
                        stage = 39;
                        return;
                    }
                    var repo = repo(content);
                    require(repo.getAllEntries().size() >= 100, "initial network catalog synchronization");
                    if (!search(content).getValue().isEmpty()) {
                        var field = search(content);
                        click(minecraft, field.getX() + 5, field.getY() + 5, InputConstants.MOUSE_BUTTON_RIGHT);
                        stage = 4;
                        return;
                    }
                    if (content.isDesktopHosted() && content.desktopSlots().stream().filter(RepoSlot.class::isInstance)
                            .count() >= repo.size()) {
                        content.requestDesktopSize(188, 130);
                        stage = 4;
                        return;
                    }
                    LOG.info("Terminal QA real slots {}, presentation slots {}, catalog {}",
                            content.getMenu().slots.size(),
                            content.desktopSlots().size(), repo.size());
                    Screenshot.grab(minecraft, false);
                    firstEntry = repo.get(0);
                    var slot = content.desktopSlots().stream().filter(RepoSlot.class::isInstance).findFirst()
                            .orElseThrow();
                    minecraft.gui.screen().mouseScrolled(content.getGuiLeft() + slot.x + 8,
                            content.getGuiTop() + slot.y + 8, 0, -3);
                }
                case 5 -> {
                    var content = content(minecraft, MEStorageMenu.TYPE);
                    require(repo(content).get(0) != firstEntry, "terminal wheel scrolling");
                    var search = search(content);
                    click(minecraft, search.getX() + 5, search.getY() + 5, InputConstants.MOUSE_BUTTON_RIGHT);
                    click(minecraft, search.getX() + 5, search.getY() + 5, InputConstants.MOUSE_BUTTON_LEFT);
                    minecraft.gui.screen()
                            .keyPressed(new net.minecraft.client.input.KeyEvent(InputConstants.KEY_E, 0, 0));
                    minecraft.gui.screen().charTyped(new CharacterEvent('e'));
                    require(search.getValue().equals("e"), "inventory key can be typed into a focused search editor");
                    if (content.isDesktopHosted()) {
                        minecraft.gui.screen()
                                .keyPressed(new net.minecraft.client.input.KeyEvent(InputConstants.KEY_TAB, 0, 0));
                        require(content.getFocused() != null && content.getFocused() != search,
                                "Tab moves focus between native controls");
                    }
                    click(minecraft, search.getX() + 5, search.getY() + 5, InputConstants.MOUSE_BUTTON_RIGHT);
                    click(minecraft, search.getX() + 5, search.getY() + 5, InputConstants.MOUSE_BUTTON_LEFT);
                    for (int character : "acacia log".toCharArray()) {
                        minecraft.gui.screen().charTyped(new CharacterEvent(character));
                    }
                }
                case 6 -> {
                    var content = content(minecraft, MEStorageMenu.TYPE);
                    require(repo(content).size() == 1 && repo(content).getSearchString().equals("acacia log"),
                            "native search text input and filtering");
                    var slot = content.desktopSlots().stream().filter(RepoSlot.class::isInstance).findFirst()
                            .orElseThrow();
                    click(minecraft, content.getGuiLeft() + slot.x + 8, content.getGuiTop() + slot.y + 8,
                            InputConstants.MOUSE_BUTTON_LEFT);
                    var window = DesktopQaHooks.WINDOWS.values().stream()
                            .filter(w -> w.content() == content).findFirst().orElse(null);
                    if (window != null) {
                        beforeResize = window;
                        content.requestDesktopSize(window.width() + 18, window.height() + 18);
                    }
                }
                case 7 -> {
                    var content = content(minecraft, MEStorageMenu.TYPE);
                    if (beforeResize != null) {
                        var window = DesktopQaHooks.WINDOWS.values().stream()
                                .filter(w -> w.content() == content).findFirst().orElseThrow();
                        require(window.width() != beforeResize.width() || window.height() != beforeResize.height(),
                                "documented native window size update");
                        require(search(content).getValue().equals("acacia log") && content.getMenu().slots.size() == 41,
                                "resize retains search and logical slot topology");
                    }
                    require(content.getMenu().getCarried().is(Items.ACACIA_LOG)
                            && content.getMenu().getCarried().getCount() == 64,
                            "network item extraction and cursor sync");
                    var slot = content.desktopSlots().stream().filter(RepoSlot.class::isInstance).findFirst()
                            .orElseThrow();
                    click(minecraft, content.getGuiLeft() + slot.x + 8, content.getGuiTop() + slot.y + 8,
                            InputConstants.MOUSE_BUTTON_LEFT);
                    var search = search(content);
                    click(minecraft, search.getX() + 5, search.getY() + 5, InputConstants.MOUSE_BUTTON_RIGHT);
                    var button = content.children().stream().filter(SettingToggleButton.class::isInstance)
                            .map(SettingToggleButton.class::cast).filter(v -> v.getSetting() == Settings.SORT_BY)
                            .findFirst().orElseThrow();
                    sortBefore = button.getCurrentValue();
                    click(minecraft, button.getX() + 5, button.getY() + 5, InputConstants.MOUSE_BUTTON_LEFT);
                    if (content.isDesktopHosted()) {
                        beforeMove = window(content);
                        beginDrag(minecraft, beforeMove.x() + 50, beforeMove.y() + 8, 36, 18);
                    }
                }
                case 8 -> {
                    var content = content(minecraft, MEStorageMenu.TYPE);
                    if (beforeMove != null) {
                        continueDrag(minecraft);
                    }
                    require(content.getMenu().getCarried().isEmpty(), "network item insertion and cursor sync");
                    var button = content.children().stream().filter(SettingToggleButton.class::isInstance)
                            .map(SettingToggleButton.class::cast).filter(v -> v.getSetting() == Settings.SORT_BY)
                            .findFirst().orElseThrow();
                    require(button.getCurrentValue() != sortBefore, "sort button and server setting synchronization");
                    stage = beforeMove == null ? 24 : 27;
                }
                case 9 -> {
                    var content = content(minecraft, CraftingTermMenu.TYPE);
                    var slot = content.getMenu().getSlots(SlotSemantics.CRAFTING_RESULT).getFirst();
                    require(slot.getItem().is(Items.ACACIA_PLANKS), "crafting recipe and output synchronization");
                    Screenshot.grab(minecraft, false);
                    click(minecraft, content.getGuiLeft() + slot.x + 8, content.getGuiTop() + slot.y + 8,
                            InputConstants.MOUSE_BUTTON_LEFT);
                }
                case 10 -> {
                    var content = content(minecraft, CraftingTermMenu.TYPE);
                    require(content.getMenu().getCarried().is(Items.ACACIA_PLANKS)
                            && content.getMenu().getCarried().getCount() == 4,
                            "custom crafting action and cursor sync");
                    Screenshot.grab(minecraft, false);
                    var slot = content.getMenu().getSlots(SlotSemantics.CRAFTING_GRID).getFirst();
                    click(minecraft, content.getGuiLeft() + slot.x + 8, content.getGuiTop() + slot.y + 8,
                            InputConstants.MOUSE_BUTTON_LEFT);
                }
                case 11 -> {
                    var content = content(minecraft, CraftingTermMenu.TYPE);
                    require(content.getMenu().getSlots(SlotSemantics.CRAFTING_GRID).getFirst().getItem()
                            .is(Items.ACACIA_PLANKS), "custom cursor placed through a native real-slot gesture");
                    if (content.isDesktopHosted()) {
                        require(minecraft.player.inventoryMenu.getCarried().is(Items.ACACIA_LOG),
                                "native real-slot cursor reflected into the Minecraft inventory cursor");
                        MenuPacketScope.run(content.getMenu(), () -> ((MEStorageMenu) content.getMenu())
                                .handleInteraction(-1, appeng.helpers.InventoryAction.PICKUP_OR_SET_DOWN));
                        minecraft.gui.screen().charTyped(new CharacterEvent(0));
                    }
                    if (content.isDesktopHosted()) {
                        stage = 14;
                    } else {
                        finishCraftingStage(minecraft);
                    }
                }
                case 12 -> {
                    var content = content(minecraft, PatternEncodingTermMenu.TYPE);
                    if (content.isDesktopHosted()) {
                        long logs = repo(content).getAllEntries().stream()
                                .filter(e -> AEItemKey.of(Items.ACACIA_LOG).equals(e.getWhat()))
                                .mapToLong(e -> e.getStoredAmount()).sum();
                        long planks = repo(content).getAllEntries().stream()
                                .filter(e -> AEItemKey.of(Items.ACACIA_PLANKS).equals(e.getWhat()))
                                .mapToLong(e -> e.getStoredAmount()).sum();
                        require(logs == 128 && planks == 0 && minecraft.player.inventoryMenu.getCarried().isEmpty(),
                                "native-to-virtual cursor deposit conserves logs and planks");
                    }
                    Screenshot.grab(minecraft, false);
                    open(minecraft, 3, PatternAccessTermMenu.TYPE);
                }
                case 13 -> {
                    content(minecraft, PatternAccessTermMenu.TYPE);
                    Screenshot.grab(minecraft, false);
                    stage = 16;
                }
                case 14 -> {
                    var content = content(minecraft, CraftingTermMenu.TYPE);
                    require(content.getMenu().getCarried().isEmpty(), "cursor deposit completes before real-slot drag");
                    var slot = content.getMenu().getSlots(SlotSemantics.CRAFTING_GRID).getFirst();
                    click(minecraft, content.getGuiLeft() + slot.x + 8, content.getGuiTop() + slot.y + 8,
                            InputConstants.MOUSE_BUTTON_LEFT);
                }
                case 15 -> {
                    var content = content(minecraft, CraftingTermMenu.TYPE);
                    var grid = content.getMenu().getSlots(SlotSemantics.CRAFTING_GRID);
                    var first = grid.get(1);
                    var second = grid.get(2);
                    if (craftDragStep == 0) {
                        require(content.getMenu().getCarried().is(Items.ACACIA_PLANKS)
                                && content.getMenu().getCarried().getCount() == 4,
                                "real-slot pickup supplies four items for native drag");
                        beginDrag(minecraft, content.getGuiLeft() + first.x + 8,
                                content.getGuiTop() + first.y + 8, second.x - first.x, second.y - first.y);
                    } else if (craftDragStep == 1) {
                        continueDrag(minecraft);
                    } else if (craftDragStep == 2) {
                        Screenshot.grab(minecraft, false);
                        endDrag(minecraft);
                    } else {
                        require(first.getItem().is(Items.ACACIA_PLANKS) && first.getItem().getCount() == 2
                                && second.getItem().is(Items.ACACIA_PLANKS) && second.getItem().getCount() == 2
                                && grid.getFirst().getItem().isEmpty() && content.getMenu().getCarried().isEmpty(),
                                "native drag distributes four items between two real slots and clears the cursor");
                        Screenshot.grab(minecraft, false);
                        finishCraftingStage(minecraft);
                        stage = 12;
                        break;
                    }
                    craftDragStep++;
                    stage = 15;
                }
                case 24 -> {
                    if (beforeBorderResize != null) {
                        var resized = window(content(minecraft, MEStorageMenu.TYPE));
                        require(resized.width() != beforeBorderResize.width()
                                || resized.height() != beforeBorderResize.height(),
                                "native border drag resizes terminal");
                        var resizedContent = resized.content();
                        if (resizedContent.usesAeDesktopLayout()) {
                            require(resized.width() == resizedContent.getStyle().getTerminalStyle()
                                    .getScreenWidth()
                                    && (resized.height() - resizedContent.desktopPreferredSize()[1]) % 18 == 0,
                                    "resize preserves AE catalog width and adds rows on the original slot pitch");
                        } else {
                            require((resized.width() - 188) % 18 == 0,
                                    "border resize keeps catalog columns on the slot pitch");
                        }
                    }
                    var content = content(minecraft, MEStorageMenu.TYPE);
                    var settings = content.children().stream().filter(ActionButton.class::isInstance)
                            .map(ActionButton.class::cast).filter(b -> b.getMessage().getString().contains("Settings"))
                            .findFirst().orElseThrow();
                    click(minecraft, settings.getX() + 5, settings.getY() + 5, InputConstants.MOUSE_BUTTON_LEFT);
                }
                case 27 -> endDrag(minecraft);
                case 28 -> {
                    var moved = window(content(minecraft, MEStorageMenu.TYPE));
                    if (moved.x() == beforeMove.x() && moved.y() == beforeMove.y() && !moveUnlockRetried) {
                        moveUnlockRetried = true;
                        DesktopQaHooks.unlockWindow.accept(moved.content().getMenu().desktopSyncId());
                        beginDrag(minecraft, moved.x() + 50, moved.y() + 8, 36, 18);
                        stage = 8;
                        return;
                    }
                    require(moved.x() != beforeMove.x() || moved.y() != beforeMove.y(),
                            "native title-bar drag moves body and external controls");
                    beforeBorderResize = moved;
                    LOG.info("Native resize diagnostics: {}",
                            DesktopQaHooks.RESIZE_DIAGNOSTICS.get(moved.content().getMenu().desktopSyncId()));
                    beginDrag(minecraft, moved.x() + moved.width() - 2, moved.y() + moved.height() - 2, 36, 36);
                }
                case 29 -> continueDrag(minecraft);
                case 30 -> {
                    endDrag(minecraft);
                    stage = 24;
                }
                case 25 -> {
                    var content = content(minecraft, MEStorageMenu.TYPE);
                    require(content instanceof TerminalSettingsScreen<?>, "settings replace the window content");
                    if (content.isDesktopHosted() && settingsFocusProbe == null) {
                        settingsFocusProbe = content.children().stream()
                                .filter(appeng.client.gui.widgets.AECheckbox.class::isInstance)
                                .map(appeng.client.gui.widgets.AECheckbox.class::cast)
                                .filter(box -> box.visible && box.active).findFirst().orElseThrow();
                        settingsFocusOriginalValue = settingsFocusProbe.isSelected();
                        click(minecraft, settingsFocusProbe.getX() + 5, settingsFocusProbe.getY() + 5,
                                InputConstants.MOUSE_BUTTON_LEFT);
                        minecraft.gui.screen().mouseMoved(-1, -1);
                        stage = 25;
                        break;
                    }
                    if (content.isDesktopHosted()) {
                        require(settingsFocusProbe.isFocused(), "checkbox retains keyboard focus after pointer leaves");
                        click(minecraft, settingsFocusProbe.getX() + 5, settingsFocusProbe.getY() + 5,
                                InputConstants.MOUSE_BUTTON_LEFT);
                        require(settingsFocusProbe.isSelected() == settingsFocusOriginalValue,
                                "settings focus probe restores the original preference");
                        settingsFocusProbe = null;
                    }
                    Screenshot.grab(minecraft, false);
                    var back = content.children().stream().filter(TabButton.class::isInstance)
                            .map(TabButton.class::cast).findFirst().orElseThrow();
                    click(minecraft, back.getX() + 5, back.getY() + 5, InputConstants.MOUSE_BUTTON_LEFT);
                }
                case 16 -> {
                    var playerId = minecraft.player.getUUID();
                    minecraft.getSingleplayerServer().execute(() -> {
                        var player = minecraft.getSingleplayerServer().getPlayerList().getPlayer(playerId);
                        var part = (AEBasePart) PartHelper.getPart(player.level(), origin, Direction.NORTH);
                        CraftAmountMenu.open(player, MenuLocators.forPart(part), AEItemKey.of(Items.ACACIA_PLANKS), 8);
                    });
                    stage = 17;
                }
                case 26 -> {
                    require(content(minecraft,
                            MEStorageMenu.TYPE) instanceof appeng.client.gui.me.common.MEStorageScreen<?>,
                            "return from settings restores terminal controls");
                    open(minecraft, 1, CraftingTermMenu.TYPE);
                    stage = 9;
                }
                case 17 -> {
                    var content = content(minecraft, CraftAmountMenu.TYPE);
                    require(((CraftAmountMenu) content.getMenu()).getWhatToCraft().amount() == 8,
                            "captured craft-amount menu receives its target and initial quantity");
                    Screenshot.grab(minecraft, false);
                    clickButton(minecraft, content, "Next");
                }
                case 18 -> {
                    var content = content(minecraft, CraftConfirmMenu.TYPE);
                    var menu = (CraftConfirmMenu) content.getMenu();
                    require(menu.getPlan() != null && !menu.getPlan().isSimulation() && !menu.hasNoCPU(),
                            "asynchronous crafting plan and CPU synchronization");
                    Screenshot.grab(minecraft, false);
                    clickButton(minecraft, content, "Start");
                }
                case 19 -> {
                    var content = content(minecraft, MEStorageMenu.TYPE);
                    require(repo(content).getAllEntries().stream().anyMatch(e -> AEItemKey.of(Items.ACACIA_PLANKS)
                            .equals(e.getWhat()) && e.getStoredAmount() >= 8),
                            "autocrafting job returns products to storage");
                    Screenshot.grab(minecraft, false);
                    if (content.isDesktopHosted()) {
                        DesktopQaHooks.closeAll.run();
                        stage = 31;
                    } else {
                        complete();
                    }
                }
                case 31 -> {
                    open(minecraft, 0, MEStorageMenu.TYPE);
                    stage = 39;
                }
                case 32 -> {
                    var windows = DesktopQaHooks.WINDOWS.values().stream()
                            .filter(w -> w.content().getMenu().getType() == MEStorageMenu.TYPE).toList();
                    require(windows.size() == 2, "two independent same-type terminal windows");
                    secondOverlap = windows.stream().filter(w -> w.content() != firstOverlap.content()).findFirst()
                            .orElseThrow();
                    DesktopQaHooks.unlockWindow.accept(secondOverlap.content().getMenu().desktopSyncId());
                    beginDrag(minecraft, secondOverlap.x() + 50, secondOverlap.y() + 8,
                            firstOverlap.x() - secondOverlap.x(), firstOverlap.y() - secondOverlap.y());
                    afterDragStage = 33;
                    stage = 36;
                }
                case 33 -> {
                    secondOverlap = window(secondOverlap.content());
                    var field = search(secondOverlap.content());
                    click(minecraft, field.getX() + 5, field.getY() + 5, InputConstants.MOUSE_BUTTON_LEFT);
                    minecraft.gui.screen().charTyped(new CharacterEvent('z'));
                    require(search(secondOverlap.content()).getValue().contains("z")
                            && search(firstOverlap.content()).getValue().equals(firstSearch),
                            "overlapping foreground editor owns text; background editor receives none");
                    require(secondOverlap.content().getMenu().desktopSyncId().equals(DesktopQaHooks.keyboardOwner),
                            "native keyboard ownership follows foreground window");
                    beginDrag(minecraft, secondOverlap.x() + 50, secondOverlap.y() + 8, 18, 48);
                    afterDragStage = 34;
                    stage = 36;
                }
                case 34 -> {
                    secondOverlap = window(secondOverlap.content());
                    var foreground = search(secondOverlap.content());
                    var press = new MouseButtonEvent(foreground.getX() + 5, foreground.getY() + 5,
                            new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0));
                    minecraft.gui.screen().mouseClicked(press, false);
                    require(secondOverlap.content().isDragging(), "foreground editor starts pointer selection");
                    minecraft.gui.screen().mouseReleased(press);
                    var background = window(firstOverlap.content());
                    click(minecraft, background.x() + 50, background.y() + 8, InputConstants.MOUSE_BUTTON_LEFT);
                }
                case 35 -> {
                    if (ownershipStep == 0) {
                        var field = search(firstOverlap.content());
                        click(minecraft, field.getX() + 5, field.getY() + 5, InputConstants.MOUSE_BUTTON_LEFT);
                        ownershipStep = 1;
                        stage = 35;
                        return;
                    }
                    if (ownershipStep == 2) {
                        var id = firstOverlap.content().getMenu().desktopSyncId();
                        require(!DesktopQaHooks.WINDOWS.containsKey(id) && !firstOverlap.content().isDragging()
                                && DesktopQaHooks.POINTER_CANCELLATIONS.getOrDefault(id, 0) > cancellationBefore,
                                "native owner removal cancels pending pointer selection");
                        complete();
                        return;
                    }
                    require(firstOverlap.content().getMenu().desktopSyncId().equals(DesktopQaHooks.keyboardOwner)
                            && secondOverlap.content().getFocused() == null && !secondOverlap.content().isDragging(),
                            "focus transfer clears the previous window's editing and pointer state");
                    Screenshot.grab(minecraft, false);
                    var field = search(firstOverlap.content());
                    var press = new MouseButtonEvent(field.getX() + 5, field.getY() + 5,
                            new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0));
                    minecraft.gui.screen().mouseClicked(press, false);
                    var id = firstOverlap.content().getMenu().desktopSyncId();
                    cancellationBefore = DesktopQaHooks.POINTER_CANCELLATIONS.getOrDefault(id, 0);
                    DesktopQaHooks.closeWindow.accept(id);
                    ownershipStep = 2;
                    stage = 35;
                }
                case 36 -> continueDrag(minecraft);
                case 37 -> endDrag(minecraft);
                case 38 -> stage = afterDragStage;
                case 39 -> {
                    if (!Boolean.getBoolean("ae2.desktop.qa.frameProbe")) {
                        firstOverlap = window(content(minecraft, MEStorageMenu.TYPE));
                        firstSearch = search(firstOverlap.content()).getValue();
                        open(minecraft, 1, MEStorageMenu.TYPE);
                        stage = 32;
                        return;
                    }
                    LOG.info("FRAME PROBE {} active={} mouse={},{} frame={},{} {}x{}", frameProbe,
                            minecraft.isWindowActive(), minecraft.mouseHandler.getScaledXPos(minecraft.getWindow()),
                            minecraft.mouseHandler.getScaledYPos(minecraft.getWindow()), beforeMove.x(), beforeMove.y(),
                            beforeMove.width(), beforeMove.height());
                    Screenshot.grab(minecraft, false);
                    if (++frameProbe < 5) {
                        hoverFrameControl(minecraft);
                        stage = 39;
                    } else {
                        LOG.info("FRAME PROBE COMPLETE");
                        stage = 40;
                    }
                }
                default -> throw new IllegalStateException("Unexpected stage " + stage);
            }
        } catch (Exception failure) {
            LOG.error("Terminal QA FAILED at stage {}", stage - 1, failure);
            Screenshot.grab(minecraft, false);
            stage = 40;
            if (Boolean.getBoolean("ae2.desktop.qa.quit"))
                minecraft.stop();
        }
    }

    private static void open(Minecraft minecraft, int offset, MenuType<?> type) {
        var playerId = minecraft.player.getUUID();
        minecraft.getSingleplayerServer().execute(() -> {
            var player = minecraft.getSingleplayerServer().getPlayerList().getPlayer(playerId);
            var part = (AEBasePart) PartHelper.getPart(player.level(), origin.offset(offset, 0, 0), Direction.NORTH);
            require(MenuOpener.open(type, player, MenuLocators.forPart(part)), "opened " + type);
        });
    }

    private static int craftDragStep;

    private static void finishCraftingStage(Minecraft minecraft) {
        var playerId = minecraft.player.getUUID();
        minecraft.getSingleplayerServer().execute(() -> {
            var player = minecraft.getSingleplayerServer().getPlayerList().getPlayer(playerId);
            var part = (AEBasePart) PartHelper.getPart(player.level(), origin, Direction.NORTH);
            part.getGridNode().getGrid().getStorageService().getInventory().insert(AEItemKey.of(Items.ACACIA_LOG),
                    64, Actionable.MODULATE, new BaseActionSource());
        });
        open(minecraft, 2, PatternEncodingTermMenu.TYPE);
    }

    private static AEBaseScreen<?> content(Minecraft minecraft, MenuType<?> type) {
        var content = DesktopQaHooks.WINDOWS.values().stream().map(DesktopQaHooks.Window::content)
                .filter(c -> c.getMenu().getType() == type).findFirst()
                .orElseGet(() -> minecraft.gui.screen() instanceof AEBaseScreen<?> ae ? ae : null);
        require(content != null && content.getMenu().getType() == type, "native content available for " + type);
        return content;
    }

    private static Repo repo(AEBaseScreen<?> content) {
        return (Repo) ((MEStorageMenu) content.getMenu()).getClientRepo();
    }

    private static EditBox search(AEBaseScreen<?> content) {
        return content.children().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast)
                .filter(EditBox::isVisible).findFirst().orElseThrow();
    }

    private static void click(Minecraft minecraft, double x, double y, int button) {
        var event = new MouseButtonEvent(x, y, new MouseButtonInfo(button, 0));
        minecraft.gui.screen().mouseClicked(event, false);
        minecraft.gui.screen().mouseReleased(event);
    }

    private static DesktopQaHooks.Window window(AEBaseScreen<?> content) {
        return DesktopQaHooks.WINDOWS.values().stream().filter(w -> w.content() == content).findFirst().orElseThrow();
    }

    private static void beginDrag(Minecraft minecraft, double x, double y, double dx, double dy) {
        var info = new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0);
        var window = minecraft.getWindow();
        double sx = (double) window.getScreenWidth() / window.getGuiScaledWidth();
        double sy = (double) window.getScreenHeight() / window.getGuiScaledHeight();
        dragMotion = new double[] { (x + dx) * sx, (y + dy) * sy, dx * sx, dy * sy };
        dragFocus = window.isFocused();
        qaFocus(minecraft, true);
        minecraft.mouseHandler.onMove(window.handle(), x * sx, y * sy, 0, 0);
        minecraft.mouseHandler.handleAccumulatedMovement();
        minecraft.mouseHandler.onButton(window.handle(), info, InputConstants.PRESS);
    }

    private static void continueDrag(Minecraft minecraft) {
        minecraft.mouseHandler.onMove(minecraft.getWindow().handle(), dragMotion[0], dragMotion[1], dragMotion[2],
                dragMotion[3]);
        minecraft.mouseHandler.handleAccumulatedMovement();
    }

    private static void endDrag(Minecraft minecraft) {
        minecraft.mouseHandler.onButton(minecraft.getWindow().handle(),
                new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0), InputConstants.RELEASE);
        qaFocus(minecraft, dragFocus);
    }

    private static void qaFocus(Minecraft minecraft, boolean focused) {
        try {
            // Simulate Minecraft's own focus callback for this background QA client.
            // No Salts implementation or user desktop pointer is accessed.
            var window = minecraft.getWindow();
            var focus = window.getClass().getDeclaredMethod("onFocus", boolean.class);
            focus.setAccessible(true);
            focus.invoke(window, focused);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Cannot dispatch Minecraft QA focus/mouse events", error);
        }
    }

    private static void hoverFrameControl(Minecraft minecraft) {
        var window = minecraft.getWindow();
        double x = beforeMove.x() + beforeMove.width() - 65 + frameProbe * 14;
        double y = beforeMove.y() + 8;
        minecraft.mouseHandler.onMove(window.handle(), x * window.getScreenWidth() / window.getGuiScaledWidth(),
                y * window.getScreenHeight() / window.getGuiScaledHeight(), 0, 0);
        minecraft.gui.screen().mouseMoved(x, y);
    }

    private static void nativeControlClick(Minecraft minecraft, double x, double y) {
        boolean focused = minecraft.getWindow().isFocused();
        qaFocus(minecraft, true);
        var window = minecraft.getWindow();
        minecraft.mouseHandler.onMove(window.handle(), x * window.getScreenWidth() / window.getGuiScaledWidth(),
                y * window.getScreenHeight() / window.getGuiScaledHeight(), 0, 0);
        var info = new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0);
        minecraft.mouseHandler.onButton(window.handle(), info, InputConstants.PRESS);
        minecraft.mouseHandler.onButton(window.handle(), info, InputConstants.RELEASE);
        qaFocus(minecraft, focused);
    }

    private static void clickButton(Minecraft minecraft, AEBaseScreen<?> content, String label) {
        var button = content.children().stream().filter(Button.class::isInstance).map(Button.class::cast)
                .filter(b -> b.active && b.getMessage().getString().equals(label)).findFirst().orElseThrow();
        click(minecraft, button.getX() + 5, button.getY() + 5, InputConstants.MOUSE_BUTTON_LEFT);
    }

    private static void require(boolean condition, String description) {
        if (!condition) {
            throw new IllegalStateException(description);
        }
        LOG.info("Terminal QA PASS: {}", description);
    }

    private static void complete() {
        LOG.info("Terminal QA COMPLETE: search, scroll, sort, cursor transfers, crafting, settings and autocrafting");
        stage = 40;
        if (Boolean.getBoolean("ae2.desktop.qa.lifecycle")) {
            DesktopLifecycleQa.begin(Minecraft.getInstance());
            return;
        }
        if (Boolean.getBoolean("ae2.desktop.qa.companion")) {
            DesktopCompanionQa.begin(Minecraft.getInstance());
            return;
        }
        if (Boolean.getBoolean("ae2.desktop.qa.quit"))
            Minecraft.getInstance().stop();
    }
}
