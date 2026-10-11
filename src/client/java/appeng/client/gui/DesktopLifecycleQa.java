package appeng.client.gui;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.InputConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import appeng.client.gui.style.PaletteColor;

/** Opt-in native resource/viewport probe. No Salt linkage outside the optional provider. */
final class DesktopLifecycleQa {
    private static final Logger LOG = LoggerFactory.getLogger(DesktopLifecycleQa.class);
    private static final Path OUTPUT = Path.of("../build/surface-lifecycle-native");
    private static final String PACK = "file/api4-native-reload-probe";
    private static final Identifier STYLE = Identifier.parse("ae2:screens/desktop/terminals/terminal.json");
    private static AEBaseScreen<?> content;
    private static UUID id;
    private static List<Slot> slots;
    private static List<ItemStack> items;
    private static String search;
    private static int textColor;
    private static int oldWidth;
    private static int oldHeight;
    private static int oldSurfaceWidth;
    private static int oldViewportWidth;
    private static long resourceRevision;
    private static List<String> selectedPacks;
    private static CompletableFuture<Void> reload;
    private static int stage;
    private static int wait;
    private static long captureStarted;

    static void begin(Minecraft client) {
        try {
            Files.createDirectories(OUTPUT);
            Files.deleteIfExists(OUTPUT.resolve("failed.txt"));
            Files.deleteIfExists(OUTPUT.resolve("completed.txt"));
            var owner = DesktopQaHooks.WINDOWS.values().stream().findFirst().orElseThrow();
            content = owner.content();
            id = content.getMenu().desktopSyncId();
            slots = List.copyOf(content.getMenu().slots);
            items = slots.stream().map(slot -> slot.getItem().copy()).toList();
            search = editor().getValue();
            textColor = content.getStyle().getColor(PaletteColor.DEFAULT_TEXT_COLOR).toARGB();
            oldWidth = client.getWindow().getScreenWidth();
            oldHeight = client.getWindow().getScreenHeight();
            oldSurfaceWidth = owner.width();
            oldViewportWidth = client.getWindow().getGuiScaledWidth();
            resourceRevision = environment().revision();
            selectedPacks = new ArrayList<>(client.getResourcePackRepository().getSelectedIds());

            var root = Path.of("resourcepacks/api4-native-reload-probe");
            var destination = root.resolve("assets/ae2/screens/desktop/terminals/terminal.json");
            Files.createDirectories(destination.getParent());
            JsonObject document;
            try (var reader = client.getResourceManager().openAsReader(STYLE)) {
                document = JsonParser.parseReader(reader).getAsJsonObject();
            }
            JsonObject palette = document.has("palette") ? document.getAsJsonObject("palette") : new JsonObject();
            palette.addProperty("DEFAULT_TEXT_COLOR", "#06f0bb");
            document.add("palette", palette);
            Files.writeString(destination, document.toString());
            Files.writeString(root.resolve("pack.mcmeta"),
                    "{\"pack\":{\"description\":\"Disposable API 4 native reload probe\",\"min_format\":97,\"max_format\":97}}");
            client.getResourcePackRepository().reload();
            require(client.getResourcePackRepository().getAvailableIds().contains(PACK),
                    "probe resource pack discovered");
            var selected = new ArrayList<>(selectedPacks);
            selected.add(PACK);
            client.getResourcePackRepository().setSelected(selected);
            reload = client.reloadResourcePacks();
            stage = 1;
        } catch (Exception failure) {
            fail(client, failure);
        }
    }

    static void tick(Minecraft client) {
        if (stage == 0 || stage >= 100 || ++wait < 30)
            return;
        wait = 0;
        try {
            if ((stage == 1 || stage == 3) && !reload.isDone())
                return;
            // The future can finish while the native reload splash is still fading.
            // Await an unobscured native frame before testing/capturing the retained UI.
            if (client.gui.overlay() != null)
                return;
            switch (stage) {
                case 1 -> {
                    reload.join();
                    require(environment().revision() > resourceRevision,
                            "retained window received actual resource reload");
                    require(content.getStyle().getColor(PaletteColor.DEFAULT_TEXT_COLOR).toARGB() == 0xFF06F0BB,
                            "retained independent style uses the reloaded palette");
                    assertRetained(client);
                    captureStarted = System.currentTimeMillis();
                    Screenshot.grab(client, false);
                    stage = 2;
                }
                case 2 -> {
                    saveCapture("reloaded-palette.png");
                    client.getResourcePackRepository().setSelected(selectedPacks);
                    reload = client.reloadResourcePacks();
                    stage = 3;
                }
                case 3 -> {
                    reload.join();
                    require(environment().revision() > resourceRevision + 1,
                            "restoring packs sends a second reload notification");
                    require(content.getStyle().getColor(PaletteColor.DEFAULT_TEXT_COLOR).toARGB() == textColor,
                            "original resource palette restored");
                    assertRetained(client);
                    client.getWindow().setWindowed(640, 480);
                    client.resizeGui();
                    stage = 4;
                }
                case 4 -> {
                    var owner = owner();
                    require(client.getWindow().getGuiScaledWidth() <= 320
                            && client.getWindow().getGuiScaledWidth() < oldViewportWidth,
                            "native minimum viewport exercises the author-selected compact row layout");
                    LOG.info("Lifecycle QA small viewport: gui={}x{} surface={}x{} environment={}",
                            client.getWindow().getGuiScaledWidth(), client.getWindow().getGuiScaledHeight(),
                            owner.width(), owner.height(), environment());
                    require(environment().width() == client.getWindow().getGuiScaledWidth()
                            && environment().height() == client.getWindow().getGuiScaledHeight(),
                            "viewport notification matches the actual native window");
                    require(owner.width() == oldSurfaceWidth,
                            "original artwork width remains unscaled in the small viewport");
                    require(owner.x() + 50 >= 0 && owner.x() + 50 < client.getWindow().getGuiScaledWidth()
                            && owner.y() + 8 >= 0 && owner.y() + 8 < client.getWindow().getGuiScaledHeight(),
                            "original title management region remains reachable");
                    assertRetained(client);
                    var field = editor();
                    click(client, field.getX() + 5, field.getY() + 5, InputConstants.MOUSE_BUTTON_LEFT);
                    field.setCursorPosition(search.length());
                    field.setHighlightPos(search.length());
                    client.gui.screen().charTyped(new CharacterEvent('q'));
                    LOG.info("Lifecycle QA typing: before={} after={} focused={} keyboardOwner={} expectedOwner={}",
                            search, editor().getValue(), field.isFocused(), DesktopQaHooks.keyboardOwner, id);
                    require(editor().getValue().equals(search + "q"), "typing uses re-prepared native widget hitboxes");
                    editor().setValue(search);
                    click(client, owner.x() + 50, owner.y() + 8, InputConstants.MOUSE_BUTTON_RIGHT);
                    captureStarted = System.currentTimeMillis();
                    Screenshot.grab(client, false);
                    stage = 5;
                }
                case 5 -> {
                    saveCapture("small-viewport-management.png");
                    client.gui.screen().keyPressed(new KeyEvent(InputConstants.KEY_ESCAPE, 0, 0));
                    client.getWindow().setWindowed(oldWidth, oldHeight);
                    client.resizeGui();
                    stage = 6;
                }
                case 6 -> {
                    assertRetained(client);
                    require(environment().width() == client.getWindow().getGuiScaledWidth(),
                            "restored viewport notification");
                    DesktopQaHooks.openModalProbe.run();
                    stage = 7;
                }
                case 7 -> {
                    require(DesktopQaHooks.popupState != null && DesktopQaHooks.popupState.open,
                            "declared modal popup is open");
                    var field = editor();
                    click(client, field.getX() + 5, field.getY() + 5, InputConstants.MOUSE_BUTTON_LEFT);
                    client.gui.screen().charTyped(new CharacterEvent('v'));
                    require(editor().getValue().equals(search), "modal popup blocks background pointer and text");
                    require(DesktopQaHooks.popupState.characters == 1, "modal owner receives character input");
                    captureStarted = System.currentTimeMillis();
                    Screenshot.grab(client, false);
                    stage = 8;
                }
                case 8 -> {
                    saveCapture("modal-popup.png");
                    client.gui.screen().keyPressed(new KeyEvent(InputConstants.KEY_ESCAPE, 0, 0));
                    require(!DesktopQaHooks.popupState.open && DesktopQaHooks.popupState.dismissals == 1
                            && "ESCAPE".equals(DesktopQaHooks.popupState.dismissal),
                            "Escape dismisses the declared popup exactly once");
                    require(!DesktopQaHooks.popupState.closed, "popup dismissal retains the parent view");
                    DesktopQaHooks.closeModalProbe.run();
                    stage = 9;
                }
                case 9 -> {
                    require(DesktopQaHooks.popupState.closed, "owner disposal closes the probe lifetime");
                    assertRetained(client);
                    Files.writeString(OUTPUT.resolve("completed.txt"),
                            "Native reload, small viewport and modal popup checks passed.\n");
                    LOG.info(
                            "Lifecycle QA COMPLETE: resource reload, retained state, small viewport, management and modal popup dismissal");
                    stage = 100;
                    if (Boolean.getBoolean("ae2.desktop.qa.companion")) {
                        DesktopCompanionQa.begin(client);
                    } else if (Boolean.getBoolean("ae2.desktop.qa.quit"))
                        client.stop();
                }
                default -> throw new IllegalStateException("Unknown lifecycle stage " + stage);
            }
        } catch (Exception failure) {
            fail(client, failure);
        }
    }

    private static DesktopQaHooks.Window owner() {
        var owner = DesktopQaHooks.WINDOWS.get(id);
        require(owner != null && owner.content() == content, "same window/content lifetime retained");
        return owner;
    }

    private static DesktopQaHooks.Environment environment() {
        var environment = DesktopQaHooks.ENVIRONMENTS.get(id);
        require(environment != null, "public environment callback was delivered");
        return environment;
    }

    private static EditBox editor() {
        return content.children().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast)
                .filter(EditBox::isVisible).findFirst().orElseThrow();
    }

    private static void assertRetained(Minecraft client) {
        owner();
        require(content.getMenu().slots.equals(slots), "logical menu slot topology and identities retained");
        for (int index = 0; index < slots.size(); index++)
            require(ItemStack.matches(items.get(index), slots.get(index).getItem()),
                    "slot contents conserved " + index);
        require(editor().getValue().equals(search), "search state retained across native relayout/reload");
        require(client.player.inventoryMenu.getCarried().isEmpty(), "shared cursor remains empty");
    }

    private static void click(Minecraft client, double x, double y, int button) {
        var event = new MouseButtonEvent(x, y, new MouseButtonInfo(button, 0));
        client.gui.screen().mouseClicked(event, false);
        client.gui.screen().mouseReleased(event);
    }

    private static void saveCapture(String name) throws Exception {
        Path latest;
        try (var files = Files.list(Path.of("screenshots"))) {
            latest = files.filter(path -> path.toString().endsWith(".png")).filter(path -> {
                try {
                    return Files.getLastModifiedTime(path).toMillis() >= captureStarted;
                } catch (Exception ignored) {
                    return false;
                }
            }).max(java.util.Comparator.comparingLong(path -> {
                try {
                    return Files.getLastModifiedTime(path).toMillis();
                } catch (Exception ignored) {
                    return 0;
                }
            })).orElseThrow();
        }
        Files.copy(latest, OUTPUT.resolve(name), StandardCopyOption.REPLACE_EXISTING);
    }

    private static void require(boolean valid, String description) {
        if (!valid)
            throw new IllegalStateException(description);
        LOG.info("Lifecycle QA PASS: {}", description);
    }

    private static void fail(Minecraft client, Exception failure) {
        LOG.error("Lifecycle QA FAILED at stage {}", stage, failure);
        stage = 100;
        try {
            Files.createDirectories(OUTPUT);
            Files.writeString(OUTPUT.resolve("failed.txt"), failure.toString());
        } catch (Exception writeError) {
            failure.addSuppressed(writeError);
        }
        if (selectedPacks != null)
            client.getResourcePackRepository().setSelected(selectedPacks);
        if (Boolean.getBoolean("ae2.desktop.qa.quit"))
            client.stop();
    }
}
